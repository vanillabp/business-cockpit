package io.vanillabp.cockpit.support.rest.httpservice;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Sends a request again when it got no answer: a refused connection, a read timeout. This is what
 * the REST client of version 1 retries, which took it from Feign's {@code Retryer}. An answer
 * with an error status is not retried, because the server did see the request.
 * <p>
 * The one exception is a {@code 401} of a client with OAuth: the OAuth interceptor further down the
 * chain has dropped the refused token by then, so the next attempt fetches a new one.
 * <p>
 * The waiting uses Spring Framework 7's own {@link RetryTemplate}, so no Spring Retry is needed. The
 * annotation {@code @Retryable} of Spring Framework does not fit here: it sits on methods of a bean,
 * and the methods of a generated interface carry no annotation of ours.
 * <p>
 * It works as an interceptor, because Spring Framework 7 builds the interceptor chain as nested
 * functions: calling {@code execution.execute} a second time runs the rest of the chain again, OAuth
 * included.
 */
public class RetryInterceptor implements ClientHttpRequestInterceptor {

  private final RetryTemplate retryTemplate;

  private final int maxAttempts;

  private final boolean retryUnauthorized;

  /**
   * Creates the interceptor with the waiting of Feign's {@code Retryer.Default}.
   *
   * @param maxAttempts       all attempts, the first one included
   * @param period            the pause before the first retry
   * @param maxPeriod         the longest pause between two attempts
   * @param retryUnauthorized whether a {@code 401} is retried, for a group with OAuth
   */
  public RetryInterceptor(
      final int maxAttempts,
      final Duration period,
      final Duration maxPeriod,
      final boolean retryUnauthorized) {

    this.maxAttempts = maxAttempts;
    this.retryUnauthorized = retryUnauthorized;
    // Feign's Retryer.Default waits 'period', then 1.5 times longer each time, at most 'maxPeriod'
    this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
        .maxRetries(maxAttempts - 1)
        .delay(period)
        .multiplier(1.5)
        .maxDelay(maxPeriod)
        .includes(IOException.class)
        .build());

  }

  @Override
  public ClientHttpResponse intercept(
      final HttpRequest request,
      final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {

    final var attempt = new AtomicInteger();
    try {
      return retryTemplate.execute(() -> {
        final var response = execution.execute(request, body);
        final var isLastAttempt = attempt.incrementAndGet() >= maxAttempts;
        if (retryUnauthorized && !isLastAttempt && response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
          response.close();
          throw new TokenRefused();
        }
        return response;
      });
    } catch (RetryException e) {
      // the caller should see what the last attempt failed with, not the retry machinery
      final var last = e.getLastException();
      if (last instanceof IOException ioException) {
        throw ioException;
      }
      if (last instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IOException(last);
    }

  }

  /** An IOException, so the retry policy's 'includes' takes it without a second rule. */
  private static class TokenRefused extends IOException {

    TokenRefused() {
      super("The server refused the OAuth token (401), the next attempt fetches a new one");
    }

  }

}
