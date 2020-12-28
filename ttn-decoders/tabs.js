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
      acc = Math.pow(2, parseInt(acc) + 2);

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