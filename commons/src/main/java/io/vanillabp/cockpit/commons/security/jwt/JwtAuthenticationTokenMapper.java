package io.vanillabp.cockpit.commons.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationTokenMapper extends JwtMapper<JwtAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationTokenMapper.class);

    public JwtAuthenticationTokenMapper(
            final JwtProperties properties) {

        super(properties);

    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected JwtAuthenticationToken buildAuthenticationToken(
            final Jwt jwt,
            final Collection<GrantedAuthority> bcAuthorities) {

        final var authorities = jwt.getClaimAsStringList(AUTHORITIES_CLAIM);
        if (authorities != null) {
            authorities
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(bcAuthorities::add);
        }

        return new JwtAuthenticationToken(jwt, bcAuthorities);

    }

    @Override
    protected void applyJwtClaimsSet(
            final JwtClaimsSet.Builder claimsSetBuilder,
            final SecurityContext context) {

        final var user = (User) context.getAuthentication().getPrincipal();

        claimsSetBuilder
                .issuer("bc")
                .subject(user.getUsername())
                .audience(List.of("bc"));

        final var authorities = Optional
                .ofNullable(context
                        .getAuthentication()
                        .getAuthorities())
                .orElse(List.of())
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        claimsSetBuilder.claim(AUTHORITIES_CLAIM, authorities);

    }

}
