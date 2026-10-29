package io.vanillabp.cockpit.commons.rest.adapter;

import java.time.Duration;

/**
 * The settings for sending a failed request again. Feign waits a little longer before each new
 * attempt, starting with {@code period} and going up to {@code maxPeriod}.
 */
public class Retry {

  private boolean enabled = false;
  private int maxAttempts = 5;
  private Duration period = Duration.ofMillis(100);
  private Duration maxPeriod = Duration.ofSeconds(1);

  /**
   * Creates the settings with their defaults: retries off, at most 5 attempts, waiting from 100
   * milliseconds up to 1 second.
   */
  public Retry() {
  }

  /**
   * Tells whether failed requests are sent again.
   *
   * @return Whether retries are on
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether failed requests are sent again.
   *
   * @param enabled Whether retries are on
   */
  public void setEnabled(
      boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * How often a request is sent at most, the first attempt included.
   *
   * @return The number of attempts
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * Sets how often a request is sent at most, the first attempt included.
   *
   * @param maxAttempts The number of attempts
   */
  public void setMaxAttempts(
      int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * How long Feign waits before the first retry.
   *
   * @return The first waiting time
   */
  public Duration getPeriod() {
    return period;
  }

  /**
   * Sets how long Feign waits before the first retry.
   *
   * @param period The first waiting time
   */
  public void setPeriod(
      Duration period) {
    this.period = period;
  }

  /**
   * The longest time Feign waits between two attempts.
   *
   * @return The longest waiting time
   */
  public Duration getMaxPeriod() {
    return maxPeriod;
  }

  /**
   * Sets the longest time Feign waits between two attempts.
   *
   * @param maxPeriod The longest waiting time
   */
  public void setMaxPeriod(
      Duration maxPeriod) {
    this.maxPeriod = maxPeriod;
  }

}
