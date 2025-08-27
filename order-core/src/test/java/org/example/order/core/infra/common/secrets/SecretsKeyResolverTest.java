package org.example.order.core.infra.common.secrets;

import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

class SecretsKeyResolverTest {

    @Test
    void update_and_backup_behavior() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        CryptoKeySpec v1 = new CryptoKeySpec();
        v1.setAlgorithm("AES");
        v1.setKeySize(128);
        v1.setValue(std(16));

        resolver.updateKey("aes128", v1);

        byte[] cur1 = resolver.getCurrentKey("aes128");
        assertThat(cur1).isNotNull().hasSize(16);
        assertThat(resolver.getBackupKey("aes128")).isNull();

        CryptoKeySpec v2 = new CryptoKeySpec();
        v2.setAlgorithm("AES");
        v2.setKeySize(128);
        v2.setValue(std(16));

        resolver.updateKey("aes128", v2);

        byte[] cur2 = resolver.getCurrentKey("aes128");
        assertThat(cur2).isNotNull().hasSize(16);
        assertThat(resolver.getBackupKey("aes128")).isNotNull().hasSize(16);
        assertThat(cur2).isNotEqualTo(resolver.getBackupKey("aes128"));
    }

    @Test
    void getCurrentKey_without_set_throws() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();
        assertThatThrownBy(() -> resolver.getCurrentKey("missing"))
                .isInstanceOf(IllegalStateException.class);
    }
}
