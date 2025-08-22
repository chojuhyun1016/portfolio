package org.example.order.core.infra.redis;

import org.example.order.core.infra.redis.repository.RedisRepository;
import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
import org.example.order.core.infra.redis.support.RedisSerializerFactory;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * embedded-redis 를 사용한 유닛 테스트
 * - 도커/네트워크 불필요, 로컬 프로세스로 Redis 기동
 * - 스프링 컨텍스트 최소화: 직접 LettuceConnectionFactory/RedisTemplate/Repository 생성
 * - 프로덕션 코드/주석은 한 줄도 변경하지 않음
 */
class RedisRepositoryTest {

    private static EmbeddedRedisHarness embedded; // ← 구현체 무관 래퍼
    private static int port;

    private static LettuceConnectionFactory cf;
    private static RedisTemplate<String, Object> template;
    private static RedisRepository repo;

    private static int randomPort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            return 6379; // fallback
        }
    }

    @BeforeAll
    static void startRedis() throws Exception {
        port = randomPort();
        embedded = EmbeddedRedisHarness.create(port,
                // 옵션은 구현체별로 없으면 무시
                "save \"\"",
                "appendonly no"
        );
        embedded.start();

        // 연결 팩토리
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration("127.0.0.1", port);
        standalone.setPassword(RedisPassword.none());

        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofSeconds(3))
                .build();

        cf = new LettuceConnectionFactory(standalone, client);
        cf.afterPropertiesSet();

        // 템플릿 (프로덕션과 동일한 직렬화 조합)
        template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        StringRedisSerializer key = new StringRedisSerializer();
        var json = RedisSerializerFactory.create("org.example.order");

        template.setKeySerializer(key);
        template.setValueSerializer(json);
        template.setHashKeySerializer(key);
        template.setHashValueSerializer(json);
        template.setEnableTransactionSupport(true); // tx 테스트용

        template.afterPropertiesSet();

        repo = new RedisRepositoryImpl(template);
    }

    @AfterAll
    static void stopRedis() {
        if (cf != null) cf.destroy();
        if (embedded != null) {
            try { embedded.stop(); } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void clean() {
        Set<String> keys = template.keys("ut:*");
        if (keys != null && !keys.isEmpty()) {
            template.delete(keys);
        }
    }

    @Test
    void value_hash_list_set_zset_ttl_tx_all_work() {
        final String PREFIX = "ut:";

        // ===== Value =====
        String vKey = PREFIX + "val:1";
        // 다형성 기본타이핑(WRAPPER_ARRAY)을 요구하는 GenericJackson2JsonRedisSerializer 설정과 충돌을 피하기 위해
        // 루트타입을 명확한 String 으로 저장 (Map 등 Object 루트는 역직렬화 시 타입정보 래퍼를 기대할 수 있음)
        repo.set(vKey, "plain-string-value");              // set
        assertThat(repo.get(vKey)).isEqualTo("plain-string-value");

        String vKeyTtl = PREFIX + "val:2";                 // set + TTL
        repo.set(vKeyTtl, "hello", 5);
        assertThat(repo.getExpire(vKeyTtl)).isGreaterThan(0);

        assertThat(repo.persist(vKeyTtl)).isTrue();        // TTL 제거
        Long persisted = repo.getExpire(vKeyTtl);
        assertThat(persisted == null || persisted == -1L).isTrue();

        // ===== Hash =====
        String hKey = PREFIX + "hash:1";
        repo.putHash(hKey, "f1", 10);
        repo.putAllHash(hKey, Map.of("f2", 20, "f3", 30));
        assertThat(repo.getHash(hKey, "f2")).isEqualTo(20);
        assertThat(repo.getAllHashValues(hKey)).contains(10, 20, 30);
        repo.deleteHash(hKey, "f1");
        assertThat(repo.getHash(hKey, "f1")).isNull();

        // ===== List =====
        String lKey = PREFIX + "list:1";
        repo.leftPush(lKey, "x");
        repo.leftPushAll(lKey, List.of("y", "z"));
        assertThat(repo.listSize(lKey)).isGreaterThanOrEqualTo(3);
        assertThat(repo.rightPop(lKey)).isIn("x", "y", "z");

        // ===== Set =====
        String sKey = PREFIX + "set:1";
        repo.addSet(sKey, "m1");
        repo.addAllSet(sKey, List.of("m2", "m3"));
        assertThat(repo.getSetMembers(sKey)).contains("m1", "m2", "m3");
        assertThat(repo.isSetMember(sKey, "m2")).isTrue();
        assertThat(repo.getSetSize(sKey)).isEqualTo(3);
        assertThat(repo.removeSet(sKey, "m1")).isEqualTo(1L);

        // ===== ZSet =====
        String zKey = PREFIX + "zset:1";
        assertThat(repo.zAdd(zKey, "a", 1.0)).isTrue();
        assertThat(repo.zAdd(zKey, "b", 2.0)).isTrue();
        assertThat(repo.zScore(zKey, "b")).isEqualTo(2.0);
        assertThat(repo.zRangeByScore(zKey, 1.0, 2.0)).contains("a", "b");
        assertThat(repo.zRemoveRangeByScore(zKey, 1.5, 2.0)).isGreaterThanOrEqualTo(1L);
        assertThat(repo.zCard(zKey)).isGreaterThanOrEqualTo(1L);
        assertThat(repo.zRemove(zKey, "a")).isEqualTo(1L);

        // ===== TTL =====
        String tKey = PREFIX + "ttl:1";
        repo.set(tKey, "ttl");
        assertThat(repo.expire(tKey, 3)).isTrue();
        Long ttl = repo.getExpire(tKey);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);

        // ===== KEYS =====
        String k1 = PREFIX + "keys:1";
        String k2 = PREFIX + "keys:2";
        repo.set(k1, "1");
        repo.set(k2, "2");
        Set<String> keys = repo.keys(PREFIX + "keys:*");
        assertThat(keys).isNotNull();
        assertThat(keys).contains(k1, k2);

        // ===== TX =====
        String th = PREFIX + "tx:hash";
        repo.transactionPutAllHash(th, Map.of("a", 1, "b", 2));
        assertThat(repo.getHash(th, "a")).isEqualTo(1);
        assertThat(repo.getHash(th, "b")).isEqualTo(2);

        String ts = PREFIX + "tx:set";
        repo.transactionAddSet(ts, List.of("x", "y", "z"));
        assertThat(repo.getSetMembers(ts)).contains("x", "y", "z");

        // ===== Delete =====
        assertThat(repo.delete(vKey)).isTrue();
        assertThat(repo.get(vKey)).isNull();
    }

    /**
     * 구현체에 종속되지 않는 임베디드 Redis 래퍼
     * - 1순위: com.github.codemonstur.embeddedredis.RedisServer (builder API)
     * - 2순위: redis.embedded.RedisServer (kstyrc, ctor API)
     */
    static final class EmbeddedRedisHarness {
        private final Object server;
        private final Method startMethod;
        private final Method stopMethod;

        private EmbeddedRedisHarness(Object server, Method startMethod, Method stopMethod) {
            this.server = server;
            this.startMethod = startMethod;
            this.stopMethod = stopMethod;
        }

        static EmbeddedRedisHarness create(int port, String... settings) throws Exception {
            // 1) codemonstur 포크 시도
            try {
                Class<?> cls = Class.forName("com.github.codemonstur.embeddedredis.RedisServer");
                Method newRedisServer = cls.getMethod("newRedisServer");
                Object builder = newRedisServer.invoke(null);

                // builder.port(int)
                Method portM = builder.getClass().getMethod("port", int.class);
                portM.invoke(builder, port);

                // builder.setting(String) — 선택적으로 모두 적용
                if (settings != null) {
                    for (String s : settings) {
                        try {
                            Method settingM = builder.getClass().getMethod("setting", String.class);
                            settingM.invoke(builder, s);
                        } catch (NoSuchMethodException ignored) { /* older builder */ }
                    }
                }

                // builder.build()
                Method build = builder.getClass().getMethod("build");
                Object srv = build.invoke(builder);

                Method start = srv.getClass().getMethod("start");
                Method stop  = srv.getClass().getMethod("stop");

                return new EmbeddedRedisHarness(srv, start, stop);
            } catch (ClassNotFoundException ignoreAndFallback) {
                // 2) kstyrc 포크 시도
                try {
                    Class<?> cls = Class.forName("redis.embedded.RedisServer");
                    Constructor<?> ctor = cls.getConstructor(int.class);
                    Object srv = ctor.newInstance(port);

                    Method start = cls.getMethod("start");
                    Method stop  = cls.getMethod("stop");

                    return new EmbeddedRedisHarness(srv, start, stop);
                } catch (ClassNotFoundException e2) {
                    throw new IllegalStateException(
                            "No embedded Redis implementation found. " +
                                    "Add either 'com.github.codemonstur:embedded-redis' or 'it.ozimov:embedded-redis' to test scope.", e2);
                }
            }
        }

        void start() throws Exception { startMethod.invoke(server); }
        void stop()  throws Exception { stopMethod.invoke(server); }
    }
}
