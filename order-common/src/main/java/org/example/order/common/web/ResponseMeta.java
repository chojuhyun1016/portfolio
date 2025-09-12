package org.example.order.common.web;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class ResponseMeta {

    private Integer code;
    private String msg;
    private LocalDateTime timestamp;

    public static ResponseMeta of(Integer code, String msg) {
        return new ResponseMeta(code, msg, LocalDateTime.now());
    }

    public static ResponseMeta ok() {
        return of(HttpStatus.OK.value(), HttpStatus.OK.name());
    }
}
