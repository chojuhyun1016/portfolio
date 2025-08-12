package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.aws.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecretsLoader / SecretsKeyResolver 통합 테스트
 */
class SecretsIntegrationTest {

    @Test
    @DisplayName("SecretsLoader가 정상적으로 키를 로드하고 Resolver에 반영한다")
    void testSecretsLoadingAndResolution() throws Exception {

        // given: 테스트용 keySpec 생성
        byte[] keyBytes = new byte[]{1, 2, 3, 4};
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm("AES");
        spec.setKeySize(keyBytes.length * 8);
        spec.setValue(Base64.getEncoder().encodeToString(keyBytes));

        Map<String, CryptoKeySpec> secretMap = Map.of("testKey", spec);
        String secretJson = new ObjectMapper().writeValueAsString(secretMap);

        // AWS SDK Client Mock
        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());

        // Properties
        SecretsManagerProperties props = new SecretsManagerProperties();
        props.setRegion("ap-northeast-2");
        props.setSecretName("dummySecret");
        props.setFailFast(true);

        SecretsKeyResolver resolver = new SecretsKeyResolver();

        SecretsLoader loader = new SecretsLoader(
                props,
                resolver,
                mockClient,
                Collections.emptyList()
        );

        // when: 로딩 실행
        loader.refreshSecrets();

        // then: Resolver에서 동일 키를 조회할 수 있어야 함
        assertArrayEquals(keyBytes, resolver.getCurrentKey("testKey"));
        assertNull(resolver.getBackupKey("testKey")); // 최초 로드는 백업 없음
    }

    @Test
    @DisplayName("SecretsKeyResolver가 키 업데이트 시 백업을 유지한다")
    void testKeyBackupOnUpdate() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        // 최초 등록
        CryptoKeySpec first = new CryptoKeySpec();
        first.setAlgorithm("AES");
        first.setKeySize(32);
        first.setValue(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
        resolver.updateKey("backupTest", first);

        // 새 키로 업데이트
        CryptoKeySpec second = new CryptoKeySpec();
        second.setAlgorithm("AES");
        second.setKeySize(32);
        second.setValue(Base64.getEncoder().encodeToString(new byte[]{9, 9, 9}));
        resolver.updateKey("backupTest", second);

        assertNotNull(resolver.getBackupKey("backupTest"));
        assertArrayEquals(new byte[]{1, 2, 3}, resolver.getBackupKey("backupTest"));
        assertArrayEquals(new byte[]{9, 9, 9}, resolver.getCurrentKey("backupTest"));
    }
}
