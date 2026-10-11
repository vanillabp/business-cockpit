package io.vanillabp.cockpit.extension.springboot.broken;

import org.springframework.data.jpa.repository.JpaRepository;

/** The repository VanillaBP uses to persist the aggregate of these applications. */
public interface BrokenAggregateRepository extends JpaRepository<BrokenAggregate, Long> {
}
