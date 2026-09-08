package io.vanillabp.cockpit.extension.springboot.stores;

import io.vanillabp.cockpit.extension.springboot.test.TestAggregate;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;

/**
 * What an application writes when it wants a store of its own for one workflow aggregate. The
 * extension has to follow it, because VanillaBP's own entries of that aggregate follow it too.
 */
public class TestAggregateOutboxAware implements PhaseTwoOutboxAware<TestAggregate> {

  private final RecordingOutbox outbox;

  public TestAggregateOutboxAware(
      final RecordingOutbox outbox) {

    this.outbox = outbox;

  }

  @Override
  public Class<TestAggregate> getAggregateClass() {

    return TestAggregate.class;

  }

  @Override
  public PhaseTwoOutbox getPhaseTwoOutbox() {

    return outbox;

  }

}
