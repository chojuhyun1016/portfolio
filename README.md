Portfolio Project
====

## Local 환경 구축
1. docker-compose 실행 
```
// docker 디렉토리로 이동
$ cd docker

// docker-compose 실행 
$ docker-compose up -d

// docker-compose 종료 
$ docker-compose down
```

2. local db 스키마 반영 
```
// core 모듈 하위 db 디렉토리로 이동 
$ cd ../core/src/main/resources/db/

// mysql 접속  
$ mysql -h 127.0.0.1 -P 3312 -u root -proot

// mysql 에서 db 디렉토리의 db 스키마 생성 파일 로드
MySQL> source order-db.sql
```
