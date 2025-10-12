package org.example.order.core.support.mapping;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.mapstruct.Named;

import java.time.LocalDateTime;

public final class TimeMapper {

    private TimeMapper() {
    }

    /**
     * LocalDateTime -> epoch millis
     */
    @Named("localDateTimeToEpochMillis")
    public static Long localDateTimeToEpochMillis(LocalDateTime dt) {
        return dt == null ? null : DateTimeUtils.localDateTimeToLong(dt);
    }

    /**
     * epoch millis -> LocalDateTime
     */
    @Named("epochMillisToLocalDateTime")
    public static LocalDateTime epochMillisToLocalDateTime(Long epochMillis) {
        return epochMillis == null ? null : DateTimeUtils.longToLocalDateTime(epochMillis);
    }
}
