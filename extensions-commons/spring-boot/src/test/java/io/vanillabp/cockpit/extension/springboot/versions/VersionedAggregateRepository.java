package io.vanillabp.cockpit.extension.springboot.versions;

import org.springframework.data.jpa.repository.JpaRepository;

/** Where this application keeps its workflow aggregates. */
public interface VersionedAggregateRepository extends JpaRepository<VersionedAggregate, Long> {
}
