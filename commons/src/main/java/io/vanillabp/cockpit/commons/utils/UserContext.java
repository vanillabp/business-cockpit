package io.vanillabp.cockpit.commons.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.vanillabp.cockpit.commons.exceptions.BcForbiddenException;
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
                .getUserDetails(authentication.getPrincipal())
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
                .getUserDetails(authentication.getPrincipal());
        
    }

    public Mono<String> getUserLoggedInAsMono() {
        
        return ReactiveSecurityContextHolder
                .getContext()
                .map(c -> c.getAuthentication())
                .map(authentication -> {
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
                            .getUserDetails(authentication.getPrincipal())
                            .getId();
                });
        
    }
    
    public Mono<UserDetails> getUserLoggedInDetailsAsMono() {

        return ReactiveSecurityContextHolder
                .getContext()
                .map(c -> c.getAuthentication())
                .map(authentication -> {
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
                            .getUserDetails(authentication.getPrincipal());
                });
        
    }
    
}