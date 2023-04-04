package io.vanillabp.cockpit.commons.utils;

/**
 * Used to configure async task executor.
 */
public class AsyncProperties {

    /**
     * Thread pool default core pool size.
     */
    private int corePoolSize = 2;

    /**
     * Thread pool default maximum pool size.
     */
    private int maxPoolSize = 50;

    /**
     * Default queue capacity: If more tasks waiting then
     * the configured queue capacity then a new thread is spawn. 
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
