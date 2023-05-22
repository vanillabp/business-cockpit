package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;

@ConfigurationProperties(prefix = "mongodb", ignoreUnknownFields = false)
public class MongoDbProperties {
    
    public enum Mode { MONGODB_4_8, AZURE_COSMOS_MONGO_4_2 };

    private String useTimeout = "PT5S";

    private boolean useTls = false;
    
    private Mode mode = Mode.MONGODB_4_8;
    
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
    
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
}
