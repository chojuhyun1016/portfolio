package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.TsidFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// ★ 추가: 자동설정 제외용
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
// ❌ [변경] redisson-spring-boot-starter의 클래스를 test 컴파일에서 참조하지 않도록 import 제거
// import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스프링 최소 컨텍스트로 TsidFactory 빈을 주입받아 사용하는 패턴 검증.
 *
 * - JPA/DB 불필요. TsidConfig + TsidIdService 만 로드
 * - 운영 코드와 동일하게 “서비스에서 주입받아 바로 사용” 시나리오 확인
 */
@SpringBootTest(
        classes = TsidFactorySpringBootTest.Boot.class
        // ★ (변경) redisson 자동설정 제외를 properties로 하지 않음
)
// ★ Redis 계열 자동설정은 클래스 리터럴로 제외 (부트 기본 autoconfigure에 포함)
@ImportAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisReactiveAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
class TsidFactorySpringBootTest {

    @org.springframework.beans.factory.annotation.Autowired
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

    // ------------------------------------------------------------
    // 스프링 부트 최소 구성:
    // - @SpringBootConfiguration + @EnableAutoConfiguration 로 부팅
    // - TsidConfig, TsidIdService 만 명시적으로 Import
    // - ★ redisson 자동설정은 **이름 기반 excludeName** 으로 안전하게 제외
    //   (클래스패스에 없어도 에러 없음)
    // ------------------------------------------------------------
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.redisson.spring.starter.RedissonAutoConfigurationV2" // ★ 핵심
    })
    @Import({TsidConfig.class, TsidIdService.class})
    static class Boot { }

    // ------------------------------------------------------------
    // 테스트용 서비스:
    // - 운영 서비스에서 TsidFactory를 주입받아 사용하는 것과 동일한 형태
    // - 여기서는 테스트 클래스 안에 두어 간단히 사용
    // ------------------------------------------------------------
    @Service
    static class TsidIdService {
        private final TsidFactory factory;

        public TsidIdService(TsidFactory factory) {
            this.factory = factory;
        }

        /** 단건 Long */
        public long next() {
            return factory.create().toLong();
        }

        /** 단건 String (toString16 등 원하면 바꿔도 됨) */
        public String nextString() {
            return Long.toUnsignedString(factory.create().toLong());
        }

        /** N개 Long */
        public List<Long> nextN(int n) {
            List<Long> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(next());
            return list;
        }

        /** N개 String */
        public List<String> nextStringN(int n) {
            List<String> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(nextString());
            return list;
        }
    }
}
