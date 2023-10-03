package io.vanillabp.cockpit.config.web.security;

import io.vanillabp.cockpit.commons.utils.UserDetails;
import io.vanillabp.cockpit.commons.utils.UserDetailsProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import reactor.core.publisher.Mono;

public class BasicUserDetailsProvider implements UserDetailsProvider {

    private final MapReactiveUserDetailsService userDetailsService;

    public BasicUserDetailsProvider(
            final MapReactiveUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public UserDetails getUserDetails(
            final Authentication authentication) {

        final var user = userDetailsService
                .findByUsername(getUsername(authentication))
                .block();
        return new BasicUserDetails(user);

    }

    @Override
    public Mono<UserDetails> getUserDetailsAsMono(
            final Authentication authentication) {

        return userDetailsService
                .findByUsername(getUsername(authentication))
                .map(BasicUserDetails::new);

    }

    private String getUsername(
            final Authentication authentication) {

        final var principal = authentication.getPrincipal();
        final String username;
        if (authentication instanceof User) {
            username = ((User) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return username;

    }

}
