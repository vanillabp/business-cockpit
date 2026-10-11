package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A store of the application, next to the one VanillaBP brings for the datasource of the test.
 * It writes nowhere. The attribution tests only ask which store got the entry, and an entry which
 * is never dispatched answers that just as well as one which is.
 */
@ApplicationScoped
public class RecordingOutbox implements PhaseTwoOutbox {

  private final List<PhaseTwoCall> scheduled = new CopyOnWriteArrayList<>();

  @Override
  public boolean schedule(
      final PhaseTwoCall call) {

    scheduled.add(call);
    return true;

  }

  /**
   * The extension plans its reports this way, so a store of an application has to implement it.
   * The inherited default would log a warning and drop what the extension wanted.
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
