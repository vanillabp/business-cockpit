package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** The store the application named for its second workflow aggregate. */
@ApplicationScoped
public class SecondAggregateOutboxAware implements PhaseTwoOutboxAware<SecondAggregate> {

  @Inject
  SecondRecordingOutbox outbox;

  @Override
  public Class<SecondAggregate> getAggregateClass() {

    return SecondAggregate.class;

  }

  @Override
  public PhaseTwoOutbox getPhaseTwoOutbox() {

    return outbox;

  }

}
