package io.vanillabp.cockpit.commons.utils;

/**
 * Configures the executor which runs the asynchronous tasks.
 */
public class AsyncProperties {

    private int corePoolSize = 2;

    private int maxPoolSize = 50;

    /**
     * How many tasks may wait. Once more than that are waiting, a new thread is started.
     */
    private int queueCapacity = 5;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
    
}
