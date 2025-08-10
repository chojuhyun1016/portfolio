// src/test/java/com/example/order/api/web/AbstractControllerTest.java
package com.example.order.api.web;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@TestPropertySource(properties = {
        "order.api.infra.security.enabled=false",
        "order.api.infra.logging.enabled=false",
        "order.api.infra.web.enabled=false",
        "order.api.infra.format.enabled=false",
        "spring.web.resources.add-mappings=false"
})
public abstract class AbstractControllerTest {
    @Autowired protected MockMvc mockMvc;
}
