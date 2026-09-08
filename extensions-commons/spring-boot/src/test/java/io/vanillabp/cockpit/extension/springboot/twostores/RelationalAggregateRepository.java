package io.vanillabp.cockpit.extension.springboot.twostores;

import org.springframework.data.jpa.repository.JpaRepository;

/** Where the first workflow aggregate is kept. */
public interface RelationalAggregateRepository extends JpaRepository<RelationalAggregate, Long> {
}
