
DROP TABLE IF EXISTS user;

CREATE TABLE IF NOT EXISTS user
(
    num    bigint auto_increment
  , id     varchar( 24 ) NOT NULL
  , pwd    varchar( 24 ) NOT NULL
  , primary key( num )
);
