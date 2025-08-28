package org.example.order.core.infra.common.idgen.tsid.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TSID 설정 프로퍼티
 * - prefix: "tsid"
 * - enabled=false 인 경우 모든 TSID 관련 빈/생성기는 미등록
 */
@Getter
@Setter
@ConfigurationProperties("tsid")
public class TsidProperties {

    /**
     * 모듈 사용 여부 (기본: false)
     */
    private boolean enabled = false;

    /**
     * 노드 비트 수 (기본: 10 → 0~1023)
     */
    private Integer nodeBits = 10;

    /**
     * 타임존 ID (예: "Asia/Seoul", 미지정 시 시스템 기본/ENV_TZ)
     */
    private String zoneId;

    /**
     * EC2 메타데이터(IMDSv2)에서 instance-id 우선 시도 여부 (기본: true 권장)
     */
    private boolean preferEc2Meta = true;
}
