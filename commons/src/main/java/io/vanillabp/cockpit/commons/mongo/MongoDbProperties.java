package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;

@ConfigurationProperties(prefix = "mongodb", ignoreUnknownFields = false)
public class MongoDbProperties {
    
    public enum Mode { MONGODB_4_8, AZURE_COSMOS_MONGO_4_2 };

    private String useTimeout = "PT5S";

    /**
     * The 'w' of the write concern the Business Cockpit writes with: how many nodes of the replica
     * set have to have a write before the cockpit is told it is stored. Write 'majority' to be safe
     * against a failover, a number to name a count of nodes, or the name of a tag set defined in
     * the replica set configuration.
     * <p>
     * Nothing set means the cockpit writes with one acknowledging node, and it says so on every
     * start. It is set on the MongoTemplate rather than taken from the connection string, because
     * a MongoTemplate which checks write results ignores what the connection brings along.
     */
    private String writeConcern;

    /**
     * The 'j' of the write concern: whether an acknowledging node has to have the write in its
     * journal before it answers. It is only read together with {@link #writeConcern}. A journal
     * flag on its own is not a write concern, and the cockpit's MongoTemplate drops it.
     */
    private Boolean writeConcernJournal;

    /**
     * How long the MongoDB server may hold an idle change-stream poll before it answers empty
     * (maxAwaitTime). Stopping the application waits for the poll in flight, because the blocking
     * driver cannot interrupt it. So this value is also the longest the shutdown waits per
     * change-stream subscription. Raise it to poll less often, on Azure Cosmos DB for example,
     * and pay for it with a slower shutdown.
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

    public String getWriteConcern() {
        return writeConcern;
    }

    public void setWriteConcern(String writeConcern) {
        this.writeConcern = writeConcern;
    }

    public Boolean getWriteConcernJournal() {
        return writeConcernJournal;
    }

    public void setWriteConcernJournal(Boolean writeConcernJournal) {
        this.writeConcernJournal = writeConcernJournal;
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
