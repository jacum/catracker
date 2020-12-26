CREATE TABLE positions (
  `id` SERIAL PRIMARY KEY,
  `recorded` datetime not null,
  `latitude` double NOT NULL DEFAULT 0,
  `longitude` double NOT NULL DEFAULT 0,
  `battery` int(11) NOT NULL,
  `temperature` int(11) NOT NULL,
  `app` VARCHAR(100),
  `deviceType` VARCHAR(100),
  `deviceSerial` VARCHAR(100),
  `positionFix`  tinyint(1) NOT NULL,
  `bestGateway` VARCHAR(100),
  `bestSNR` double NOT NULL DEFAULT 0,
  `counter` int(11) NOT NULL,
  INDEX(recorded),
  INDEX(deviceSerial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

