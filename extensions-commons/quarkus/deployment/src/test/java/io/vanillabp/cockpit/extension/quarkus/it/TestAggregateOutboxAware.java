package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * What an application writes when it wants a store of its own for one workflow aggregate. The
 * extension has to follow it, because VanillaBP's own entries of that aggregate follow it too.
 */
@ApplicationScoped
public class TestAggregateOutboxAware implements PhaseTwoOutboxAware<TestAggregate> {

  @Inject
  RecordingOutbox outbox;

  @Override
  public Class<TestAggregate> getAggregateClass() {

    return TestAggregate.class;

  }

  @Override
  public PhaseTwoOutbox getPhaseTwoOutbox() {

    return outbox;

  }

}
