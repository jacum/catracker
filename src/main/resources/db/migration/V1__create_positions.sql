CREATE TABLE positions (
  `id` SERIAL PRIMARY KEY,
  `recorded` BIGINT not null,
  `latitude` double NOT NULL DEFAULT 0,
  `longitude` double NOT NULL DEFAULT 0,
  `battery` TINYINT NOT NULL,
  `temperature` TINYINT NOT NULL,
  `app` VARCHAR(100),
  `deviceType` VARCHAR(100),
  `deviceSerial` VARCHAR(100),
  `positionFix`  tinyint(1) NOT NULL,
  `bestGateway` VARCHAR(100),
  `bestSNR` double NOT NULL DEFAULT 0,
  `counter` MEDIUMINT NOT NULL,
  INDEX(recorded),
  INDEX(deviceSerial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

