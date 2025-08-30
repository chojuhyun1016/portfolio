package org.example.order.core.infra.common.secrets;

import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

/**
 * SecretsKeyResolver 동작 검증 단위 테스트
 * - 키 업데이트 및 백업 처리 확인
 * - 설정되지 않은 키 조회 시 예외 발생 확인
 */
class SecretsKeyResolverTest {

    /**
     * 키를 업데이트 했을 때 현재 키와 백업 키가 어떻게 동작하는지 검증한다
     * - 최초 등록 시에는 현재 키만 존재하고 백업 키는 없다
     * - 두 번째 등록 시 기존 키는 백업 키로 이동하고 새로운 키가 현재 키가 된다
     */
    @Test
    void update_and_backup_behavior() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        // 첫 번째 키 생성 및 등록
        CryptoKeySpec v1 = new CryptoKeySpec();
        v1.setAlgorithm("AES");
        v1.setKeySize(128);
        v1.setValue(std(16));

        resolver.updateKey("aes128", v1);

        byte[] cur1 = resolver.getCurrentKey("aes128");
        assertThat(cur1).isNotNull().hasSize(16);
        assertThat(resolver.getBackupKey("aes128")).isNull();

        // 두 번째 키 생성 및 등록
        CryptoKeySpec v2 = new CryptoKeySpec();
        v2.setAlgorithm("AES");
        v2.setKeySize(128);
        v2.setValue(std(16));

        resolver.updateKey("aes128", v2);

        // 현재 키는 새 키로 교체되고 이전 키는 백업으로 이동한다
        byte[] cur2 = resolver.getCurrentKey("aes128");
        assertThat(cur2).isNotNull().hasSize(16);
        assertThat(resolver.getBackupKey("aes128")).isNotNull().hasSize(16);
        assertThat(cur2).isNotEqualTo(resolver.getBackupKey("aes128"));
    }

    /**
     * 등록되지 않은 키를 조회할 경우 예외가 발생하는지 검증한다
     */
    @Test
    void getCurrentKey_without_set_throws() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        assertThatThrownBy(() -> resolver.getCurrentKey("missing"))
                .isInstanceOf(IllegalStateException.class);
    }
}
