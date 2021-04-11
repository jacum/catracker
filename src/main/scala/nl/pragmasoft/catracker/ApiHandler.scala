package nl.pragmasoft.catracker

import akka.actor.typed.ActorSystem
import cats.Applicative
import cats.effect.Async
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import nl.pragmasoft.catracker.Model.{PositionRepository, StoredPosition}
import nl.pragmasoft.catracker.TrackerProtocol.UpdatePosition
import nl.pragmasoft.catracker.http.definitions.{DevicePath, KpnEventRecord, TtnEvent}
import nl.pragmasoft.catracker.http.{Handler, IncomingEventKpnResponse, IncomingEventTtnResponse, PathForDeviceResponse}

import java.time.{LocalDateTime, ZoneOffset}

class ApiHandler[F[_]: Applicative: Async](positions: PositionRepository[F], system: ActorSystem[TrackerProtocol.Command]) extends Handler[F] with LazyLogging {
  def pathForDevice(respond: PathForDeviceResponse.type)(device: String): F[PathForDeviceResponse] =
    for {
      lastPositions <- positions.findForDevice(device)
      allPathPositions = lastPositions.distinctBy(_.recorded).filter(p => p.accuracy <= 16 && p.positionFix && p.longitude != 0 && p.latitude != 0)
      pathPositions = allPathPositions.
        headOption map { head =>
        List(head) ++
          allPathPositions.tail.filter(head.recorded - _.recorded < Tracker.TrackingTailLength)
      } getOrElse List.empty
      _ = logger.info(s"Fetching path for $device")
    } yield {
      val lastSeen = pathPositions.headOption.map(p => LocalDateTime.now().atOffset(ZoneOffset.UTC).toInstant.toEpochMilli - p.recorded).getOrElse(0L)
      PathForDeviceResponse.Ok(
        DevicePath(
          description = pathPositions.headOption.map(p => s"${p.app} ${p.deviceType} ${p.deviceSerial}").getOrElse("?"),
          lastSeen = BigDecimal(lastSeen),
          positions = pathPositions
            .map(p => DevicePath.Positions(p.latitude.doubleValue, p.longitude.doubleValue, p.battery))
            .toVector
        )
      )
    }

  def incomingEventTtn(respond: IncomingEventTtnResponse.type)(e: TtnEvent): F[IncomingEventTtnResponse] = {
    val gw = e.metadata.gateways.maxBy(_.snr)
    val p  = e.payloadFields
    val position = StoredPosition(
      recorded = gw.time.toInstant.toEpochMilli,
      app = e.appId,
      deviceType = e.devId,
      deviceSerial = e.hardwareSerial,
      latitude = p.latitude,
      longitude = p.longitude,
      positionFix = p.gnssFix,
      bestGateway = gw.gtwId,
      bestSNR = gw.snr,
      accuracy = p.accuracy,
      battery = p.capacity.toInt,
      temperature = p.temperature,
      counter = e.counter
    )
    system ! UpdatePosition(position)

    for {
      _ <- positions.add(position)
    } yield IncomingEventTtnResponse.Created
  }


  def incomingEventKpn(respond: IncomingEventKpnResponse.type)(body: Vector[KpnEventRecord]): F[IncomingEventKpnResponse] = {

      val position = KpnEvent.decode(body).get
      system ! UpdatePosition(position)
      for {
        _ <- positions.add(position)
      } yield IncomingEventKpnResponse.Created

  }

}
