package io.vanillabp.cockpit.extension.springboot.everytask;

import org.springframework.data.jpa.repository.JpaRepository;

/** Where this application keeps its workflow aggregates. */
public interface OrderAggregateRepository extends JpaRepository<OrderAggregate, Long> {
}
