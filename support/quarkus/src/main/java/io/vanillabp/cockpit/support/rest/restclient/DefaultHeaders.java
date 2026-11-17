package io.vanillabp.cockpit.support.rest.restclient;

import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Adds the headers of the keys {@code default-header.<name>} to each request which does not set
 * them itself, like Spring Boot's key of the same name.
 */
final class DefaultHeaders implements ClientRequestFilter {

  private final Map<String, String> headers;

  DefaultHeaders(
      final Map<String, String> headers) {
    this.headers = Map.copyOf(headers);
  }

  @Override
  public void filter(
      final ClientRequestContext request) {

    headers.forEach((
        name,
        value) -> {
      if (!request.getHeaders().containsKey(name)) {
        request.getHeaders().putSingle(name, value);
      }
    });

  }

}
