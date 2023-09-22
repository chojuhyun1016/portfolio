
동시성 문제 : DB 동시성 이슈 해결 샘플
              Mysql의 named lock(distributed lock)을 이용하여 해결
              주요 소스 : com.example.named.lock.rsv.lecture.lock.NamedLock.java
                        , com.example.named.lock.rsv.lecture.service.impl.LectureService.java
              rsv_cli : 동시성 문제를 일으키기위한 클라이언트
              rsv_srv : 예약 구축 시스템 서버

모바일 전자고지 : 현업 담당 서비스 백엔드 소스 일부(개발PM, 운영PM, 백엔드 개발)
                  서비스 설계, DB 스키마 설계, 백엔드 개발
                  Web, WAS, Git(형상), Jenkins(자동 빌드/배포), Redis, SSL, 이중화 구성

프로젝트 기본 : 프로젝트에 기본이 되는 기능 모음(정리)
