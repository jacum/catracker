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

/*
function Decoder(bytes, port) {
    var params = {
        "bytes": bytes
    };

    bytes = bytes.slice(bytes.length-11);

      if ((bytes[0] & 0x8) === 0) {
        params.gnss_fix = true;
      } else {
        params.gnss_fix = false;
      }

      // Mask off enf of temp byte, RFU
      temp = bytes[2] & 0x7f;

      acc = bytes[10] >> 5;
      acc = Math.pow(2, parseInt(bytes(10) >> 5) + 2);

      // Mask off end of accuracy byte, so lon doesn't get affected
      bytes[10] &= 0x1f;

      if ((bytes[10] & (1 << 4)) !== 0) {
        bytes[10] |= 0xe0;
      }

      // Mask off end of lat byte, RFU
      bytes[6] &= 0x0f;

      lat = bytes[6] << 24 | bytes[5] << 16 | bytes[4] << 8  | bytes[3];
      lon = bytes[10] << 24 | bytes[9] << 16 | bytes[8] << 8  | bytes[7];

      battery = bytes[1];
      capacity = battery >> 4;
      voltage = battery & 0x0f;

      params.latitude = lat/1000000;
      params.longitude = lon/1000000;
      params.accuracy = acc;
      params.temperature = temp - 32;
      params.capacity = (capacity / 15) * 100;
      params.voltage = (25 + voltage)/10;
      params.port=port;

      return params;

}
 */
