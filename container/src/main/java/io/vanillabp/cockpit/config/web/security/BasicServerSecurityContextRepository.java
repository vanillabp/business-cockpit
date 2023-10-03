package io.vanillabp.cockpit.config.web.security;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.commons.security.jwt.JwtServerSecurityContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BasicServerSecurityContextRepository extends JwtServerSecurityContextRepository {

    private static final Logger logger = LoggerFactory.getLogger(BasicServerSecurityContextRepository.class);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public BasicServerSecurityContextRepository(
            final JwtProperties properties) {

        super(properties);

    }

    protected JwtEncoderParameters getJwtEncoderParameters(
            final SecurityContext context,
            final Instant expiresAt) {

        final var user = (User) context.getAuthentication().getPrincipal();
        final var authorities = user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        final var claims = JwtClaimsSet
                .builder()
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .issuer("bc")
                .subject(user.getUsername())
                .audience(List.of("bc"))
                .claim("authorities", authorities)
                .build();

        return  JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims);

    }

}
