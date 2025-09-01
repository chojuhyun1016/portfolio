package org.example.order.common.core.exception.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonExceptionCode implements ExceptionCodeEnum {
    UNKNOWN_SERVER_ERROR(1000, "Unknown server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),
    NOT_FOUND_RESOURCE(1002, "Not found resource", HttpStatus.NOT_FOUND),
    DATA_PARSING_ERROR(1003, "Data parsing error", HttpStatus.INTERNAL_SERVER_ERROR),
    FAIL_TO_WRITE_CSV_FILE(1004, "Fail to write csv file", HttpStatus.INTERNAL_SERVER_ERROR),
    UPLOAD_FILE_TO_S3_ERROR(1005, "Error while uploading file to Aws S3", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_READ_ERROR(1006, "Data read error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_WRITE_ERROR(1007, "Data writing error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_LOCK_ERROR(1008, "Database lock error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_API_KEY(1009, "Invalid api key", HttpStatus.BAD_REQUEST),
    MISSING_API_KEY(1010, "Api key is required", HttpStatus.BAD_REQUEST);

    private int code;
    private String msg;
    private HttpStatus httpStatus;

    CommonExceptionCode(int code, String msg, HttpStatus httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }
}
