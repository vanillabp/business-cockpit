package io.vanillabp.cockpit.extension.springboot.test;

import org.springframework.data.jpa.repository.JpaRepository;

/** Where the test application keeps its workflow aggregates. */
public interface TestAggregateRepository extends JpaRepository<TestAggregate, Long> {
}
