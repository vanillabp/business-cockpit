package io.vanillabp.cockpit.support.security;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;

/**
 * Answers {@link BcUnauthorizedException} for every REST endpoint of the application with
 * {@code 401 Unauthorized} and the message as plain text. In the cockpit its
 * {@code RestfulExceptionHandler} does the same. A Quarkus workflow module has no such handler, so
 * this module brings one, and Quarkus REST finds it the same way as the mapper of the user task
 * exceptions.
 */
public class BcUnauthorizedExceptionMapper {

  /**
   * Creates the mapper. Quarkus calls this constructor.
   */
  public BcUnauthorizedExceptionMapper() {
  }

  /**
   * Answers a request without a valid login with {@code 401 Unauthorized}.
   *
   * @param exception What the workflow module found
   * @return The answer, with the message as the body
   */
  @ServerExceptionMapper
  public RestResponse<String> unauthorized(
      final BcUnauthorizedException exception) {

    return RestResponse.ResponseBuilder
        .<String>create(BcUnauthorizedException.HTTP_STATUS_CODE)
        .entity(exception.getMessage())
        .type(MediaType.TEXT_PLAIN_TYPE)
        .build();

  }

}
