package org.example.order.api.master;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 슬라이스 테스트 공통 부모
 * - AutoConfigureMockMvc: MockMvc 자동 주입 보장
 * - AutoConfigureRestDocs: REST Docs 스니펫 생성
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;
}
