package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserDetailsProvider;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public class ReactiveJwtUserDetailsProvider extends JwtUserDetailsProvider implements ReactiveUserDetailsProvider {

    @Override
    public Mono<UserDetails> getUserDetailsAsMono(Authentication authentication) {

        try {

            final var details = getUserDetails(authentication);
            if (details == null) {
                return Mono.empty();
            }
            return Mono.just(details);

        } catch (Exception e) {

            return Mono.error(e);

        }

    }

}
