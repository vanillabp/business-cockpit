package io.vanillabp.cockpit.commons.rest.adapter.bearer;

import java.util.List;

import feign.FeignException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import io.vanillabp.cockpit.commons.rest.adapter.ClientsConfigurationBase;

/**
 * Sends a bearer token in the &quot;Authorization&quot; header of each request. If the server
 * answers 401, the request is marked as one Feign may retry, so it can be sent again with a new
 * token. Subclasses say how a token is fetched.
 */
public abstract class BearerTokenBasedAuthInterceptor extends ClientsConfigurationBase implements RequestInterceptor, ErrorDecoder {

  private ThreadLocal<String> tokens = new ThreadLocal<>();

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

      tokens.remove();

      return new RetryableException(
          response.status(), response.reason(), response.request().httpMethod(),
          // the moment Feign may retry, in epoch millis: now, because all the
          // retry needs is a token which has not expired yet
          System.currentTimeMillis(), response.request());

    }

    return FeignException.errorStatus(methodKey, response);

  }

  private String getToken() {

    final var currentToken = tokens.get();
    if (currentToken != null) {
      return currentToken;
    }

    // if there was no token or refreshing failed then create a new token
    return createToken();

  }

}
