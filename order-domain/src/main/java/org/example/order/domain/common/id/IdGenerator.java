package org.example.order.domain.common.id;

/**
 * 도메인 포트: 식별자 생성기
 * - 구현은 인프라 계층에서 제공(TSID 등)
 */
public interface IdGenerator {
    long nextId();
}
