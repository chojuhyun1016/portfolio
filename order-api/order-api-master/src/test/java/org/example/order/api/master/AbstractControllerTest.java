package org.example.order.api.master;

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
// REST Docs: 스니펫 생성 경로 및 표시용 기본 URI 옵션
@AutoConfigureRestDocs(
        outputDir = "build/generated-snippets",
        uriScheme = "https",                      // REST Docs: 문서 예시용 스킴
        uriHost = "api.example.com",              // REST Docs: 문서 예시용 호스트
        uriPort = 443                             // REST Docs: 문서 예시용 포트
)
@TestPropertySource(properties = {
        "order.api.infra.security.enabled=false",
        "order.api.infra.logging.enabled=false",
        "order.api.infra.web.enabled=false",
        "order.api.infra.format.enabled=false"
})
public abstract class AbstractControllerTest {
    @Autowired
    protected MockMvc mockMvc;
}
