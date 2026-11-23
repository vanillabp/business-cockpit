package io.vanillabp.cockpit.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps the ID token of the provider after a login, for the logout at the provider. The cockpit
 * keeps nothing else of the provider: the login ends in the cockpit's own cookie. But the logout
 * has to tell the provider whose session ends, and the ID token does that.
 * <p>
 * The token goes into a cookie of its own, together with the id of the provider's registration.
 * The cookie is {@code HttpOnly}, and its path is {@value #LOGOUT_PATH}, so the browser sends it
 * with the logout only. {@link LogoutAtTheProvider} reads it.
 */
class KeepTheIdTokenForTheLogout extends SavedRequestAwareAuthenticationSuccessHandler {

  /** The name of the cookie. */
  static final String ID_TOKEN_COOKIE = "bc-id-token";

  /** The path of the logout, and so of the cookie. */
  static final String LOGOUT_PATH = "/logout";

  /**
   * Separates the registration id from the token in the value of the cookie. A token has no colon,
   * and neither has the id of a registration in the Spring Boot keys.
   */
  private static final String SEPARATOR = ":";

  /** Creates the handler. */
  KeepTheIdTokenForTheLogout() {
  }

  @Override
  public void onAuthenticationSuccess(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final Authentication authentication) throws ServletException, IOException {

    if ((authentication instanceof OAuth2AuthenticationToken login) && (login
        .getPrincipal() instanceof OidcUser user)) {
      final var cookie = ResponseCookie
          .from(ID_TOKEN_COOKIE,
              login.getAuthorizedClientRegistrationId() + SEPARATOR + user.getIdToken().getTokenValue())
          .httpOnly(true)
          .secure(request.isSecure())
          .sameSite("Lax")
          .path(LOGOUT_PATH)
          .build();
      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    super.onAuthenticationSuccess(request, response, authentication);

  }

  /**
   * What the cookie of a request says, if the request has the cookie.
   *
   * @param request The request of the logout
   * @return The registration and the token
   */
  static Optional<KeptIdToken> keptIn(
      final HttpServletRequest request) {

    return Optional
        .ofNullable(WebUtils.getCookie(request, ID_TOKEN_COOKIE))
        .map(cookie -> cookie.getValue().split(SEPARATOR, 2))
        .filter(parts -> parts.length == 2)
        .map(parts -> new KeptIdToken(parts[0], parts[1]));

  }

  /**
   * Removes the cookie from the browser.
   *
   * @param response The answer to the logout
   */
  static void forget(
      final HttpServletResponse response) {

    response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie
        .from(ID_TOKEN_COOKIE, "")
        .path(LOGOUT_PATH)
        .maxAge(0)
        .build()
        .toString());

  }

  /**
   * The ID token of a login, and the registration of the provider it came from.
   *
   * @param registrationId The id of the registration in the Spring Boot keys
   * @param idToken The ID token
   */
  record KeptIdToken(String registrationId, String idToken) {
  }

}
