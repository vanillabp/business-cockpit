package io.vanillabp.cockpit.security;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import io.vanillabp.cockpit.security.KeepTheIdTokenForTheLogout.KeptIdToken;
import io.vanillabp.cockpit.support.security.jwt.JwtProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ends the login in the cockpit, and at the OpenID Connect provider as well. The provider keeps a
 * session of its own. Without its end, the next request after the logout goes to the provider, the
 * provider sees its session and sends the user back logged in, without a question. To the user the
 * logout would then do nothing that a reload does not undo.
 * <p>
 * So the browser goes on to the {@code end_session_endpoint} of the provider, the RP-initiated
 * logout of OpenID Connect, with three parameters: {@code id_token_hint}, the ID token which
 * {@link KeepTheIdTokenForTheLogout} kept, so the provider asks no question; {@code client_id}; and
 * {@code post_logout_redirect_uri}, the start page of the cockpit. The provider sends the browser
 * back there, and the start page asks for a new login. The provider has to know the start page as a
 * post-logout redirect URI of the client.
 * <p>
 * Spring reads the {@code end_session_endpoint} from the discovery document of the provider, so it
 * knows it only when the provider is configured with {@code issuer-uri}. Without it the logout ends
 * the login in the cockpit only and goes to the start page, as it did before. Spring Security's
 * {@code OidcClientInitiatedLogoutSuccessHandler} cannot be used: it takes the ID token from the
 * login of the request, and the login of the cockpit is its own token, not the login of the
 * provider.
 */
class LogoutAtTheProvider implements LogoutSuccessHandler {

  private final JwtProperties jwtProperties;

  private final ClientRegistrationRepository clientRegistrations;

  /**
   * Creates the handler.
   *
   * @param jwtProperties The settings of the cockpit's cookie
   * @param clientRegistrations The providers of the Spring Boot keys
   */
  LogoutAtTheProvider(
      final JwtProperties jwtProperties,
      final ClientRegistrationRepository clientRegistrations) {

    this.jwtProperties = jwtProperties;
    this.clientRegistrations = clientRegistrations;

  }

  @Override
  public void onLogoutSuccess(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final Authentication authentication) throws IOException {

    JwtSecurityContextRepository.clearCookie(jwtProperties, response);
    final var kept = KeepTheIdTokenForTheLogout.keptIn(request);
    KeepTheIdTokenForTheLogout.forget(response);

    final var startPage = ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString();
    final var provider = kept
        .map(KeptIdToken::registrationId)
        .map(clientRegistrations::findByRegistrationId)
        .or(this::theOnlyProvider);
    final var endOfSession = provider
        .map(registration -> registration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint"))
        .map(Object::toString);
    if (endOfSession.isEmpty()) {
      response.sendRedirect(startPage);
      return;
    }

    response.sendRedirect(UriComponentsBuilder
        .fromUriString(endOfSession.get())
        // without the hint, for a login from before this handler, the provider asks the user
        .queryParamIfPresent("id_token_hint", kept.map(KeptIdToken::idToken))
        .queryParam("client_id", provider.get().getClientId())
        .queryParam("post_logout_redirect_uri", startPage)
        .encode()
        .toUriString());

  }

  /**
   * The provider of a logout without the cookie of the ID token, for a login from before this
   * handler. It is known only if there is just one.
   */
  private Optional<ClientRegistration> theOnlyProvider() {

    if (!(clientRegistrations instanceof Iterable<?> all)) {
      return Optional.empty();
    }
    final var registrations = StreamSupport
        .stream(all.spliterator(), false)
        .filter(ClientRegistration.class::isInstance)
        .map(ClientRegistration.class::cast)
        .toList();
    return registrations.size() == 1
        ? Optional.of(registrations.getFirst())
        : Optional.empty();

  }

}
