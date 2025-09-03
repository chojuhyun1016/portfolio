package org.example.order.batch.service.common.impl;

import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.model.S3Object;

import java.io.File;

import org.example.order.batch.service.common.S3Service;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.example.order.common.core.exception.core.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * S3ServiceImpl 테스트
 * - putObject/read 위임 및 예외 전파 확인
 */
class S3ServiceImplTest {

    @Test
    @DisplayName("upload: putObject 위임")
    void upload_should_call_putObject() {
        S3Client client = mock(S3Client.class);
        S3Properties props = new S3Properties();
        props.getS3().setBucket("b");
        props.getS3().setDefaultFolder("d");

        S3ServiceImpl svc = new S3ServiceImpl(client, props);
        svc.upload("f.json", new File("build.gradle"));

        verify(client, times(1)).putObject(eq("b"), eq("d/f.json"), any(File.class));
    }

    @Test
    @DisplayName("upload 실패: CommonException 전파")
    void upload_failure_should_throw_CommonException() {
        S3Client client = mock(S3Client.class);
        S3Properties props = new S3Properties();
        props.getS3().setBucket("b");
        props.getS3().setDefaultFolder("d");

        doThrow(new RuntimeException("S3 down"))
                .when(client).putObject(anyString(), anyString(), any(File.class));

        S3Service svc = new S3ServiceImpl(client, props);

        try {
            svc.upload("f.json", new File("x"));
        } catch (CommonException e) {
            return;
        }
        assert false : "CommonException 이 발생해야 합니다.";
    }

    @Test
    @DisplayName("read: getObject 위임")
    void read_should_delegate_getObject() {
        S3Client client = mock(S3Client.class);
        S3Properties props = new S3Properties();
        props.getS3().setBucket("b");
        props.getS3().setDefaultFolder("d");

        S3Object obj = new S3Object();
        when(client.getObject("b", "k")).thenReturn(obj);

        S3ServiceImpl svc = new S3ServiceImpl(client, props);
        S3Object got = svc.read("k");

        assert got == obj;
    }
}
