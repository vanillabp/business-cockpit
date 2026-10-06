package io.vanillabp.cockpit.gui.api.v1;

public class GuiSseProperties {

    public static final int DEFAULT_MAX_KNOWN_IDS_PER_STREAM = 2_000;

    private int updateInterval = 500;

    private int collectingInterval = 250;

    private int filteringInterval = 1_000;

    private int maxItemsPerUpdate = 100;

    private int maxKnownIdsPerStream = DEFAULT_MAX_KNOWN_IDS_PER_STREAM;

    public int getCollectingInterval() {
        return collectingInterval;
    }

    public void setCollectingInterval(int collectingInterval) {
        this.collectingInterval = collectingInterval;
    }

    /**
     * How often, in milliseconds, the changes collected since the last time are checked against
     * the open streams. Each check asks the database once per stream and kind of entity, and only
     * when something was collected.
     */
    public int getFilteringInterval() {
        return filteringInterval;
    }

    public void setFilteringInterval(int filteringInterval) {
        this.filteringInterval = filteringInterval;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public int getMaxItemsPerUpdate() {
        return maxItemsPerUpdate;
    }

    public void setMaxItemsPerUpdate(int maxItemsPerUpdate) {
        this.maxItemsPerUpdate = maxItemsPerUpdate;
    }

    /**
     * How many ids of user tasks and workflows together a stream remembers as shown in its
     * browser. Above that the stream forgets the older ones and asks the browser to load its lists
     * again.
     */
    public int getMaxKnownIdsPerStream() {
        return maxKnownIdsPerStream;
    }

    public void setMaxKnownIdsPerStream(int maxKnownIdsPerStream) {
        this.maxKnownIdsPerStream = maxKnownIdsPerStream;
    }

}
