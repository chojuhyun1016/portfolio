package org.example.order.common.core.exception.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.common.helper.datetime.DateTimeUtils;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomErrorMessage {
    private Integer code;
    private String msg;
    private Long timestamp;

    public CustomErrorMessage(String msg) {
        this.msg = msg;
        this.timestamp = DateTimeUtils.nowTime();
    }

    public static CustomErrorMessage toMessage(Integer code, Exception e) {
        return new CustomErrorMessage(code, getErrorMessage(e), DateTimeUtils.nowTime());
    }

    public static CustomErrorMessage toMessage(Exception e) {
        return new CustomErrorMessage(getErrorMessage(e));
    }

    public static String getErrorMessage(Exception e) {
        return String.format("%s\n%s", e.getMessage(), e);
    }
}
