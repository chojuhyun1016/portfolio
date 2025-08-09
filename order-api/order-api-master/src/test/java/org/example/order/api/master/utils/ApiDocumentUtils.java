package org.example.order.api.master.utils;

import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * REST Docs 출력 전후 전처리 유틸
 * - 요청/응답 JSON을 pretty print
 * - 필요시 민감 헤더 제거 등 헤더 전처리 추가 가능
 */
public final class ApiDocumentUtils {

    private ApiDocumentUtils() {
    }

    /**
     * 요청 전처리
     * - pretty print
     * - 게이트웨이 시크릿 등 민감 헤더 제거 예시 포함
     */
    public static OperationRequestPreprocessor getDocumentRequest() {
        return preprocessRequest(
                modifyHeaders().remove("X-Internal-Auth"),
                prettyPrint()
        );
    }

    /**
     * 응답 전처리
     * - pretty print
     */
    public static OperationResponsePreprocessor getDocumentResponse() {
        return preprocessResponse(prettyPrint());
    }
}
