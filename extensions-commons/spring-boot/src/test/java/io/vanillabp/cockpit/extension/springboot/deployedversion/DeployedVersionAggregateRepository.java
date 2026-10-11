package io.vanillabp.cockpit.extension.springboot.deployedversion;

import org.springframework.data.jpa.repository.JpaRepository;

/** The repository VanillaBP uses to persist the aggregate of these applications. */
public interface DeployedVersionAggregateRepository extends JpaRepository<DeployedVersionAggregate, Long> {
}
