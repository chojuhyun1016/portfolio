package org.example.order.core.infra.common.secrets;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.config.SecretsManualConfig;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

class SecretsManualConfigTest {

    @Test
    void manual_mode_beans_and_usage() {
        new ApplicationContextRunner()
                .withPropertyValues("secrets.enabled=true")
                .withConfiguration(UserConfigurations.of(SecretsManualConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SecretsKeyResolver.class);
                    assertThat(ctx).hasSingleBean(SecretsKeyClient.class);

                    SecretsKeyClient client = ctx.getBean(SecretsKeyClient.class);

                    CryptoKeySpec spec = new CryptoKeySpec();
                    spec.setAlgorithm("HMAC-SHA256");
                    spec.setKeySize(256);
                    spec.setValue(std(32));

                    client.setKey("hmac", spec);

                    byte[] cur = client.getKey("hmac");
                    assertThat(cur).isNotNull().hasSize(32);
                    assertThat(client.getBackupKey("hmac")).isNull();

                    // 업데이트 → 백업 존재
                    CryptoKeySpec spec2 = new CryptoKeySpec();
                    spec2.setAlgorithm("HMAC-SHA256");
                    spec2.setKeySize(256);
                    spec2.setValue(std(32));
                    client.setKey("hmac", spec2);
                    assertThat(client.getBackupKey("hmac")).isNotNull().hasSize(32);
                });
    }
}
