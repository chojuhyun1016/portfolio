-- 글로벌/세션 UTC
SET PERSIST time_zone = '+00:00';
SET GLOBAL  time_zone = '+00:00';
SET SESSION time_zone = '+00:00';

-- DB / 계정 / 권한
CREATE DATABASE IF NOT EXISTS `order_local`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'order'@'%' IDENTIFIED BY 'order1234';
GRANT ALL PRIVILEGES ON `order_local`.* TO 'order'@'%';
FLUSH PRIVILEGES;
