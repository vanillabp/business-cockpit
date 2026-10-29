package io.vanillabp.cockpit.gui.api.v1;

/**
 * The settings of the update streams which tell the browsers about changes, found under
 * {@code business-cockpit.gui-sse}. All intervals are in milliseconds.
 */
public class GuiSseProperties {

  /** The default for {@link #getMaxKnownIdsPerStream()}, used as well when it is set below one. */
  public static final int DEFAULT_MAX_KNOWN_IDS_PER_STREAM = 2_000;

  /** Creates the settings with their defaults. Spring binds the configured values afterwards. */
  public GuiSseProperties() {
  }

  private int updateInterval = 500;

  private int collectingInterval = 250;

  private int filteringInterval = 250;

  private int maxItemsPerUpdate = 100;

  private int maxKnownIdsPerStream = DEFAULT_MAX_KNOWN_IDS_PER_STREAM;

  /**
   * How often, in milliseconds, the changes matched to the streams are written to the browsers.
   * The scheduler reads the key {@code business-cockpit.gui-sse.collecting-interval} itself, so
   * this value is there for binding and reading only.
   *
   * @return The interval
   */
  public int getCollectingInterval() {
    return collectingInterval;
  }

  /**
   * Sets how often the matched changes are written to the browsers.
   *
   * @param collectingInterval The interval in milliseconds
   */
  public void setCollectingInterval(
      int collectingInterval) {
    this.collectingInterval = collectingInterval;
  }

  /**
   * How often, in milliseconds, the changes collected since the last time are checked against
   * the open streams. Each check asks the database once per kind of entity, whatever the number
   * of open streams, and only when something was collected.
   *
   * @return The interval
   */
  public int getFilteringInterval() {
    return filteringInterval;
  }

  /**
   * Sets how often the collected changes are checked against the open streams.
   *
   * @param filteringInterval The interval in milliseconds
   */
  public void setFilteringInterval(
      int filteringInterval) {
    this.filteringInterval = filteringInterval;
  }

  /**
   * The least time, in milliseconds, between two updates written to the same browser. Changes
   * which come in faster wait for the next update.
   *
   * @return The interval
   */
  public int getUpdateInterval() {
    return updateInterval;
  }

  /**
   * Sets the least time between two updates written to the same browser.
   *
   * @param updateInterval The interval in milliseconds
   */
  public void setUpdateInterval(
      int updateInterval) {
    this.updateInterval = updateInterval;
  }

  /**
   * How many changes one update to a browser carries at most. The rest waits for the next
   * update.
   *
   * @return The number of changes
   */
  public int getMaxItemsPerUpdate() {
    return maxItemsPerUpdate;
  }

  /**
   * Sets how many changes one update to a browser carries at most.
   *
   * @param maxItemsPerUpdate The number of changes
   */
  public void setMaxItemsPerUpdate(
      int maxItemsPerUpdate) {
    this.maxItemsPerUpdate = maxItemsPerUpdate;
  }

  /**
   * How many ids of user tasks and workflows together a stream remembers as shown in its
   * browser. Above that the stream forgets the older ones and asks the browser to load its lists
   * again.
   *
   * @return The number of ids
   */
  public int getMaxKnownIdsPerStream() {
    return maxKnownIdsPerStream;
  }

  /**
   * Sets how many ids a stream remembers as shown in its browser.
   *
   * @param maxKnownIdsPerStream The number of ids, at least one
   */
  public void setMaxKnownIdsPerStream(
      int maxKnownIdsPerStream) {
    this.maxKnownIdsPerStream = maxKnownIdsPerStream;
  }

}
