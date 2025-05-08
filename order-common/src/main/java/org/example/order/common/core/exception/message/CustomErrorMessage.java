package org.example.order.common.core.exception.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.common.core.messaging.code.MonitoringLevel;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomErrorMessage {
    private Integer code;
    private String msg;
    private Long timestamp;
    private MonitoringLevel level;

    public CustomErrorMessage(String msg, MonitoringLevel level) {
        this.msg = msg;
        this.timestamp = DateTimeUtils.nowTime();
        this.level = level;
    }

    public Integer getLevelCode() {
        return this.level.getLevel();
    }

    public static CustomErrorMessage toMessage(Integer code, Exception e) {
        return new CustomErrorMessage(code, getErrorMessage(e), DateTimeUtils.nowTime(), MonitoringLevel.WARN);
    }

    public static CustomErrorMessage toMessage(Exception e) {
        return new CustomErrorMessage(getErrorMessage(e), MonitoringLevel.DANGER);
    }

    public static String getErrorMessage(Exception e) {
        return String.format("%s\n%s", e.getMessage(), e);
    }
}
