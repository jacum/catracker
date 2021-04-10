package nl.pragmasoft.catracker

import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import nl.pragmasoft.catracker.http.client.{Client, IncomingEventTtnResponse}
import nl.pragmasoft.catracker.http.client.definitions.TtnEvent
import org.http4s.client.blaze.BlazeClientBuilder

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Random

object MainTrackerSimulator extends IOApp with LazyLogging {

  override def run(args: List[String]): IO[ExitCode] = {

    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client =>
      val ttnPoster = new Client[IO]("http://localhost:8081/api/catracker") (implicitly[Async[IO]], client)
      def send: IO[IncomingEventTtnResponse] = ttnPoster.incomingEventTtn(createTtnEvent)
      def repeat: IO[Unit] = send >> timer.sleep(10 seconds) >> repeat
      repeat
    } .as(ExitCode.Success)

  }


  private def createTtnEvent: TtnEvent =
    TtnEvent("pragma_cats_dragino","dragino_test1","A840416B61826E5F",93,
      TtnEvent.PayloadFields(4,60,true,52.331956 + Random.between(-0.001,+0.001),4.944941 + Random.between(-0.001,+0.001),Some(2),0,3.685),
      TtnEvent.Metadata(LocalDateTime.now().atOffset(ZoneOffset.UTC),868.1,
        "LORA","SF7BW125","4/5",
        Vector(TtnEvent.Metadata.Gateways("eui-58a0cbfffe802a34",
          LocalDateTime.now().atOffset(ZoneOffset.UTC),0,-87,9.75))))
}
