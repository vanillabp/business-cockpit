package io.vanillabp.cockpit.gui.api.v1;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;

/**
 * Answers the GUI's questions about the application and opens the update stream of a browser tab.
 * Who is signed in is part of the official GUI API and answered by {@link CurrentUserApiController}.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

  /** Creates the controller. Spring fills the fields. */
  public LoginApiController() {
  }

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private UserContext userContext;

  @Autowired
  private UpdateStreams updateStreams;

  /**
   * Opens the update stream of a browser tab. The stream ends when the sign-in expires, and the
   * browser then opens a new one.
   *
   * @return The stream, kept open as the response
   * @throws Exception When no user is signed in yet, as {@link BcUnauthorizedException}
   */
  @RequestMapping(
      method = RequestMethod.GET,
      value = "/updates",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter updatesSubscription() throws Exception {

    final var user = userContext.getUserLoggedInDetails();
    // a request signed in by basic authentication alone has no user yet. Its response carries
    // the cookie, so the browser's next attempt to connect is the one which succeeds
    if (user == null) {
      throw new BcUnauthorizedException("No user signed in for the update stream");
    }

    return updateStreams.subscribe(
        user,
        whenTheSignInExpires(SecurityContextHolder.getContext().getAuthentication()));

  }

  /**
   * When the token of this sign-in expires, if it says so. The cockpit's own token carries a
   * {@link Jwt} as its details. A token of Spring Security's OAuth support carries it as its
   * credentials or its principal. A sign-in which names no end gets a stream which lasts as long
   * as the tab.
   */
  static Optional<Instant> whenTheSignInExpires(
      final Authentication authentication) {

    if (authentication == null) {
      return Optional.empty();
    }
    return Stream
        .of(authentication.getDetails(), authentication.getCredentials(),
            authentication.getPrincipal())
        .filter(AbstractOAuth2Token.class::isInstance)
        .map(AbstractOAuth2Token.class::cast)
        .map(AbstractOAuth2Token::getExpiresAt)
        .filter(Objects::nonNull)
        .findFirst();

  }

  @Override
  public ResponseEntity<AppInformation> appInformation() {

    return ResponseEntity.ok(
        new AppInformation()
            .titleLong(properties.getTitleLong())
            .titleShort(properties.getTitleShort())
            .version(properties.getApplicationVersion())
            .buildTimestamp(properties.getBuildTimestamp())
            .additionalProperties(properties.getAdditionalProperties()));
  }

}
