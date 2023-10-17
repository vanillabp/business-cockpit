package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import org.springframework.security.core.Authentication;

public class JwtUserDetailsProvider implements UserDetailsProvider {

    @Override
    public UserDetails getUserDetails(
            final Authentication authentication) {

        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken) {
            return new JwtUserDetails((JwtAuthenticationToken) authentication);
        }

        throw new RuntimeException("Unsupported authentication class '"
                + authentication.getClass().getName()
                + "', '"
                + JwtAuthenticationToken.class.getName()
                + "' expected!");

    }

}
