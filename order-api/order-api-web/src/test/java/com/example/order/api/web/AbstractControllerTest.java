package com.example.order.api.web;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 슬라이스 테스트 공통 부모
 * - MockMvc / REST Docs 구성
 * - 공통 자동구성(보안/로깅/Web/포맷) 비활성화해 슬라이스 간섭 제거
 */
@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@TestPropertySource(properties = {
        "order.api.infra.security.enabled=false",
        "order.api.infra.logging.enabled=false",
        "order.api.infra.web.enabled=false",
        "order.api.infra.format.enabled=false",
        "spring.web.resources.add-mappings=false",
        "spring.mvc.throw-exception-if-no-handler-found=true",
        "spring.web.resources.chain.enabled=false"
})
public abstract class AbstractControllerTest {
    @Autowired
    protected MockMvc mockMvc;
}
