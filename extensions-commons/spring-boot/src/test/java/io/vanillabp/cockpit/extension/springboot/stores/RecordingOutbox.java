package io.vanillabp.cockpit.extension.springboot.stores;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;

/**
 * A store of the application, next to the one VanillaBP brings for the relational database.
 * It writes nowhere. The tests of the attribution ask which store was handed the entry, and an
 * entry which is never dispatched answers that as well as one which is.
 */
public class RecordingOutbox implements PhaseTwoOutbox {

  private final List<PhaseTwoCall> scheduled = new CopyOnWriteArrayList<>();

  @Override
  public boolean schedule(
      final PhaseTwoCall call) {

    scheduled.add(call);
    return true;

  }

  /**
   * A report of the extension is planned this way, so a store of an application answers it.
   * Inheriting the default would write a warning and discard what the extension wanted.
   */
  @Override
  public boolean scheduleReplacingWhatIsStillWaiting(
      final PhaseTwoCall call) {

    return schedule(call);

  }

  /**
   * @return What was written into this store
   */
  public List<PhaseTwoCall> getScheduled() {

    return scheduled;

  }

}
