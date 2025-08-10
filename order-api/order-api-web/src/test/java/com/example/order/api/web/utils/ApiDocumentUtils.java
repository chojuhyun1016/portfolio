package com.example.order.api.web.utils;

import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * REST Docs 전처리 유틸
 * - 요청/응답 바디 pretty print
 * - 민감 헤더 제거 예시 포함
 */
public final class ApiDocumentUtils {
    private ApiDocumentUtils() {
    }

    public static OperationRequestPreprocessor getDocumentRequest() {
        return preprocessRequest(
                modifyHeaders().remove("X-Internal-Auth"),
                prettyPrint()
        );
    }

    public static OperationResponsePreprocessor getDocumentResponse() {
        return preprocessResponse(prettyPrint());
    }
}
