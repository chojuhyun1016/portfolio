package org.example.order.api.master.utils;

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
 * REST Docs: 테스트에서 document(...) 호출 시 공통 전처리로 사용
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
