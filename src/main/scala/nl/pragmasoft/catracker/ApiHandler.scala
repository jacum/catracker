package nl.pragmasoft.catracker

import cats.Applicative
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import nl.pragmasoft.catracker.Model.{PositionRepository, StoredPosition}
import nl.pragmasoft.catracker.http.{Handler, IncomingEventResponse, PathForDeviceResponse}
import nl.pragmasoft.catracker.http.definitions.{DevicePath, TtnEvent}

import java.time.{Instant, ZoneOffset}

class ApiHandler[F[_]: Applicative](positions: PositionRepository[F]) extends Handler[F] with LazyLogging {
  def pathForDevice(respond: PathForDeviceResponse.type)(device: String): F[PathForDeviceResponse] = for {
    pathPositions <- positions.findForDevice(device)
    _ = logger.info(s"Fetching path for $device")
  } yield {
    PathForDeviceResponse.Ok(
      DevicePath(
        description = pathPositions.headOption.map( p => s"${p.app} ${p.deviceType} ${p.deviceSerial}").getOrElse("?"),
        positions = pathPositions
          .distinctBy(_.recorded)
          .map( p => DevicePath.Positions(p.latitude.doubleValue, p.longitude.doubleValue,
            Instant.ofEpochMilli(p.recorded).atOffset(ZoneOffset.UTC))
        ).toVector
      )
    )
  }

  def incomingEvent(respond: IncomingEventResponse.type)(e: TtnEvent): F[IncomingEventResponse] = {
    val gw = e.metadata.gateways.maxBy(_.snr)
    val p = e.payloadFields
    for {
      _ <- positions.add(
        StoredPosition(
          recorded = gw.time.toInstant.toEpochMilli,
          app = e.appId,
          deviceType = e.devId,
          deviceSerial = e.hardwareSerial,
          latitude = p.latitude,
          longitude = p.longitude,
          positionFix = p.gnssFix,
          bestGateway = gw.gtwId,
          bestSNR = gw.snr,
          battery = p.capacity.toInt,
          temperature = p.temperature,
          counter = e.counter
        )
      )
    } yield IncomingEventResponse.Created
  }
}