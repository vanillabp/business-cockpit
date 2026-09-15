package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;

/**
 * How the cockpit tells a report with news from a report which arrives late.
 * <p>
 * A workflow module reports through VanillaBP's outbox. That outbox puts its entries in no order.
 * It dispatches them in parallel, and an entry whose dispatch failed comes back after later entries
 * have gone through. So a change can reach the cockpit after the change which followed it, and an
 * end can reach it before the creation it ends. The reports do not say which of them is the younger
 * one, so the cockpit weighs them here.
 * <p>
 * It weighs by the timestamp of the event, never by the moment the report arrived. The report is
 * built when the outbox gets around to its entry, which can be much later. The event is what the
 * workflow module saw. See decision 18 in the repository's DECISIONS.md.
 */
public final class OrderOfReports {

    private OrderOfReports() {
    }

    /**
     * @param eventTimestamp When the event behind the report happened
     * @param latestEventStored When the event behind the state the cockpit holds happened, or
     *        {@code null} where the cockpit does not know
     * @return Whether the report is older than what the cockpit holds, and therefore says nothing
     *         about the task or the case which is still true
     */
    public static boolean isOlderThanWhatIsStored(
            final OffsetDateTime eventTimestamp,
            final OffsetDateTime latestEventStored) {

        // a report without a timestamp cannot be weighed. Neither can a state which was stored
        // before the cockpit kept the timestamp. Both are let through: a report the cockpit refuses
        // is lost for good
        if ((eventTimestamp == null)
                || (latestEventStored == null)) {
            return false;
        }
        return eventTimestamp.isBefore(latestEventStored);

    }

}
