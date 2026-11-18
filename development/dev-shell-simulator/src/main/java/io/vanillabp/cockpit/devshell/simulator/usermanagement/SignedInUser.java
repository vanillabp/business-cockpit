package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import java.util.Optional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tells which user the dev shell switched to. The id travels in the login cookie of the request,
 * the same cookie the cockpit sets. The generated controller interfaces pass no request, so the
 * request a controller answers comes from Spring's request context.
 */
public class SignedInUser {

  private final JwtServerSecurityContextRepository securityContextRepository;

  /**
   * Creates the helper.
   *
   * @param securityContextRepository Reads the login cookie
   */
  public SignedInUser(
      final JwtServerSecurityContextRepository securityContextRepository) {

    this.securityContextRepository = securityContextRepository;

  }

  /**
   * The id of the user signed in with the request being answered.
   *
   * @return The id, or nothing if the request carries no login
   */
  public Optional<String> id() {

    return id(currentRequest());

  }

  /**
   * The id of the user signed in with a request.
   *
   * @param request The request, which may carry the login cookie
   * @return The id, or nothing if the request carries no login
   */
  public Optional<String> id(
      final HttpServletRequest request) {

    return Optional
        .ofNullable(securityContextRepository
            .loadDeferredContext(request)
            .get())
        .map(context -> context.getAuthentication())
        .map(authentication -> authentication.getName());

  }

  /**
   * The request being answered.
   *
   * @return The request
   */
  static HttpServletRequest currentRequest() {

    return currentAttributes().getRequest();

  }

  /**
   * The response to the request being answered.
   *
   * @return The response
   */
  static HttpServletResponse currentResponse() {

    return currentAttributes().getResponse();

  }

  private static ServletRequestAttributes currentAttributes() {

    return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

  }

}
