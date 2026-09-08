package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.vanillabp.integration.spi.AggregatePersistenceAware;
import jakarta.enterprise.context.ApplicationScoped;

/** Where the second workflow aggregate is kept, in memory like the first. */
@ApplicationScoped
public class SecondAggregatePersistence implements AggregatePersistenceAware<SecondAggregate> {

  private final Map<Long, SecondAggregate> aggregates = new ConcurrentHashMap<>();

  private final AtomicLong ids = new AtomicLong();

  @Override
  public Class<SecondAggregate> getAggregateClass() {

    return SecondAggregate.class;

  }

  @Override
  public SecondAggregate save(
      final SecondAggregate aggregate) {

    if (aggregate.getId() == null) {
      aggregate.setId(ids.incrementAndGet());
    }
    aggregates.put(aggregate.getId(), aggregate);
    return aggregate;

  }

  @Override
  public SecondAggregate loadById(
      final Object aggregateId) {

    return aggregates.get(aggregateId);

  }

  @Override
  public Object getAggregateId(
      final SecondAggregate aggregate) {

    return aggregate.getId();

  }

}
