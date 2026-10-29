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

  /**
   * Creates the settings with their defaults: 2 threads at least, 50 at most, and 5 waiting tasks
   * before a new thread starts.
   */
  public AsyncProperties() {
  }

  /**
   * How many threads the executor keeps, even when they have nothing to do.
   *
   * @return The number of threads
   */
  public int getCorePoolSize() {
    return corePoolSize;
  }

  /**
   * Sets how many threads the executor keeps, even when they have nothing to do.
   *
   * @param corePoolSize The number of threads
   */
  public void setCorePoolSize(
      int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  /**
   * How many threads the executor starts at most.
   *
   * @return The number of threads
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * Sets how many threads the executor starts at most.
   *
   * @param maxPoolSize The number of threads
   */
  public void setMaxPoolSize(
      int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  /**
   * How many tasks may wait. Once more than that are waiting, a new thread is started.
   *
   * @return The number of tasks
   */
  public int getQueueCapacity() {
    return queueCapacity;
  }

  /**
   * Sets how many tasks may wait. Once more than that are waiting, a new thread is started.
   *
   * @param queueCapacity The number of tasks
   */
  public void setQueueCapacity(
      int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

}
