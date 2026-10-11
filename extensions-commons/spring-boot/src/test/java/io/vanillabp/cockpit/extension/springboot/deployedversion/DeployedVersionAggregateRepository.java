package io.vanillabp.cockpit.extension.springboot.deployedversion;

import org.springframework.data.jpa.repository.JpaRepository;

/** What VanillaBP persists the aggregate of those applications with. */
public interface DeployedVersionAggregateRepository extends JpaRepository<DeployedVersionAggregate, Long> {
}
