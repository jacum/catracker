package nl.pragmasoft.catracker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import cats.effect.{Concurrent, IO, Timer}
import com.typesafe.scalalogging.LazyLogging
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import fs2._
import fs2.concurrent.Topic
import nl.pragmasoft.catracker.Model.StoredPosition
import nl.pragmasoft.catracker.Trackers.StateUpdated
import nl.pragmasoft.catracker.http.definitions.DevicePath.Positions
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Ping, Text}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt

object Trackers extends LazyLogging {
  implicit val positionEncoder: Encoder[Positions] = deriveEncoder[Positions]

  def connect(deviceId: DeviceId, connectionId: ConnectionId)(implicit concurrent: Concurrent[IO]): Stream[IO, WebSocketFrame] = {
    logger.info(s"$deviceId $connectionId: connected")
    activeConnections
      .getOrElseUpdate(deviceId, TrieMap.empty)
      .getOrElseUpdate(connectionId, Topic[IO, WebSocketFrame](Text("")).unsafeRunSync())
      .subscribe(10)
  }

  def disconnect(deviceId: DeviceId, connectionId: ConnectionId): IO[Unit] =
    IO {
      activeConnections.get(deviceId).foreach(_.remove(connectionId))
      logger.info(s"$deviceId $connectionId: disconnected")
    }

  sealed trait Command
  final case class UpdatePosition(storedPosition: StoredPosition) extends Command
  final case class StateUpdated(state: Tracker.State) extends Command

  private val activeConnections: TrieMap[DeviceId, TrieMap[ConnectionId, Topic[IO, WebSocketFrame]]] = TrieMap.empty
  private val activeDevices: TrieMap[DeviceId, ActorRef[Tracker.Command]] = TrieMap.empty

  def apply(timer: Timer[IO]): Behavior[Command] =
    Behaviors.setup { context =>

      def ping: IO[Unit] = IO(activeConnections.foreach(_._2.foreach(_._2.publish1(Ping()))))
      def repeat: IO[Unit] = ping >> timer.sleep(10 seconds) >> repeat
      repeat.unsafeRunAsyncAndForget()

      def tracker(device: DeviceId): ActorRef[Tracker.Command] =
        activeDevices.getOrElseUpdate(device,
          context.spawn(Tracker(device, context.self), device.value))

      Behaviors.receiveMessage {
        case UpdatePosition(storedPosition) =>
          tracker(DeviceId(storedPosition.deviceSerial)) ! Tracker.Update(storedPosition)
          Behaviors.same
        case StateUpdated(state) =>
          state.positions.headOption.foreach( p =>
            activeConnections.get(DeviceId(p.deviceSerial)).foreach( d =>
              d.foreach {
                case (_, topic) =>
                  topic.publish1(Text(Positions(p.latitude.toDouble, p.longitude.toDouble).asJson.toString)).unsafeRunAsyncAndForget()
              }
            )
          )
          Behaviors.same
      }
    }
}

object Tracker {
  val RestartTrackingAfterMillis: Long = (15 minutes).toMillis
  sealed trait Command
  final case class Update(position: StoredPosition) extends Command

  sealed trait Event
  final case class PositionUpdated(position: StoredPosition) extends Event

  sealed trait Message
  final case object MovementStarted
  final case object MovementStopped

  case class State(message: Option[Message], positions: List[StoredPosition])
  val EmptyState: State = State(None, List.empty)

  private def appendPosition(positions: List[StoredPosition], newPosition: StoredPosition): List[StoredPosition] = {
    positions.headOption.map( h =>
    if (newPosition.recorded > h.recorded + RestartTrackingAfterMillis) List(newPosition)
    else newPosition :: positions
    ).getOrElse(List(newPosition))
  }

  def apply(device: DeviceId, parent: ActorRef[Trackers.Command]): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(device.value),
      emptyState = State(None, List.empty),
      commandHandler = { (_, command) =>
        command match {
          case Update(position) =>
            Effect.persist(PositionUpdated(position))
        }
      },
      eventHandler = {
        case (state, event: PositionUpdated) =>
          val newState = state.copy(positions = appendPosition(state.positions, event.position))
          parent ! StateUpdated(newState)
          newState
      })
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2).withDeleteEventsOnSnapshot)
      .receiveSignal {
         case (state, RecoveryCompleted) => parent ! StateUpdated(state)
      }

}
