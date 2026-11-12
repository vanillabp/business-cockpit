package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtException;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The mapper signs and checks a token only through its key source. So the key can come from
 * somewhere else later, and the code which uses the mapper does not change.
 */
@ExtendWith(SuppressOutputExtension.class)
class JwtKeySourceTest {

  private JwtProperties properties;

  @BeforeEach
  void setUp() throws Exception {

    properties = propertiesWithANewSecret();

  }

  @Test
  void theMapperSignsAndChecksThroughTheKeySourceItIsGiven() {

    final var recorder = new RecordingKeySource(new HmacJwtKeySource(properties));
    final var mapper = new JwtAuthenticationTokenMapper(properties, recorder, Function.identity());

    final var token = login(mapper);
    final var auth = mapper.toAuth(token);

    assertThat(recorder.calls).containsExactly("sign", "check");
    assertThat(auth.getName()).isEqualTo("alice");

  }

  @Test
  void theDefaultKeySourceIsTheSharedSecretOfTheSettings() {

    final var token = login(new JwtAuthenticationTokenMapper(properties));

    final var jwt = new HmacJwtKeySource(properties).check(token);

    assertThat(jwt.getSubject()).isEqualTo("alice");

  }

  @Test
  void aTokenSignedWithAnotherSecretIsRefused() throws Exception {

    final var token = login(new JwtAuthenticationTokenMapper(propertiesWithANewSecret()));

    final var mapper = new JwtAuthenticationTokenMapper(properties);

    assertThatThrownBy(() -> mapper.toAuth(token)).isInstanceOf(JwtException.class);

  }

  private static JwtProperties propertiesWithANewSecret() throws Exception {

    final var result = new JwtProperties();
    result.setHmacSHA256Base64(Base64
        .getEncoder()
        .encodeToString(KeyGenerator.getInstance("HmacSha256").generateKey().getEncoded()));
    return result;

  }

  private static String login(
      final JwtAuthenticationTokenMapper mapper) {

    final var user = new User("alice", "", List.of(new SimpleGrantedAuthority("GROUP")));
    final var context = new SecurityContextImpl(
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

    return mapper
        .toToken(context)
        .orElseThrow()
        .getKey();

  }

  /** Passes every call on and notes which one it was. */
  private static class RecordingKeySource implements JwtKeySource {

    private final JwtKeySource delegate;

    private final List<String> calls = new ArrayList<>();

    RecordingKeySource(
        final JwtKeySource delegate) {

      this.delegate = delegate;

    }

    @Override
    public String sign(
        final JwtClaimsSet claimsSet) {

      calls.add("sign");
      return delegate.sign(claimsSet);

    }

    @Override
    public Jwt check(
        final String token) {

      calls.add("check");
      return delegate.check(token);

    }

  }

}
