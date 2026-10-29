package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.Serial;
import java.util.Collection;

/**
 * A login which Spring Security knows from a JSON Web Token (JWT). The token is kept as the
 * details of the authentication. The principal is the subject of the token, the user id.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an authentication which counts as authenticated, because the token was checked before.
     *
     * @param jwt         The checked token
     * @param authorities The authorities of the user
     */
    public JwtAuthenticationToken(
            final Jwt jwt,
            final Collection<? extends GrantedAuthority> authorities) {
        
        super(authorities);
        super.setDetails(jwt);
        setAuthenticated(true);
        
    }

    @Override
    public Object getPrincipal() {

        return getJwt().getSubject();

    }
    
    @Override
    public Object getCredentials() {
        
        return getJwt().getTokenValue();
        
    }

    /**
     * The token this login comes from.
     *
     * @return The token
     */
    public Jwt getJwt() {

        return (Jwt) getDetails();

    }

}