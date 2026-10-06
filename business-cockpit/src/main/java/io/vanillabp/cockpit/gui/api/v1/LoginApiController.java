package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
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

@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private UserContext userContext;

    @Autowired
    private UpdateStreams updateStreams;

    @Autowired
    private PersonAndGroupApiMapper personAndGroupMapper;

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

    @Override
    public ResponseEntity<User> currentUser(
            final String xRefreshToken) {

        final var user = userContext.getUserLoggedInDetails();
        // the request which signs in by basic authentication is authenticated, but the response
        // to exactly that request is what carries the JWT cookie. The cockpit reads its user
        // details from that cookie, so the sign-in request has no user to report yet and the
        // client asks again with the cookie. The answer has no body, the way it had while the
        // application was reactive and the empty publisher ended up as an empty 200.
        if (user == null) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok(Optional
                .ofNullable(personAndGroupMapper.toApiPerson(user.getId()))
                .map(person -> new User()
                        .id(person.getId())
                        .email(person.getEmail())
                        .avatar(person.getAvatar())
                        .display(person.getDisplay())
                        .displayShort(person.getDisplayShort())
                        .details(person.getDetails()))
                .orElse(new User()
                        .id(user.getId())
                        .display(user.getDisplay())
                        .displayShort(user.getDisplayShort())
                        .email(user.getEmail()))
                .groups(user
                        .getAuthorities()
                        .stream()
                        .map(personAndGroupMapper::authorityToApiGroup)
                        .toList()));

    }

}
