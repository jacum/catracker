function Decoder(bytes, port) {
  var value=bytes[0]<<24 | bytes[1]<<16 | bytes[2]<<8 | bytes[3];
  if(bytes[0] & 0x80) { value |=0xFFFFFFFF00000000; }
  var latitude=value/1000000;
  value=bytes[4]<<24 | bytes[5]<<16 | bytes[6]<<8 | bytes[7];
  if(bytes[4] & 0x80) { value |=0xFFFFFFFF00000000; }
  var longitude=value/1000000;//gps longitude,units: °
  var alarm=(bytes[8] & 0x40)?"TRUE":"FALSE";//Alarm status
  value=((bytes[8] & 0x3f) <<8) | bytes[9];
  var batV=value/1000;//Battery,units:V
  value=(bytes[10] & 0xC0);
  if(value==0x40) { var motion_mode="Move"; }
  else if(value==0x80) { motion_mode="Collide"; }
  else if(value==0xC0) { motion_mode="User"; }
  else { motion_mode="Disable"; }
  var led_updown=(bytes[10] & 0x20)?"ON":"OFF";
  value=bytes[11]<<8 | bytes[12];
  if(bytes[11] & 0x80)
  {
  value |=0xFFFF0000;
  }
  var roll=value/100;//roll,units: °
  value=bytes[13]<<8 | bytes[14];
  if(bytes[13] & 0x80)
  {
  value |=0xFFFF0000;
  }
  var pitch=value/100; //pitch,units: °
  var params = {};
        params.latitude = latitude;
        params.longitude = longitude;
         params.gnss_fix = (params.latitude + params.longitude) > 0;
        params.accuracy = 0;
        params.temperature = 0;
        params.capacity = 0;
        params.voltage = batV;
        params.port=port;

        return params;

}