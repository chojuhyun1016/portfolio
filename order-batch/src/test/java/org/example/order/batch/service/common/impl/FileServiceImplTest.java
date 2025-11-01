//package org.example.order.batch.service.common.impl;
//
//import static org.mockito.Mockito.*;
//
//import java.io.File;
//
//import org.example.order.batch.service.common.S3Service;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
///**
// * FileServiceImpl 테스트
// * - 객체를 임시 파일로 직렬화 → S3 업로드 위임
// */
//class FileServiceImplTest {
//
//    @Test
//    @DisplayName("upload: 임시 파일 생성 후 S3 업로드 위임")
//    void upload_should_convert_and_call_s3() {
//        S3Service s3 = mock(S3Service.class);
//        FileServiceImpl svc = new FileServiceImpl(s3);
//
//        Object sample = new Sample("abc", 123);
//        svc.upload("f.json", ".json", sample);
//
//        verify(s3, atLeastOnce()).upload(eq("f.json"), any(File.class));
//    }
//
//    private record Sample(String a, int b) {}
//}
