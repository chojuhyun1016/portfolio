
DROP TABLE IF EXISTS board;


CREATE TABLE IF NOT EXISTS board
(
    id      integer NOT NULL
  , title   varchar( 255 ) NOT null
  , content varchar( 4000 )
  , userId  varchar( 255 )
  , regDate timestamp
  , primary key( id )
);
