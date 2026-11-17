package io.vanillabp.cockpit.support.rest.httpservice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Sends the login for the proxy with every request, before the proxy asks for it. The JDK's
 * HttpClient sends the header to the proxy only: with the request itself for an http address, and
 * with the {@code CONNECT} request of the tunnel for an https address.
 * <p>
 * The JDK's {@code Authenticator} would answer the proxy's {@code 407} as well, but a client with
 * an {@code Authenticator} drops every {@code Authorization} header the application sets. That
 * would break OAuth and basic authentication through a proxy.
 */
final class ProxyLogin implements ClientHttpRequestInterceptor {

  private final String credentials;

  ProxyLogin(
      final String username,
      final String password) {

    this.credentials = "Basic "
        + Base64.getEncoder().encodeToString((username
            + ":"
            + password).getBytes(StandardCharsets.UTF_8));

  }

  @Override
  public ClientHttpResponse intercept(
      final HttpRequest request,
      final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {

    request.getHeaders().set(HttpHeaders.PROXY_AUTHORIZATION, credentials);
    return execution.execute(request, body);

  }

}
