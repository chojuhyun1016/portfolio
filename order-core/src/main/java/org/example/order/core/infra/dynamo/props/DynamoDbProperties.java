package org.example.order.core.infra.dynamo.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DynamoDB 프로퍼티 (심플)
 * <p>
 * - enabled     : 전역 스위치 (true일 때만 모듈 동작)
 * - endpoint    : 수동 구성 조건 중 하나 (LocalStack 등, 예: http://localhost:4566)
 * - region      : 리전 (수동/자동 모두에서 사용 가능; 자동에서는 미지정 시 SDK 기본 탐지)
 * - accessKey   : 수동 구성 조건 (StaticCredentials) — secretKey 와 함께 설정해야 유효
 * - secretKey   : 수동 구성 조건 (StaticCredentials)
 * - tableName   : 존재할 때만 OrderDynamoRepository 빈 등록 (기본값/시딩 없음)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dynamodb")
public class DynamoDbProperties {

    /**
     * 전역 ON/OFF
     */
    private boolean enabled = false;

    /**
     * 수동 구성(Manual) 관련 옵션
     */
    private String endpoint;   // ex) http://localhost:4566
    private String region;     // ex) ap-northeast-2
    private String accessKey;  // access+secret 둘 다 있으면 StaticCredentials
    private String secretKey;

    /**
     * 리포지토리 사용 시 필수: 명시적 테이블명 (없으면 미등록)
     */
    private String tableName;
}
