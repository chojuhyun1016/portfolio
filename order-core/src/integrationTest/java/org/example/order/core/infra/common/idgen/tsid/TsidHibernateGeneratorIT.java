package org.example.order.core.infra.common.idgen.tsid;

import jakarta.persistence.*;
import org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid;
import org.example.order.core.infra.common.idgen.tsid.config.TsidModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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

@SpringBootTest(classes = TsidHibernateGeneratorIT.JpaBoot.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tsid-it;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        // ---- TSID 모듈 활성화 ----
        "tsid.enabled=true"
})
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
    @Import(TsidModuleConfig.class)
    static class JpaBoot {
    }

    @Entity(name = "orders_tsid_it")
    static class OrderEntity {
        @Id
        @CustomTsid
        private Long id;
        private String memo;

        protected OrderEntity() {
        }

        OrderEntity(String memo) {
            this.memo = memo;
        }

        public Long getId() {
            return id;
        }

        public String getMemo() {
            return memo;
        }
    }

    interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    }

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
