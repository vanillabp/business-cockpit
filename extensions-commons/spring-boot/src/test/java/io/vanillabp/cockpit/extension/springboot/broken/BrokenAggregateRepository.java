package io.vanillabp.cockpit.extension.springboot.broken;

import org.springframework.data.jpa.repository.JpaRepository;

/** What VanillaBP persists the aggregate of those applications with. */
public interface BrokenAggregateRepository extends JpaRepository<BrokenAggregate, Long> {
}
