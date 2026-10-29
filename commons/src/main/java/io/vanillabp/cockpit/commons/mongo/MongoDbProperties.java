package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;

/**
 * The MongoDB settings of the Business Cockpit, bound from the keys below {@code mongodb}. A key
 * below {@code mongodb} which no field takes stops the start.
 */
@ConfigurationProperties(prefix = "mongodb", ignoreUnknownFields = false)
public class MongoDbProperties {

  /**
   * The kind of database the cockpit talks to. Azure Cosmos DB speaks the MongoDB protocol but lacks
   * some features, so the change streams are built in another way for it.
   */
  public enum Mode {
    /**
     * MongoDB itself, or a database which speaks the full MongoDB protocol.
     */
    MONGODB_4_8,
    /**
     * Azure Cosmos DB for MongoDB. Its change streams report no deletions and always need the
     * full document.
     */
    AZURE_COSMOS_MONGO_4_2
  };

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
   * <p>
   * <a href="https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern">The
   * write concern</a> in the wiki says what each value costs.
   */
  private String writeConcern;

  /**
   * The 'j' of the write concern: whether an acknowledging node has to have the write in its
   * journal before it answers. It is only read together with {@link #writeConcern}. A journal
   * flag on its own is not a write concern, and the cockpit's MongoTemplate drops it.
   * <p>
   * <a href="https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern">The
   * write concern</a> in the wiki says what the journal flag costs.
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

  /**
   * Creates the settings with their defaults: plain MongoDB, change-stream polls of at most one
   * second, no write concern of the cockpit's own.
   */
  public MongoDbProperties() {
  }

  /**
   * A timeout as an ISO-8601 duration, five seconds by default. Nothing in the cockpit reads this
   * value at the moment.
   *
   * @return The timeout
   */
  public String getUseTimeout() {
    return useTimeout;
  }

  /**
   * Sets a timeout as an ISO-8601 duration. Nothing in the cockpit reads this value at the moment.
   *
   * @param useTimeout The timeout
   */
  public void setUseTimeout(
      String useTimeout) {
    this.useTimeout = useTimeout;
  }

  /**
   * The 'w' of the write concern the cockpit writes with. See the field for the values it takes.
   *
   * @return The 'w', or {@code null} for one acknowledging node
   */
  public String getWriteConcern() {
    return writeConcern;
  }

  /**
   * Sets the 'w' of the write concern the cockpit writes with.
   *
   * @param writeConcern The 'w': 'majority', a count of nodes or the name of a tag set
   */
  public void setWriteConcern(
      String writeConcern) {
    this.writeConcern = writeConcern;
  }

  /**
   * The 'j' of the write concern: whether an acknowledging node has to have the write in its
   * journal before it answers. It counts only together with the 'w'.
   *
   * @return The 'j', or {@code null} if it is not set
   */
  public Boolean getWriteConcernJournal() {
    return writeConcernJournal;
  }

  /**
   * Sets the 'j' of the write concern. It counts only together with the 'w'.
   *
   * @param writeConcernJournal The 'j'
   */
  public void setWriteConcernJournal(
      Boolean writeConcernJournal) {
    this.writeConcernJournal = writeConcernJournal;
  }

  /**
   * How long the server may hold an idle change-stream poll before it answers empty. This is also
   * the longest time the shutdown waits per change-stream subscription.
   *
   * @return The time as an ISO-8601 duration
   */
  public String getChangeStreamMaxAwaitTime() {
    return changeStreamMaxAwaitTime;
  }

  /**
   * Sets how long the server may hold an idle change-stream poll before it answers empty.
   *
   * @param changeStreamMaxAwaitTime The time as an ISO-8601 duration
   */
  public void setChangeStreamMaxAwaitTime(
      String changeStreamMaxAwaitTime) {
    this.changeStreamMaxAwaitTime = changeStreamMaxAwaitTime;
  }

  /**
   * Whether the connection to the database uses TLS. Nothing in the cockpit reads this value at the
   * moment; TLS is set in the connection string.
   *
   * @return Whether TLS is used
   */
  public boolean isUseTls() {
    return useTls;
  }

  /**
   * Sets whether the connection to the database uses TLS. Nothing in the cockpit reads this value at
   * the moment.
   *
   * @param useTls Whether TLS is used
   */
  public void setUseTls(
      boolean useTls) {
    this.useTls = useTls;
  }

  /**
   * The thread pool settings for the change streams. Nothing in the cockpit reads them at the
   * moment; the change streams run on the threads of the message listener container.
   *
   * @return The thread pool settings
   */
  public AsyncProperties getChangeStreamExecutor() {
    return changeStreamExecutor;
  }

  /**
   * Sets the thread pool settings for the change streams. Nothing in the cockpit reads them at the
   * moment.
   *
   * @param changeStreamExecutor The thread pool settings
   */
  public void setChangeStreamExecutor(
      AsyncProperties changeStreamExecutor) {
    this.changeStreamExecutor = changeStreamExecutor;
  }

  /**
   * The kind of database the cockpit talks to.
   *
   * @return The mode
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Sets the kind of database the cockpit talks to.
   *
   * @param mode The mode
   */
  public void setMode(
      Mode mode) {
    this.mode = mode;
  }

}
