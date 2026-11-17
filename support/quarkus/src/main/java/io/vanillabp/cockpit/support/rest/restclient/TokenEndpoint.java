package io.vanillabp.cockpit.support.rest.restclient;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The token endpoint of an OAuth server, as Quarkus' REST client calls it. Its base address is the
 * whole {@code oauth.token-uri}. It is built with the timeouts, the TLS settings and the proxy of
 * the clients it fetches tokens for, so a token request goes the same way as the requests it is
 * for.
 * <p>
 * The answer comes as a {@link CompletionStage}, because a token is fetched inside a filter of a
 * request. Waiting there would block the thread which has to read the answer.
 */
public interface TokenEndpoint {

  /**
   * Asks for a token.
   *
   * @param form       the fields of the grant, and client id and secret if they go into the form
   * @param basicLogin client id and secret as an HTTP basic authentication header, or {@code null}
   *                   if they go into the form
   * @return the answer, also if its status is an error
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  CompletionStage<Response> requestToken(
      Form form,
      @HeaderParam(HttpHeaders.AUTHORIZATION) String basicLogin);

}
