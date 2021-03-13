package nl.pragmasoft.catracker

import akka.actor.typed.ActorSystem
import cats.Applicative
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import nl.pragmasoft.catracker.Model.{PositionRepository, StoredPosition}
import nl.pragmasoft.catracker.http.definitions.{DevicePath, TtnEvent}
import nl.pragmasoft.catracker.http.{Handler, IncomingEventResponse, PathForDeviceResponse}

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class ApiHandler[F[_]: Applicative](positions: PositionRepository[F], system: ActorSystem[Trackers.Command]) extends Handler[F] with LazyLogging {
  def pathForDevice(respond: PathForDeviceResponse.type)(device: String): F[PathForDeviceResponse] =
    for {
      pathPositions <- positions.findForDevice(device)
      _ = logger.info(s"Fetching path for $device")
    } yield PathForDeviceResponse.Ok(
      DevicePath(
        description = pathPositions.headOption.map(p => s"${p.app} ${p.deviceType} ${p.deviceSerial}").getOrElse("?"),
        lastSeen = pathPositions.headOption.map(p =>
          humanReadable(LocalDateTime.now().atOffset(ZoneOffset.UTC).toInstant.toEpochMilli - p.recorded) ).getOrElse("?"),
        positions = pathPositions
          .distinctBy(_.recorded)
          .map(p => DevicePath.Positions(p.latitude.doubleValue, p.longitude.doubleValue))
          .toVector
      )
    )

  private val units = List((TimeUnit.DAYS, "d"), (TimeUnit.HOURS, "h"), (TimeUnit.MINUTES, "m"))
  private val JustNow = (1 minute).toMillis

  private def humanReadable(timediff :Long):String = {
    if (timediff < JustNow) {
      "Now"
    } else {
      val init = ("", timediff)
      units.foldLeft(init) { case (acc, next) =>
        val (human, rest) = acc
        val (unit, name) = next
        val res = unit.convert(rest, TimeUnit.MILLISECONDS)
        val str = if (res > 0) human + " " + res + " " + name else human
        val diff = rest - TimeUnit.MILLISECONDS.convert(res, unit)
        (str, diff)
      }._1
    }
  }

  def incomingEvent(respond: IncomingEventResponse.type)(e: TtnEvent): F[IncomingEventResponse] = {
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
      battery = p.capacity.toInt,
      temperature = p.temperature,
      counter = e.counter
    )
    system ! Trackers.UpdatePosition(position)

    for {
      _ <- positions.add(position)
    } yield IncomingEventResponse.Created
  }
}
