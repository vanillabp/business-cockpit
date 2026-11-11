package io.vanillabp.cockpit.support.security.jwt;

import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;

/**
 * Signs and checks login tokens with the shared secret of the settings
 * ({@code hmacSHA256-base64}), using HMAC with SHA-256.
 * <p>
 * The secret is read from the settings on every call, so a change of the bound settings takes
 * effect without a new instance.
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

    final var key = new SecretKeySpec(
        properties.getHmacSHA256(), "HMACSHA256");

    final var decoder = NimbusJwtDecoder
        .withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();

    return decoder.decode(token);

  }

}
