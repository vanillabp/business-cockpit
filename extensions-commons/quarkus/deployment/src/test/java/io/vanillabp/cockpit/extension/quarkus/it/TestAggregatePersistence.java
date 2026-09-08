package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.vanillabp.integration.spi.AggregatePersistenceAware;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Where the test application keeps its workflow aggregates. It is in memory rather than in a
 * database, because what is under test is the extension and not a persistence.
 */
@ApplicationScoped
public class TestAggregatePersistence implements AggregatePersistenceAware<TestAggregate> {

  private final Map<Long, TestAggregate> aggregates = new ConcurrentHashMap<>();

  private final AtomicLong ids = new AtomicLong();

  @Override
  public Class<TestAggregate> getAggregateClass() {

    return TestAggregate.class;

  }

  @Override
  public TestAggregate save(
      final TestAggregate aggregate) {

    if (aggregate.getId() == null) {
      aggregate.setId(ids.incrementAndGet());
    }
    aggregates.put(aggregate.getId(), aggregate);
    return aggregate;

  }

  @Override
  public TestAggregate loadById(
      final Object aggregateId) {

    return aggregates.get(aggregateId);

  }

  @Override
  public Object getAggregateId(
      final TestAggregate aggregate) {

    return aggregate.getId();

  }

  /**
   * @param id The aggregate's id
   * @return What is stored under it
   */
  public TestAggregate byId(
      final Long id) {

    return aggregates.get(id);

  }

}
