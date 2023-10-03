package io.vanillabp.cockpit.commons.utils;

import io.vanillabp.cockpit.commons.exceptions.BcForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserContext {
    
    @Autowired
    private UserDetailsProvider userDetailsProvider;
    
    public String getUserLoggedIn() {
        
        final var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        
        if (authentication == null) {
            throw new BcForbiddenException("No security context");
        }
        if (!authentication.isAuthenticated()) {
            throw new BcForbiddenException("User anonymous");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        return userDetailsProvider
                .getUserDetails(authentication)
                .getId();
        
    }

    public UserDetails getUserLoggedInDetails() {
        
        final var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        
        if (authentication == null) {
            throw new BcForbiddenException("No security context");
        }
        if (!authentication.isAuthenticated()) {
            throw new BcForbiddenException("User anonymous");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        return userDetailsProvider
                .getUserDetails(authentication);
        
    }

    public Mono<String> getUserLoggedInAsMono() {
        
        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null) {
                        throw new BcForbiddenException("No security context");
                    }
                    if (!authentication.isAuthenticated()) {
                        throw new BcForbiddenException("User anonymous");
                    }
                    if (authentication instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    return userDetailsProvider
                            .getUserDetailsAsMono(authentication)
                            .map(UserDetails::getId);
                });
        
    }
    
    public Mono<UserDetails> getUserLoggedInDetailsAsMono() {

        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null) {
                        throw new BcForbiddenException("No security context");
                    }
                    if (!authentication.isAuthenticated()) {
                        throw new BcForbiddenException("User anonymous");
                    }
                    if (authentication instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    return userDetailsProvider
                            .getUserDetailsAsMono(authentication);
                });

    }
    
}