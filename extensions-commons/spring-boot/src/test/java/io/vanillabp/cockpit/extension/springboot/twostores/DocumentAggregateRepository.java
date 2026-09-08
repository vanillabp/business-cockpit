package io.vanillabp.cockpit.extension.springboot.twostores;

import org.springframework.data.jpa.repository.JpaRepository;

/** Where the second workflow aggregate is kept. */
public interface DocumentAggregateRepository extends JpaRepository<DocumentAggregate, Long> {
}
