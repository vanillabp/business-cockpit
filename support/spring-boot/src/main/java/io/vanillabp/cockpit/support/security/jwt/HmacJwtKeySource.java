package io.vanillabp.cockpit.support.security.jwt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;


/**
 * Signs and checks login tokens with the shared secret of the settings
 * ({@code hmacSHA256-base64}), using HMAC with SHA-256.
 * <p>
 * The secret is read from the settings on every call, so a change of the bound settings takes
 * effect without a new instance.
 * <p>
 * A token is checked by {@link LoginTokenVerifier} of the support's core, the same check a Quarkus
 * workflow module runs. So both platforms accept the same tokens.
 */
public class HmacJwtKeySource implements JwtKeySource {

  private final JwtProperties properties;

  /**
   * Creates the key source.
   *
   * @param properties The token settings which hold the secret
   */
  public HmacJwtKeySource(
      final JwtProperties properties) {

    this.properties = properties;

  }

  @Override
  public String sign(
      final JwtClaimsSet claimsSet) {

    final var jwk = new OctetSequenceKey.Builder(properties.getHmacSHA256())
        .keyID(UUID.randomUUID().toString())
        .algorithm(JWSAlgorithm.HS256)
        .build();

    final var encoder = new NimbusJwtEncoder(
        (
            jwkSelector,
            securityContext) -> List.of(jwk));

    final var parameters = JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(),
        claimsSet);

    return encoder
        .encode(parameters)
        .getTokenValue();

  }

  @Override
  public Jwt check(
      final String token) {

    final LoginToken checked;
    try {
      checked = new LoginTokenVerifier(properties.getHmacSHA256()).verify(token);
    } catch (InvalidLoginTokenException e) {
      if (e.isExpired()) {
        throw new JwtValidationException(e.getMessage(), List.of(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, e.getMessage(), null)));
      }
      throw new BadJwtException(e.getMessage(), e);
    }

    final var claims = MappedJwtClaimSetConverter
        .withDefaults(Map.of())
        .convert(checked.claims());
    return Jwt
        .withTokenValue(token)
        .headers(headers -> headers.putAll(checked.headers()))
        .claims(all -> all.putAll(claims))
        .build();

  }

}
