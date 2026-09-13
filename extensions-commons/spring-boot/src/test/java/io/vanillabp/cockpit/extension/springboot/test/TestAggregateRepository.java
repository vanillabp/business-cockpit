package io.vanillabp.cockpit.extension.springboot.test;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Where the test application keeps its workflow aggregates.
 * <p>
 * It overrides <code>save</code> to write down who was saved, and does the save itself
 * afterwards. That is the only way a test can tell a save VanillaBP performed apart from the
 * write JPA's dirty checking produces on its own.
 */
public interface TestAggregateRepository extends JpaRepository<TestAggregate, Long> {

  @Override
  default <S extends TestAggregate> S save(
      final S aggregate) {

    PlatformSaves.note(aggregate.getToken());
    return saveAndFlush(aggregate);

  }

}
