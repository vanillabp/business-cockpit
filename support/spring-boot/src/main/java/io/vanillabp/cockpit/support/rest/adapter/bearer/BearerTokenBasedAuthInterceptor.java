package io.vanillabp.cockpit.support.rest.adapter.bearer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import feign.FeignException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import io.vanillabp.cockpit.support.rest.adapter.ClientsConfigurationBase;

/**
 * Sends a bearer token in the &quot;Authorization&quot; header of each request. If the server
 * answers 401, the request is marked as one Feign may retry, so it can be sent again with a new
 * token. Subclasses say how a token is fetched.
 * <p>
 * A token is fetched once and used for every request until shortly before it expires. All threads
 * share it, so the token issuer is asked once per lifetime of a token and not once per request. A
 * token without a known lifetime is used until the server answers 401. See
 * {@code BearerTokenBasedAuthInterceptorTest}.
 */
public abstract class BearerTokenBasedAuthInterceptor extends ClientsConfigurationBase implements RequestInterceptor, ErrorDecoder {

  /**
   * The longest time before its expiry at which a token is replaced. A request sent with a token
   * which is about to expire may reach the server after the expiry.
   */
  private static final Duration RENEWAL_MARGIN = Duration.ofSeconds(30);

  private final AtomicReference<BearerToken> token = new AtomicReference<>();

  /**
   * A token and the moment from which it is no longer used.
   *
   * @param value The token, without the &quot;Bearer&quot; prefix
   * @param renewAt From this moment on a new token is fetched, or {@code null} to use the token
   *        until the server answers 401
   */
  public record BearerToken(
                            String value,
                            Instant renewAt) {

    /**
     * A token which is replaced shortly before it expires. The margin is a tenth of its lifetime,
     * but at most 30 seconds.
     *
     * @param value The token, without the &quot;Bearer&quot; prefix
     * @param fetchedAt When the token was fetched
     * @param lifetime How long the token issuer says the token is valid
     * @return The token
     */
    public static BearerToken expiringAfter(
        final String value,
        final Instant fetchedAt,
        final Duration lifetime) {

      final var tenth = lifetime.dividedBy(10);
      final var margin = tenth.compareTo(RENEWAL_MARGIN) < 0 ? tenth : RENEWAL_MARGIN;
      return new BearerToken(value, fetchedAt.plus(lifetime).minus(margin));

    }

    private boolean isUsableAt(
        final Instant now) {

      return (renewAt == null) || now.isBefore(renewAt);

    }

  }

  /**
   * Creates the interceptor. It fetches a token when a request needs one.
   */
  public BearerTokenBasedAuthInterceptor() {
  }

  /**
   * Fetches a new token from the token issuer.
   *
   * @return The token, without the &quot;Bearer&quot; prefix
   */
  protected abstract String createToken();

  /**
   * Fetches a new token together with the moment it has to be replaced. Override it if the token
   * issuer says how long a token lives. Otherwise the token from {@link #createToken()} is used
   * until the server answers 401.
   *
   * @return The token
   */
  protected BearerToken createBearerToken() {

    return new BearerToken(createToken(), null);

  }

  @Override
  public void apply(
      final RequestTemplate template) {

    // replace existing header which may hold expired values
    template.removeHeader("Authorization");
    template.header("Authorization", List.of("Bearer "
        + getToken()));

  }

  @Override
  public Exception decode(
      final String methodKey,
      final Response response) {

    // if token expired then build a new token
    if (response.status() == 401) {

      forgetTheTokenSentWith(response);

      return new RetryableException(
          response.status(), response.reason(), response.request().httpMethod(),
          // the moment Feign may retry, in epoch millis: now, because all the
          // retry needs is a token which has not expired yet
          System.currentTimeMillis(), response.request());

    }

    return FeignException.errorStatus(methodKey, response);

  }

  /**
   * Forgets the token the server refused, so the retry fetches a new one. Another thread may have
   * fetched a new token since the request was sent. That one is kept.
   */
  private void forgetTheTokenSentWith(
      final Response response) {

    final var current = token.get();
    if (current == null) {
      return;
    }
    final var sent = response.request().headers().get("Authorization");
    final var anotherTokenWasSent = (sent != null) && !sent.isEmpty() && !sent.contains("Bearer "
        + current.value());
    if (!anotherTokenWasSent) {
      token.compareAndSet(current, null);
    }

  }

  private String getToken() {

    final var current = token.get();
    if ((current != null) && current.isUsableAt(Instant.now())) {
      return current.value();
    }

    // one thread fetches, the others wait for its token instead of fetching their own
    synchronized (token) {
      final var fetchedMeanwhile = token.get();
      if ((fetchedMeanwhile != null) && fetchedMeanwhile.isUsableAt(Instant.now())) {
        return fetchedMeanwhile.value();
      }
      final var fresh = createBearerToken();
      token.set(fresh);
      return fresh.value();
    }

  }

}
