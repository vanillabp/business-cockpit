package io.vanillabp.cockpit.support.rest.httpservice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

/**
 * Logs each request and its answer with headers and bodies at level INFO, as
 * {@code ClientsConfigurationBase} does with {@code log: true}. Neither Spring Framework nor Spring
 * Boot has such logging for a {@code RestClient}. At DEBUG it names the Java object it writes and
 * the type it reads into, but not the URL, the status or the body of the answer.
 * <p>
 * Reading the answer's body here would leave nothing for the message converters, so the group
 * buffers answers ({@code RestClient.Builder.bufferContent}) when logging is switched on.
 * <p>
 * The values of the headers which carry a login are not written to the log.
 */
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

  private static final Set<String> LOGINS = Set.of(
      HttpHeaders.AUTHORIZATION.toLowerCase(),
      HttpHeaders.PROXY_AUTHORIZATION.toLowerCase(),
      HttpHeaders.COOKIE.toLowerCase(),
      HttpHeaders.SET_COOKIE.toLowerCase());

  private static final String HIDDEN = "<hidden>";

  private final String groupName;

  /**
   * Creates the interceptor of one group.
   *
   * @param groupName the name of the group, written in front of each line
   */
  public LoggingInterceptor(
      final String groupName) {
    this.groupName = groupName;
  }

  @Override
  public ClientHttpResponse intercept(
      final HttpRequest request,
      final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {

    logger.info("[{}] ---> {} {} {}\n{}", groupName, request.getMethod(), request.getURI(),
        withoutLogins(request.getHeaders()), new String(body, StandardCharsets.UTF_8));
    final var response = execution.execute(request, body);
    logger.info("[{}] <--- {} {}\n{}", groupName, response.getStatusCode().value(),
        withoutLogins(response.getHeaders()), StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
    return response;

  }

  private static HttpHeaders withoutLogins(
      final HttpHeaders headers) {

    final var shown = new HttpHeaders();
    headers.forEach((
        name,
        values) -> {
      if (LOGINS.contains(name.toLowerCase())) {
        shown.add(name, HIDDEN);
      } else {
        shown.addAll(name, values);
      }
    });
    return shown;

  }

}
