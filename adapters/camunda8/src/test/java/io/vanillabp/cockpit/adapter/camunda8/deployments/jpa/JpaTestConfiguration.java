package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test configuration for JPA integration tests.
 * Uses minimal configuration with only JPA components to avoid conflicts.
 */
@Configuration
@Import({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "io.vanillabp.cockpit.adapter.camunda8.deployments.jpa")
@EntityScan(basePackages = "io.vanillabp.cockpit.adapter.camunda8.deployments.jpa")
public class JpaTestConfiguration {
}
