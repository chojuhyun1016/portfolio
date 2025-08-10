// src/test/java/com/example/order/api/web/utils/ApiDocumentUtils.java
package com.example.order.api.web.utils;

import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

public final class ApiDocumentUtils {
    private ApiDocumentUtils() {}
    public static OperationRequestPreprocessor getDocumentRequest() {
        return preprocessRequest(modifyHeaders().remove("X-Internal-Auth"), prettyPrint());
    }
    public static OperationResponsePreprocessor getDocumentResponse() {
        return preprocessResponse(prettyPrint());
    }
}
