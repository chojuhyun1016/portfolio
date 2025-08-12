package org.example.order.api.master.utils;

import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

public final class ApiDocumentUtils {
    private ApiDocumentUtils() {}

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
