package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.common.idgen.tsid.config.TsidModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TsidFactorySpringBootTest.Boot.class
)
@TestPropertySource(properties = {
        // TSID 모듈 활성화
        "tsid.enabled=true"
})
@ImportAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisReactiveAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
class TsidFactorySpringBootTest {

    @Autowired
    TsidIdService tsids;

    @Test
    @DisplayName("서비스: 단건 TSID(Long) 생성")
    void service_next() {
        long id = tsids.next();

        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("서비스: N개 TSID(Long) 생성 시 전부 유니크")
    void service_nextN_distinct() {
        List<Long> ids = tsids.nextN(500);

        assertThat(ids).hasSize(500);
        assertThat(ids.stream().distinct().count()).isEqualTo(500);
    }

    @Test
    @DisplayName("서비스: 단건 TSID(String) 생성")
    void service_nextString() {
        String id = tsids.nextString();

        assertThat(id).isNotBlank();
    }

    @Test
    @DisplayName("서비스: N개 TSID(String) 생성 시 전부 유니크")
    void service_nextStringN_distinct() {
        List<String> ids = tsids.nextStringN(300);

        assertThat(ids).hasSize(300);
        assertThat(ids.stream().distinct().count()).isEqualTo(300);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.redisson.spring.starter.RedissonAutoConfigurationV2"
    })
    @Import({TsidModuleConfig.class, TsidIdService.class})
    static class Boot {
    }

    @Service
    static class TsidIdService {
        private final TsidFactory factory;

        public TsidIdService(TsidFactory factory) {
            this.factory = factory;
        }

        public long next() {
            return factory.create().toLong();
        }

        public String nextString() {
            return Long.toUnsignedString(factory.create().toLong());
        }

        public List<Long> nextN(int n) {
            List<Long> list = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                list.add(next());
            }

            return list;
        }

        public List<String> nextStringN(int n) {
            List<String> list = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                list.add(nextString());
            }

            return list;
        }
    }
}
