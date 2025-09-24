package org.example.order.core.infra.dynamo.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DynamoDB 프로퍼티 (심플)
 * ------------------------------------------------------------------------
 * - enabled          : 전역 스위치 (true일 때만 모듈 동작)
 * - endpoint         : 수동 엔드포인트(LocalStack/DDB Local 등, 예: http://localhost:4566)
 * - region           : 리전 (수동/자동 모두에서 사용 가능; 자동에서는 미지정 시 SDK 기본 탐지)
 * - accessKey/secretKey : StaticCredentialsProvider 용 (선택)
 * - tableName        : 레포지토리 기본 테이블명 (존재 시 OrderDynamoRepository 빈 등록)
 * - autoCreate       : 로컬 + true 일 때 테이블 생성 + 시드 적용
 * - migrationLocation: 마이그레이션 JSON 위치 (V{n}__*.json)
 * - seedLocation     : 시드 JSON 위치 (V{n}__*.json)
 * - createMissingGsi : 기존 테이블이 있을 때 누락된 GSI만 보강
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dynamodb")
public class DynamoDbProperties {

    private boolean enabled = false;

    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;

    private String tableName;

    private Boolean autoCreate = false;
    private String migrationLocation = "classpath:dynamodb/migration";
    private String seedLocation = "classpath:dynamodb/seed";
    private Boolean createMissingGsi;
}
