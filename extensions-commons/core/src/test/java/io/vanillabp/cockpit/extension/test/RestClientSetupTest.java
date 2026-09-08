package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import feign.Client;
import feign.DefaultClient;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.transport.RestClientSetup;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the client talking to the cockpit server is built with.
 * <p>
 * The assertions are about the pieces rather than about a connection, because what a
 * certificate check does is only visible against a server presenting a certificate - which a
 * test cannot issue itself. What a test can say is which stack a configuration leads to, and
 * that a deployment configuring nothing keeps the one the generated client brings.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RestClientSetupTest {

  private static RestTransportConfiguration configuration(
      final Duration connectTimeout,
      final Duration readTimeout,
      final RestTransportConfiguration.Proxy proxy,
      final boolean verifySsl,
      final String truststore,
      final String truststorePassword) {

    return new RestTransportConfiguration(
        "http://localhost:8080", null, null, connectTimeout, readTimeout, proxy, verifySsl, truststore, truststorePassword, null);

  }

  /**
   * @param directory Where the file is written
   * @param password Its password
   * @return A PKCS12 file holding no certificate at all, which is enough to say whether it was
   *         opened
   */
  private static String anEmptyTruststore(
      final Path directory,
      final String password) throws Exception {

    final var keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, password.toCharArray());
    final var file = directory.resolve("cockpit.p12");
    try (OutputStream out = Files.newOutputStream(file)) {
      keyStore.store(out, password.toCharArray());
    }
    return file.toString();

  }

  @Test
  @DisplayName("What was not configured keeps the timeouts the client brings")
  public void unconfiguredTimeoutsStayTheClientsOwn() {

    final var defaults = RestClientSetup
        .timeoutsOf(configuration(null, null, null, true, null, null));
    final var configured = RestClientSetup
        .timeoutsOf(
            configuration(Duration.ofMillis(250), null, null, true, null, null));

    assertEquals(250, configured.connectTimeoutMillis());
    assertEquals(
        defaults.readTimeoutMillis(), configured.readTimeoutMillis(),
        "a connect timeout of its own changed how long the server may take to answer");

  }

  @Test
  @DisplayName("A deployment which configures nothing keeps the generated client's own stack")
  public void nothingConfiguredKeepsTheGeneratedClient() {

    assertNull(RestClientSetup.clientOf(configuration(null, null, null, true, null, null)));
    assertNull(RestClientSetup.sslContextOf(configuration(null, null, null, true, null, null)));
    assertNull(
        RestClientSetup.hostnameVerifierOf(configuration(null, null, null, true, null, null)));

  }

  @Test
  @DisplayName("A certificate check switched off trusts and accepts everything")
  public void aCheckSwitchedOffTrustsEverything() {

    final var configuration = configuration(null, null, null, false, null, null);

    assertInstanceOf(DefaultClient.class, RestClientSetup.clientOf(configuration));
    assertNotNull(RestClientSetup.sslContextOf(configuration));
    assertTrue(
        RestClientSetup.hostnameVerifierOf(configuration).verify("nobody.example", null),
        "a check which was switched off still refused a host");

  }

  @Test
  @DisplayName("A truststore of the deployment is opened while the client is built")
  public void aTruststoreIsOpened(
      @TempDir final Path directory) throws Exception {

    final var truststore = anEmptyTruststore(directory, "changeit");

    assertNotNull(
        RestClientSetup
            .sslContextOf(configuration(null, null, null, true, truststore, "changeit")));

    final var wrongPassword = assertThrows(
        IllegalStateException.class,
        () -> RestClientSetup
            .sslContextOf(configuration(null, null, null, true, truststore, "wrong")));
    assertTrue(wrongPassword.getMessage().contains(truststore), wrongPassword.getMessage());
    assertTrue(wrongPassword.getMessage().contains("PKCS12"), wrongPassword.getMessage());

  }

  @Test
  @DisplayName("A configured proxy is what the requests go through, with what it expects")
  public void aProxyIsUsedAndAuthenticated() {

    final var withoutAUser = configuration(
        null, null, new RestTransportConfiguration.Proxy("proxy.internal", 3128, null, null),
        true, null, null);
    final var withAUser = configuration(
        null, null,
        new RestTransportConfiguration.Proxy("proxy.internal", 3128, "walter", "s3cret"), true,
        null, null);

    assertInstanceOf(Client.Proxied.class, RestClientSetup.clientOf(withoutAUser));
    assertNull(RestClientSetup.proxyAuthorizationOf(withoutAUser.proxy()));
    assertInstanceOf(Client.Proxied.class, RestClientSetup.clientOf(withAUser));
    assertEquals(
        "Basic d2FsdGVyOnMzY3JldA==", RestClientSetup.proxyAuthorizationOf(withAUser.proxy()));
    assertNull(RestClientSetup.proxyAuthorizationOf(null));

  }

}
