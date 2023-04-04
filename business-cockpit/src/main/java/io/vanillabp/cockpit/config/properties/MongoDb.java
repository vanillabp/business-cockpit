package io.vanillabp.cockpit.config.properties;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;

public class MongoDb {

    private String useTimeout = "PT5S";

    private boolean useTls = false;
    
    private AsyncProperties changeStreamExecutor = new AsyncProperties();

    public String getUseTimeout() {
        return useTimeout;
    }

    public void setUseTimeout(String useTimeout) {
        this.useTimeout = useTimeout;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public AsyncProperties getChangeStreamExecutor() {
        return changeStreamExecutor;
    }

    public void setChangeStreamExecutor(AsyncProperties changeStreamExecutor) {
        this.changeStreamExecutor = changeStreamExecutor;
    }
    
}
