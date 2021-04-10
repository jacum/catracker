package nl.pragmasoft.catracker

import nl.pragmasoft.catracker.Model.StoredPosition
import nl.pragmasoft.catracker.http.definitions.KpnEventRecord
import org.apache.commons.codec.binary.Hex

import java.nio.{ByteBuffer, ByteOrder}

object KpnEvent {


  def decode(body: Vector[KpnEventRecord]): Option[StoredPosition] = {
    for {
      header <- body.find(_.bn.isDefined)
      payload <- body.find(_.n.contains("payload"))
      port <- body.find(_.n.contains("port"))
    } yield {
      val bytes = Hex.decodeHex(payload.vs.get.toCharArray)
      val byte10a: Byte = (bytes(10) & 0x1).toByte
      val byte10b: Byte = if ((byte10a & (1 << 4)) != 0) (byte10a | 0xe0).toByte else byte10a

      StoredPosition(
        recorded = header.bt.get.toLong,
        app = "kpn",
        deviceType = "kpn",
        deviceSerial = header.bn.get.split(':')(3),
        id = header.bt.get.toLong,
        latitude =  ByteBuffer.wrap(Array[Byte](bytes(6),bytes(5),bytes(4),bytes(3))).order(ByteOrder.BIG_ENDIAN).getInt.toDouble/1000000,
        longitude = ByteBuffer.wrap(Array[Byte](byte10b,bytes(9),bytes(8),bytes(7))).order(ByteOrder.BIG_ENDIAN).getInt.toDouble/1000000,
        positionFix = (bytes(0) & 0x8) == 0,
        bestGateway = "",
        bestSNR = 0,
        battery = (((bytes(1) & 0x0F).toDouble / 15) * 100).toInt,
        accuracy = Math.pow(2, (bytes(10) >> 5) + 2).toInt,
        temperature = bytes(2) & 0x7f - 32,
        counter = 0
      )
    }
  }
}
