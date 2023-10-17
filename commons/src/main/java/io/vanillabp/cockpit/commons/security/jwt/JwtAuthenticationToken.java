package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.Serial;
import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    public JwtAuthenticationToken(
            final Jwt jwt,
            final Collection<? extends GrantedAuthority> authorities) {
        
        super(authorities);
        super.setDetails(jwt);
        setAuthenticated(true);
        
    }

    @Override
    public Object getPrincipal() {

        return ((Jwt) getDetails()).getSubject();

    }
    
    @Override
    public Object getCredentials() {
        
        return ((Jwt) getDetails()).getTokenValue();
        
    }

}