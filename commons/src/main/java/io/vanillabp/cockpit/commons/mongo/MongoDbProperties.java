package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;

@ConfigurationProperties(prefix = "mongodb", ignoreUnknownFields = false)
public class MongoDbProperties {
    
    public enum Mode { MONGODB_4_8, AZURE_COSMOS_MONGO_4_2 };

    private String useTimeout = "PT5S";

    /**
     * How long the MongoDB server may hold an idle change-stream poll before answering empty
     * (maxAwaitTime). Stopping the application waits for the poll in flight, because the blocking
     * driver cannot interrupt it, so this value is also the upper bound the shutdown waits per
     * change-stream subscription. Raise it to reduce polling cost (for example on Azure Cosmos DB)
     * at the price of a slower shutdown.
     */
    private String changeStreamMaxAwaitTime = "PT1S";

    private boolean useTls = false;
    
    private Mode mode = Mode.MONGODB_4_8;
    
    private AsyncProperties changeStreamExecutor = new AsyncProperties();

    public String getUseTimeout() {
        return useTimeout;
    }

    public void setUseTimeout(String useTimeout) {
        this.useTimeout = useTimeout;
    }

    public String getChangeStreamMaxAwaitTime() {
        return changeStreamMaxAwaitTime;
    }

    public void setChangeStreamMaxAwaitTime(String changeStreamMaxAwaitTime) {
        this.changeStreamMaxAwaitTime = changeStreamMaxAwaitTime;
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
