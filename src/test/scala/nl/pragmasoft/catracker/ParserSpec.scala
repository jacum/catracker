package nl.pragmasoft.catracker

import cats.effect.IO
import io.circe
import io.circe.parser._
import nl.pragmasoft.catracker.Model.PositionRepository
import nl.pragmasoft.catracker.http.definitions.TtnEvent
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec

import scala.io.Source
import cats.effect.IO
import fs2.Stream
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Status, Uri, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.ZoneOffset

class ParserSpec extends AnyWordSpec with MockFactory with Matchers {

  import java.time.LocalDateTime
  import java.time.format.DateTimeFormatter._

  val dts = "2018-12-13T19:19:08.266120+00:00"
  LocalDateTime.parse(dts, ISO_DATE_TIME)
  "sample JSON" should {
    "be parsed from tabs tracker" in {

      TtnEvent.decodeTtnEvent.decodeJson(
        parse(
          Source.fromResource("http-ttn.json").getLines().mkString("\n")
        ).left.map(f => fail(s"can't parse: $f")).merge
      ) shouldBe Right(
        TtnEvent(
          "pragma_cats_tabs","tabs_test1",
          "58A0CB0000204688",85,
          TtnEvent.PayloadFields(
            16,93.33333333333333,false,52.331984,4.944248,Some(136),18,3.9),
          TtnEvent.Metadata(
            LocalDateTime.parse("2020-12-25T12:07:49.102008218Z", ISO_DATE_TIME).atOffset(ZoneOffset.UTC),
            868.5,"LORA","SF12BW125","4/5",
            Vector(TtnEvent.Metadata.Gateways(
              "eui-58a0cbfffe802a34",
              LocalDateTime.parse("2020-12-25T12:07:49.168767929Z", ISO_DATE_TIME).atOffset(ZoneOffset.UTC)
              ,0,-68,6.5))),
          Some("https://integrations.thethingsnetwork.org/ttn-eu/api/v2/down/pragma_cats_tabs/requestbin?key=ttn-account-v2.yG7tTCBkMQ8Ktg35n6rBsiUEGPIWLdf36mW_v_Mfwp8"))
      )
    }

    "be parsed from dragino tracker" in {

      TtnEvent.decodeTtnEvent.decodeJson(
        parse(
          Source.fromResource("http-ttn-dragino.json").getLines().mkString("\n")
        ).left.map(f => fail(s"can't parse: $f")).merge
      ) shouldBe Right(
        Right(TtnEvent("pragma_cats_tabs","tabs_test1","58A0CB0000204688",85,
          TtnEvent.PayloadFields(16,93.33333333333333,false,52.331984,4.944248,Some(136),18,3.9),
          TtnEvent.Metadata( LocalDateTime.parse("2020-12-25T12:07:49.102008218Z,868.5").atOffset(ZoneOffset.UTC),
            868.5, "LORA","SF12BW125","4/5",
            Vector(TtnEvent.Metadata.Gateways("eui-58a0cbfffe802a34","2020-12-25T12:07:49.168767929Z").atOffset(ZoneOffset.UTC),0,-68,6.5))),
          Some("https://integrations.thethingsnetwork.org/ttn-eu/api/v2/down/pragma_cats_tabs/requestbin?key=ttn-account-v2.yG7tTCBkMQ8Ktg35n6rBsiUEGPIWLdf36mW_v_Mfwp8")))
      )
    }
  }

}
