DROP TABLE IF EXISTS `lect`;

CREATE TABLE IF NOT EXISTS `lect` (
  `no` bigint(20) NOT NULL AUTO_INCREMENT,
  `lecturer` varchar(32) NOT NULL,
  `location` varchar(256) NOT NULL,
  `max_appl` int(11) NOT NULL,
  `time` datetime DEFAULT NULL,
  `content` varchar(4096) DEFAULT NULL,
  `reg_dt` datetime DEFAULT NULL,
  PRIMARY KEY (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='강의정보';

DROP TABLE IF EXISTS `appl`;

CREATE TABLE IF NOT EXISTS `appl` (
  `no` bigint(20) NOT NULL AUTO_INCREMENT,
  `let_no` bigint(20) NOT NULL,
  `appl_no` varchar(5) NOT NULL,
  `reg_dt` datetime NOT NULL,
  PRIMARY KEY (`no`,`let_no`,`appl_no`),
  KEY `appl_idx_01` (`let_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;