package org.example.order.core.support.mapping;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.mapstruct.Named;

import java.time.LocalDateTime;

/**
 * 시간/타임스탬프 변환 공통 메서드
 * - MapStruct @Named 로 지정해 qualifiedByName 로 재사용
 */
public final class TimeMapper {

    private TimeMapper() {
    }

    /**
     * LocalDateTime -> epoch millis
     */
    @Named("toEpochMillis")
    public static Long toEpochMillis(LocalDateTime dt) {
        return dt == null ? null : DateTimeUtils.localDateTimeToLong(dt);
    }

    /**
     * epoch millis -> LocalDateTime
     */
    @Named("toLocalDateTime")
    public static LocalDateTime toLocalDateTime(Long epochMillis) {
        return epochMillis == null ? null : DateTimeUtils.longToLocalDateTime(epochMillis);
    }
}
