package nl.pragmasoft.catracker

import cats.effect.IO
import io.circe
import io.circe.parser._
import nl.pragmasoft.catracker.Model.{PositionRepository, StoredPosition}
import nl.pragmasoft.catracker.http.definitions.{KpnEventRecord, TtnEvent}
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
      ) shouldBe
        Right(TtnEvent("pragma_cats_dragino","dragino_test1","A840416B61826E5F",93,
          TtnEvent.PayloadFields(0,0,false,52.331956,4.944941,Some(2),0,3.685),
          TtnEvent.Metadata(LocalDateTime.parse("2020-12-28T11:36:17.269018381Z", ISO_DATE_TIME).atOffset(ZoneOffset.UTC),868.1,
            "LORA","SF7BW125","4/5",
          Vector(TtnEvent.Metadata.Gateways("eui-58a0cbfffe802a34",
            LocalDateTime.parse("2020-12-28T11:36:17.480709075Z", ISO_DATE_TIME).atOffset(ZoneOffset.UTC),0,-87,9.75))),
      Some("https://integrations.thethingsnetwork.org/ttn-eu/api/v2/down/pragma_cats_dragino/catracker?key=ttn-account-v2.q3vCLU1Une4Z7lxiSy3P1ZG8cBfaxQB66AbnL02aHNg")))
    }

    "be parsed from Browan via KPN" in {
      KpnEvent.decode(
        parse(
          Source.fromResource("kpn-2.json").getLines().mkString("\n")
        ).left.map(f => fail(s"can't parse: $f")).merge.asArray.get
          .map(json => KpnEventRecord.decodeKpnEventRecord.decodeJson(json).left.map(f => fail(s"can't parse: $f")).merge)
      ) shouldBe
        Some(StoredPosition(1618049415,1618049415,"kpn","kpn","E8E1E10001060A56",52.331967,4.944089,false,"",0,93,16,19,0))
    }
  }

}
