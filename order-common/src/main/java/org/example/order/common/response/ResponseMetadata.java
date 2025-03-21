package org.example.order.common.response;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class ResponseMetadata {
    private Integer code;
    private String msg;
    private LocalDateTime timestamp;

    public static ResponseMetadata of(Integer code, String msg) {
        return new ResponseMetadata(code, msg, LocalDateTime.now());
    }

    public static ResponseMetadata ok() {
        return of(HttpStatus.OK.value(), HttpStatus.OK.name());
    }
}
