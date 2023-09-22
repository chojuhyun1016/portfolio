# 개발 환경
  1. 개발언어
     -> Java 1.8 (openjdk-1.8.0)
  
  2. 프레임워크
    -> Spring Boot 2.7.11

  3. RDBMS
    -> MariaDB

#. 데이타 설계
  - 강연 테이블, 강의 신청자 테이블
     참고: /resources/schema.sql, /resources/data.sql
 
# 고려 사항  
  - 이슈 
     강연 인원수 제한으로 인해 신청 시  동시성 이슈 발생
 
  - 해결 방안
    1. 비관적 락(Pessimistic Lock) 사용
       예시 : SELECT FOR UPDATE
       이슈 : 단순 조회성 조회까지 제한되므로 성능 이슈 발생
                 데드락 이슈 발생

    2. Transactio 격리 수준 상향
       예시 : @Transacntional(isolation = Isolation.SERIALIZABLE)
       이슈 : 억세스 제한으로 인한 성능 이슈

    3. 분산락
        1. Redis Redisson
           이슈 : 추가 Redis 환경 구축

        2. Redis Lettuce
           이슈 : 스핀락 으로 인한 Redis 부하 및 서비스 점유 문제
                     추가 Redis 환경 구축

        3. Named Lock
           이슈 : 데드락, 트랜잭션 반납 로직 구현

  - 해결
    선택 : Named Lock

    이유 : 기존에 사용하던 DB 가 MariaDB 이므로 별도의 환경(Redis) 작업 불필요(Named Lock 지원)
              MariaDB 의 기본 격리레벨이 Repeatable Read 이므로 Non-repeatable read 해결 및
              gap lock 으로 인한 Phantom read 문제 까지 해결

    구현 : 락 전용 트랜잭션과 DB CRUD 트랜잭션 분리
              락 전용 인스턴스에서 트랜잭션 획득 및 반납 로직 구현
              (구현 코드 : /lecture/lock/NamedLock.java)

# API 목록
  1. 강연 목록
      - URL : /list/lect

  2. 실시간 인기 강연 목록
    - URL : /list/lect/pop

   3. 강연신청자 목록
     - URL : /list/app

  4. 강연 등록
    - URL : /reg/lect

  5. 강연 신청
    - URL : /reg/appl

  6. 강연 삭제
    - URL : /del/lect

  7. 강연 신청 취소
    - URL : /del/appl


# 기본 요청사항(필수개발사항)
   1) 강연장에 입장 가능한 인원 수는 강연마다 다릅니다.
      -> 강연 등록(URL : /reg/lect) -> max_appl 설정
       
   2) 강연신청 목록에는 강연시작시간 1주일 전에 노출, 강연시작시간 1일 후 노출 목록에서 제외
      -> 강연 목록(URL : /list/lect) -> enable_flag 설정("Y":강연시작 1주전 <= 현재시간 <= 강의시작 1일 후, "N":모두 노출)

   3) 강연신청시에는 1인 1좌석만 예약 가능합니다.
      -> 강연 등록(URL : /reg/appl) -> 1차 : 신청 여부 필터링, 2차: DB Pk Duplicate 로 방지
       
   4) 같은 강연의 중복신청은 불가합니다.(타 공연 신청 가능)
      -> 강연 등록(URL : /reg/appl) -> 1차 : 신청 여부 필터링, 2차: DB Pk Duplicate 로 방지

   5) 사번을 입력하면 해당 사번으로 신청된 강연목록 조회가 가능합니다.
      -> 강연 목록(URL : /list/lect) -> appl_no 설정(0 = 모두 조회, 0! = 설정 사번으로 신청한 강의 목록 조회)
       
   6) 조회한 신청한 강연정보를 취소할 수 있어야 합니다.
      -> 강연 신청 취소(URL : /del/appl)

   7) 강연 정보는 강연자, 강연장, 신청인원, 강연시간, 강연내용 입력이가능
      -> 강연 등록(URL : /reg/lect)

   8) 강연관련 백오피스 페이지에서는 강연 신청한 사람들의 사번 목록 조회가 가능
      -> 강연신청자 목록(URL : /list/appl)

   9) 실시간 인기 강연 메뉴가 있으며 해당 메뉴는 3일간 가장 신청이 많은 강연순으로 노출
      -> 실시간 인기 강연 목록(URL : /list/lect/pop)

# BackOffice
  1. 강연 목록(전체 강연 목록)
    -> 강연 목록 : /list/lect

  2. 강연 등록(강연자, 강연장, 신청인원, 강연시간, 강연내용 입력)
    -> 강연 등록 : /reg/lect
 
  3. 강연신청자 목록(강연별 신청한 사번 목록)
    -> 강연신청자 목록 : /list/app
