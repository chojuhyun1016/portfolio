//package org.example.order.api.master;
//
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.restdocs.RestDocumentationExtension;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//
//@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
//@AutoConfigureMockMvc
//@AutoConfigureRestDocs(
//        outputDir = "build/generated-snippets",
//        uriScheme = "https",
//        uriHost = "api.example.com",
//        uriPort = 443
//)
//@TestPropertySource(properties = {
//        "order.api.infra.security.enabled=false",
//        "order.api.infra.logging.enabled=false",
//        "order.api.infra.web.enabled=false",
//        "order.api.infra.format.enabled=false"
//})
//public abstract class AbstractControllerTest {
//    @Autowired
//    protected MockMvc mockMvc;
//}
