package io.vanillabp.cockpit.support.rest.restclient;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

/**
 * A stub server's dispatcher which holds every request until the test lets them go. While they are
 * held, each request keeps its connection busy, so the number of requests waiting at the server is
 * the number of connections the client has open at the same time.
 */
public final class HeldRequests extends Dispatcher {

  private final CountDownLatch released = new CountDownLatch(1);

  private final AtomicInteger waiting = new AtomicInteger();

  /** Creates the dispatcher. It holds requests until {@link #release()}. */
  public HeldRequests() {
  }

  @Override
  public MockResponse dispatch(
      final RecordedRequest request) throws InterruptedException {

    waiting.incrementAndGet();
    // a test which fails before it releases the requests must not keep the server's threads forever
    released.await(10, TimeUnit.SECONDS);
    return Stubs.item("1");

  }

  /**
   * How many requests wait at the server once no new one arrived for a while.
   *
   * @param expected the number the test expects, so the wait ends as soon as that many arrived
   * @param quiet    how long no further request must arrive after that
   * @return the number of waiting requests
   */
  public int waitingAfter(
      final int expected,
      final Duration quiet) throws InterruptedException {

    final var giveUpAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while ((waiting.get() < expected) && (System.nanoTime() < giveUpAt)) {
      Thread.sleep(10);
    }
    // more requests than expected may still be on their way, so they get time to arrive
    Thread.sleep(quiet.toMillis());
    return waiting.get();

  }

  /** Lets every held request and every later one get its answer. */
  public void release() {
    released.countDown();
  }

  /** The server closes its dispatcher when it stops, and a stopped server holds nothing back. */
  @Override
  public void close() {
    release();
  }

}
