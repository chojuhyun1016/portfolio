package org.example.order.core.infra.common.secrets;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.config.SecretsManualConfig;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

/**
 * 통합 테스트 (수동 모드):
 * - @SpringBootTest + SecretsManualConfig 만 로드
 * - Client setKey/getKey/backup 흐름 e2e 검증
 */
@SpringBootTest
@Import(SecretsManualConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecretsManualIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("secrets.enabled", () -> "true");
    }

    @org.springframework.beans.factory.annotation.Autowired
    SecretsKeyClient client;

    @Test
    void set_and_update_then_backup_exists() {
        CryptoKeySpec spec1 = new CryptoKeySpec();
        spec1.setAlgorithm("AES");
        spec1.setKeySize(128);
        spec1.setValue(std(16));

        client.setKey("aes128", spec1);
        assertThat(client.getKey("aes128")).hasSize(16);
        assertThat(client.getBackupKey("aes128")).isNull();

        CryptoKeySpec spec2 = new CryptoKeySpec();
        spec2.setAlgorithm("AES");
        spec2.setKeySize(128);
        spec2.setValue(std(16));

        client.setKey("aes128", spec2);
        assertThat(client.getKey("aes128")).hasSize(16);
        assertThat(client.getBackupKey("aes128")).isNotNull().hasSize(16);
    }
}
