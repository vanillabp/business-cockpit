package io.vanillabp.cockpit.commons.security.usercontext;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContext {

    private final UserDetailsProvider userDetailsProvider;

    public UserContext(
            final UserDetailsProvider userDetailsProvider) {

        this.userDetailsProvider = userDetailsProvider;

    }

    public String getUserLoggedIn() throws BcUnauthorizedException {
        
        final var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        
        if (authentication == null) {
            throw new BcUnauthorizedException("No security context");
        }
        if (!authentication.isAuthenticated()) {
            throw new BcUnauthorizedException("User anonymous");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        final var userDetails = userDetailsProvider
                .getUserDetails(authentication);
        if (userDetails != null) {
            return userDetails.getId();
        }
        return null;

    }

    public UserDetails getUserLoggedInDetails() throws BcUnauthorizedException {
        
        final var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        
        if (authentication == null) {
            throw new BcUnauthorizedException("No security context");
        }
        if (!authentication.isAuthenticated()) {
            throw new BcUnauthorizedException("User anonymous");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        return userDetailsProvider
                .getUserDetails(authentication);
        
    }
    
}