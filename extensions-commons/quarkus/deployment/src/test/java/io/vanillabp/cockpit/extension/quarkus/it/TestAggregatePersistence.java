package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.vanillabp.integration.spi.AggregatePersistenceAware;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Where the test application keeps its workflow aggregates. It is in memory and not in a
 * database, because the extension is under test and not a persistence.
 */
@ApplicationScoped
public class TestAggregatePersistence implements AggregatePersistenceAware<TestAggregate> {

  private final Map<Long, TestAggregate> aggregates = new ConcurrentHashMap<>();

  private final Set<Long> saved = ConcurrentHashMap.newKeySet();

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
    saved.add(aggregate.getId());
    aggregates.put(aggregate.getId(), aggregate);
    return aggregate;

  }

  /**
   * Starts a fresh record of the saves, so that the save of a test's setup is not mistaken for
   * one of the run under test.
   */
  public void forgetSaves() {

    saved.clear();

  }

  /**
   * @param id The aggregate asked about
   * @return Whether somebody asked this persistence to save it since the last
   *         {@link #forgetSaves()}
   */
  public boolean sawSaveOf(
      final Long id) {

    return saved.contains(id);

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
