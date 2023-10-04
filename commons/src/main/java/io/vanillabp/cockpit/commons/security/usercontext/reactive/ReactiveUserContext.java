package io.vanillabp.cockpit.commons.security.usercontext.reactive;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class ReactiveUserContext extends UserContext {

    private final ReactiveUserDetailsProvider userDetailsProvider;

    public ReactiveUserContext(
            final ReactiveUserDetailsProvider userDetailsProvider) {

        super(userDetailsProvider);
        this.userDetailsProvider = userDetailsProvider;

    }

    public Mono<String> getUserLoggedInAsMono() throws BcUnauthorizedException {

        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null) {
                        throw new BcUnauthorizedException("No security context");
                    }
                    if (!authentication.isAuthenticated()) {
                        throw new BcUnauthorizedException("User anonymous");
                    }
                    if (authentication instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    return userDetailsProvider
                            .getUserDetailsAsMono(authentication)
                            .map(UserDetails::getId);
                });

    }

    public Mono<UserDetails> getUserLoggedInDetailsAsMono() throws BcUnauthorizedException {

        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null) {
                        throw new BcUnauthorizedException("No security context");
                    }
                    if (!authentication.isAuthenticated()) {
                        throw new BcUnauthorizedException("User anonymous");
                    }
                    if (authentication instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    return userDetailsProvider
                            .getUserDetailsAsMono(authentication);
                });

    }

}
