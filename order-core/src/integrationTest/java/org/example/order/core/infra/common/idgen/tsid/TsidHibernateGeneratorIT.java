package org.example.order.core.infra.common.idgen.tsid;

import jakarta.persistence.*;
import org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// ★ 추가: 자동설정 제외용
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합(경량) 테스트:
 * - 목적: @CustomTsid 가 Hibernate 식별자 생성기로 정상 동작하는지 검증
 * - 실제 MySQL/Testcontainers 불필요. H2 in-memory 로 부팅하여 엔티티 저장 시 ID 생성만 확인.
 */
@SpringBootTest(classes = TsidHibernateGeneratorIT.JpaBoot.class)
@TestPropertySource(properties = {
        // --- H2 전용 데이터소스 구성 (드라이버/방언까지 명시) ---
        "spring.datasource.url=jdbc:h2:mem:tsid-it;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
})
// ★ 핵심: Redisson/Redis 관련 자동설정 전부 제외(테스트 컨텍스트 격리)
@ImportAutoConfiguration(exclude = {
        RedissonAutoConfigurationV2.class,
        RedisAutoConfiguration.class,
        RedisReactiveAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
class TsidHibernateGeneratorIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = TsidHibernateGeneratorIT.OrderEntity.class)
    @EnableJpaRepositories(
            basePackageClasses = TsidHibernateGeneratorIT.OrderRepository.class,
            considerNestedRepositories = true
    )
    @Import(TsidConfig.class) // TsidFactory 빈 등록(제너레이터가 필요 시 참조)
    static class JpaBoot { }

    // ----- 테스트용 최소 엔티티 -----
    @Entity(name = "orders_tsid_it")
    static class OrderEntity {
        @Id @CustomTsid
        private Long id;
        private String memo;
        protected OrderEntity() {}
        OrderEntity(String memo) { this.memo = memo; }
        public Long getId() { return id; }
        public String getMemo() { return memo; }
    }

    interface OrderRepository extends JpaRepository<OrderEntity, Long> {}

    @Autowired
    private OrderRepository repo;

    @Test
    @DisplayName("@CustomTsid: JPA 저장 시 Long TSID가 자동 생성된다")
    void customTsidGeneratesOnPersist() {
        OrderEntity e = new OrderEntity("hello");
        OrderEntity saved = repo.saveAndFlush(e);
        assertThat(saved.getId()).isNotNull().isPositive();
    }

    @Test
    @DisplayName("@CustomTsid: 여러 건 저장해도 충돌 없이 고유 식별자가 생성된다")
    void customTsidUniqueAcrossMultipleSaves() {
        OrderEntity a = repo.saveAndFlush(new OrderEntity("a"));
        OrderEntity b = repo.saveAndFlush(new OrderEntity("b"));
        OrderEntity c = repo.saveAndFlush(new OrderEntity("c"));

        assertThat(a.getId()).isNotNull();
        assertThat(b.getId()).isNotNull();
        assertThat(c.getId()).isNotNull();

        assertThat(a.getId()).isNotEqualTo(b.getId());
        assertThat(b.getId()).isNotEqualTo(c.getId());
        assertThat(a.getId()).isNotEqualTo(c.getId());
    }
}
