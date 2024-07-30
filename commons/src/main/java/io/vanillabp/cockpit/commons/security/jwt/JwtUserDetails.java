package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtUserDetails implements UserDetails {

    private static final Pattern PATTERN_NAME = Pattern.compile("^(\\S+)\\s+(.*)$");
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
    public String getDisplay() {

        final var name = getJwt().getClaimAsString("name");
        final var givenName = getJwt().getClaimAsString("given_name");
        final var familyName = getJwt().getClaimAsString("family_name");
        if (familyName != null) {
            if (givenName == null) {
                return familyName;
            }
            return familyName + ", " + givenName;
        }
        return name;
        
    }
    
    @Override
    public String getDisplayShort() {

        final var name = getJwt().getClaimAsString("name");
        final var givenName = getJwt().getClaimAsString("given_name");
        final var familyName = getJwt().getClaimAsString("family_name");
        if (familyName != null) {
            if (givenName == null) {
                return familyName;
            }
            return familyName + ", " + givenName;
        }
        final var posOfSeparator = name.indexOf(' ');
        if (posOfSeparator == -1) {
            return name;
        }
        final var matcher = PATTERN_NAME.matcher(name);
        if (!matcher.matches()) {
            return name;
        }
        return matcher.group(1) + ", " + matcher.group(2).trim();
        
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
    
}
