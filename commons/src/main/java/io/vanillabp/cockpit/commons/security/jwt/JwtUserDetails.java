package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class JwtUserDetails implements UserDetails {

    public static final String USER_AUTHORITY_PREFIX = "USER_";

    private final JwtAuthenticationToken authenticationToken;

    public JwtUserDetails(
            final JwtAuthenticationToken authenticationToken) {

        this.authenticationToken = authenticationToken;
        
    }

    private Jwt getJwt() {
        return (Jwt) authenticationToken.getDetails();
    }

    @Override
    public String getId() {

        return getJwt().getSubject();
        
    }
    
    @Override
    public String getFirstName() {

        final var name = getJwt().getClaimAsString("name");
        final var givenName = getJwt().getClaimAsString("given_name");
        if ((givenName == null)
                && (name == null)) {
            return null;
        }
        if (givenName == null) {
            final var posOfSeparator = name.indexOf(' ');
            if (posOfSeparator != -1) {
                return name.substring(0, posOfSeparator);
            }
        }
        return givenName;
        
    }
    
    @Override
    public String getLastName() {

        final var name = getJwt().getClaimAsString("name");
        final var familyName = getJwt().getClaimAsString("family_name");
        if ((familyName == null)
                && (name == null)) {
            return getJwt().getSubject();
        }
        if (familyName == null) {
            final var posOfSeparator = name.indexOf(' ');
            if (posOfSeparator != -1) {
                return name.substring(posOfSeparator + 1);
            }
        }
        return familyName;
        
    }
    
    @Override
    public String getEmail() {
        
        return getJwt().getSubject();
        
    }
    
    @Override
    public List<String> getAuthorities() {

        return authenticationToken
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

    }
    
    @Override
    public boolean isActive() {
        
        return true;
        
    }
    
    @Override
    public Boolean isFemale() {

        final var gender = getJwt().getClaimAsString("gender");
        if ("female".equals(gender)) {
            return Boolean.TRUE;
        } else if ("male".equals(gender)) {
            return Boolean.FALSE;
        }
        return null;
        
    }
    
}
