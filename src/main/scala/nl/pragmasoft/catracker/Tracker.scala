package nl.pragmasoft.catracker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import cats.effect.{Concurrent, IO, Timer}
import cats.implicits._
import com.mysql.cj.x.protobuf.Mysqlx.ServerMessages
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import fs2._
import fs2.concurrent.Topic
import nl.pragmasoft.catracker.Model.StoredPosition
import nl.pragmasoft.catracker.http.definitions.DevicePath.Positions
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Ping, Text}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt

object TrackerProtocol {
  // (external) API commands
  sealed trait Command
  final case class UpdatePosition(storedPosition: StoredPosition) extends Command

  final case class ServerMessage(deviceId: DeviceId, payload: ServerMessagePayload) extends Command

  sealed trait ServerMessagePayload
  final case class Path(positions: List[StoredPosition]) extends ServerMessagePayload
  final case object FixLost                              extends ServerMessagePayload
  final case object LiveTrackingStarted                  extends ServerMessagePayload
  final case class BatteryWarning(left: Int)             extends ServerMessagePayload

  private val typeFieldName: String = "t"
  sealed trait ClientMessage
  case class AdjustFrequency(updatePerSeconds: Int) extends ClientMessage

  // (internal) commands changing state
  sealed trait StateCommand
  final case class Update(position: StoredPosition)                                                extends StateCommand
  final case class IncomingClientMessage(connectionId: ConnectionId, clientMessage: ClientMessage) extends StateCommand

  implicit val positionEncoder: Encoder[StoredPosition]                      = deriveEncoder[StoredPosition]
  implicit val pathEncoder: Encoder[Path]                                    = deriveEncoder[Path]
  implicit val fixLostEncoder: Encoder[FixLost.type]                         = deriveEncoder[FixLost.type]
  implicit val liveTrackingStartedEncoder: Encoder[LiveTrackingStarted.type] = deriveEncoder[LiveTrackingStarted.type]
  implicit val batteryWarningEncoder: Encoder[BatteryWarning]                = deriveEncoder[BatteryWarning]
  implicit val serverMessagePayloadDecoder: Encoder[ServerMessagePayload]    = deriveEncoder[ServerMessagePayload]

  implicit val adjustFrequencyDecoder: Decoder[AdjustFrequency] = deriveDecoder[AdjustFrequency]
  implicit val clientMessageDecoder: Decoder[ClientMessage]     = deriveDecoder[ClientMessage]

}

object Trackers extends LazyLogging {

  import TrackerProtocol._

  def toClient(deviceId: DeviceId, connectionId: ConnectionId)(implicit concurrent: Concurrent[IO]): Stream[IO, WebSocketFrame] = {
    logger.info(s"$deviceId $connectionId: connected")
    activeConnections
      .getOrElseUpdate(deviceId, TrieMap.empty)
      .getOrElseUpdate(connectionId, Topic[IO, WebSocketFrame](Text("")).unsafeRunSync())
      .subscribe(10)
  }

  def fromClient(deviceId: DeviceId, connectionId: ConnectionId)(wsfStream: Stream[IO, WebSocketFrame]): Stream[IO, Unit] =
    wsfStream.collect {
      case Text(text, _) =>
        (for {
          json    <- parse(text)
          message <- clientMessageDecoder.decodeJson(json)
        } yield message)
          .foreach(m => activeDevices.get(deviceId).map(_ ! IncomingClientMessage(connectionId, m)))

      case Close(_) => close(deviceId, connectionId)
    }

  def close(deviceId: DeviceId, connectionId: ConnectionId): IO[Unit] =
    IO {
      activeConnections.get(deviceId).foreach(_.remove(connectionId))
      logger.info(s"$deviceId $connectionId: disconnected")
    }

  private val activeConnections: TrieMap[DeviceId, TrieMap[ConnectionId, Topic[IO, WebSocketFrame]]] = TrieMap.empty
  private val activeDevices: TrieMap[DeviceId, ActorRef[StateCommand]]                               = TrieMap.empty

  def apply(timer: Timer[IO]): Behavior[Command] =
    Behaviors.setup { context =>
      def ping: IO[Unit]   = IO(activeConnections.foreach(_._2.foreach(_._2.publish1(Ping()))))
      def repeat: IO[Unit] = ping >> timer.sleep(duration = 10 seconds) >> repeat
      repeat.unsafeRunAsyncAndForget()

      def tracker(device: DeviceId): ActorRef[StateCommand] =
        activeDevices.getOrElseUpdate(device, context.spawn(Tracker(device, context.self), device.value))

      Behaviors.receiveMessage {
        case UpdatePosition(storedPosition) =>
          tracker(DeviceId(storedPosition.deviceSerial)) ! Update(storedPosition)
          Behaviors.same
        case ServerMessage(deviceId, payload) =>
          activeConnections
            .get(deviceId)
            .foreach(d =>
              d.foreach {
                case (_, topic) =>
                  topic.publish1(Text(payload.asJson.toString)).unsafeRunAsyncAndForget()
              }
            )
          Behaviors.same
      }
    }
}

object Tracker {

  import TrackerProtocol._
  val TrackingTailLength: Long = (60 minutes).toMillis

  sealed trait Event
  final case class PositionUpdated(position: StoredPosition) extends Event

  case class State(deviceId: DeviceId, positions: List[StoredPosition]) {
    def messagesOnIncomingPosition(incoming: StoredPosition): List[ServerMessage] =
      positions match {
        case Nil => List(ServerMessage(deviceId, LiveTrackingStarted))
        case last :: _
            if incoming.positionFix &&
              (!last.positionFix || (incoming.recorded - last.recorded) > TrackingTailLength) =>
          List(ServerMessage(deviceId, LiveTrackingStarted))
        case p :: _ if p.positionFix && !incoming.positionFix =>
          List(ServerMessage(deviceId, FixLost))

        case _ => List.empty
      }
  }
  private def appendPosition(positions: List[StoredPosition], newPosition: StoredPosition): List[StoredPosition] =
    positions.headOption
      .map(h =>
        if (newPosition.recorded > h.recorded + TrackingTailLength) List(newPosition)
        else newPosition :: positions
      )
      .getOrElse(List(newPosition))

  def apply(device: DeviceId, parent: ActorRef[Command]): Behavior[StateCommand] =
    EventSourcedBehavior[StateCommand, Event, State](
      persistenceId = PersistenceId.ofUniqueId(device.value),
      emptyState = State(device, List.empty),
      commandHandler = { (state, command) =>
        command match {
          case Update(position) =>
            state.messagesOnIncomingPosition(position).foreach(parent ! _)
            Effect.persist(PositionUpdated(position))
          case IncomingClientMessage(connectionId, clientMessage) =>
            // todo process message
            Effect.none
        }
      },
      eventHandler = {
        case (state, event: PositionUpdated) =>
          val newState = state.copy(positions = appendPosition(state.positions, event.position))
          parent ! ServerMessage(device, Path(newState.positions))
          newState
      }
    )
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2).withDeleteEventsOnSnapshot)
      .receiveSignal {
        case (state, RecoveryCompleted) if state.positions.nonEmpty => parent ! ServerMessage(device, Path(state.positions))
      }

}
