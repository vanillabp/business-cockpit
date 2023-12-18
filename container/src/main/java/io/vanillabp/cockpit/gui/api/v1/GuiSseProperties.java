package io.vanillabp.cockpit.gui.api.v1;

public class GuiSseProperties {

    private int updateInterval = 500;

    private int collectingInterval = 250;

    private int maxItemsPerUpdate = 100;

    public int getCollectingInterval() {
        return collectingInterval;
    }

    public void setCollectingInterval(int collectingInterval) {
        this.collectingInterval = collectingInterval;
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

}
