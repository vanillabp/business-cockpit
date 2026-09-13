package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;

/**
 * How the cockpit tells a report which brings news from one which is only arriving late.
 * <p>
 * A workflow module reports through VanillaBP's outbox, and that outbox gives its entries no order.
 * They are dispatched in parallel, and an entry whose dispatch failed comes back after the entries
 * planned later have gone through. So a change can reach the cockpit after the change which followed
 * it, and an end can reach it before the creation it ends. Nothing in the reports themselves fixes
 * that, which is why the cockpit weighs them here.
 * <p>
 * What it weighs by is the timestamp of the event, never the moment the report arrived: the report
 * is built when its outbox entry is dispatched, which is as late as the outbox happens to get to it,
 * while the event is what the workflow module saw. See decision 18 in the repository's DECISIONS.md.
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

        // a report without a timestamp cannot be weighed, and a state stored before the cockpit kept
        // the timestamp cannot be weighed against: both are let through, because refusing a report
        // the cockpit cannot judge would lose it for good
        if ((eventTimestamp == null)
                || (latestEventStored == null)) {
            return false;
        }
        return eventTimestamp.isBefore(latestEventStored);

    }

}
