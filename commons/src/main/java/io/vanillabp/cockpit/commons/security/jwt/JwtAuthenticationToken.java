package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final Jwt jwt;

    public JwtAuthenticationToken(
            final Jwt jwt,
            final Collection<? extends GrantedAuthority> authorities) {
        
        super(authorities);
        this.jwt = jwt;
        setAuthenticated(true);
        
    }

    @Override
    public Object getPrincipal() {

        return jwt.getSubject();

    }
    
    @Override
    public Object getCredentials() {
        
        return jwt.getTokenValue();
        
    }
    
}