package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.transport.RestClientSetup;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the client talking to the cockpit server is built with.
 * <p>
 * The tests check the parts the client is built from, not a connection. A certificate check
 * only shows against a server which presents a certificate, and a test cannot issue one itself.
 * A test can say what the JDK's client is built with for a configuration, and that a deployment
 * which configures nothing gets the client as the JDK builds it.
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
  @DisplayName("What was not configured keeps the timeouts of version 1")
  public void unconfiguredTimeoutsStayTheDefaults() {

    final var configured = configuration(Duration.ofMillis(250), null, null, true, null, null);

    assertEquals(Duration.ofMillis(250), RestClientSetup.connectTimeoutOf(configured));
    assertEquals(
        RestTransportConfiguration.DEFAULT_READ_TIMEOUT, RestClientSetup.readTimeoutOf(configured),
        "a connect timeout of its own changed how long the server may take to answer");
    assertEquals(
        Duration.ofMillis(250), RestClientSetup.httpClientOf(configured).build().connectTimeout().orElseThrow());

  }

  @Test
  @DisplayName("A deployment which configures nothing gets the JDK's client with HTTP/1.1 and no proxy")
  public void nothingConfiguredKeepsTheJdksClient() {

    final var client = RestClientSetup
        .httpClientOf(configuration(null, null, null, true, null, null))
        .build();

    assertEquals(HttpClient.Version.HTTP_1_1, client.version());
    assertTrue(client.proxy().isEmpty(), "a client without a configured proxy has a proxy");
    assertNull(RestClientSetup.sslContextOf(configuration(null, null, null, true, null, null)));

  }

  @Test
  @DisplayName("A certificate check switched off trusts everything")
  public void aCheckSwitchedOffTrustsEverything() throws Exception {

    final var configuration = configuration(null, null, null, false, null, null);

    assertNotNull(RestClientSetup.sslContextOf(configuration));
    assertNotEquals(
        SSLContext.getDefault(), RestClientSetup.httpClientOf(configuration).build().sslContext(),
        "a check which was switched off kept the JVM's own certificates");

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

    final var proxy = RestClientSetup
        .httpClientOf(withoutAUser)
        .build()
        .proxy()
        .orElseThrow()
        .select(URI.create("http://cockpit.internal:8080"));
    assertEquals(
        List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.internal", 3128))), proxy);
    assertNull(RestClientSetup.proxyAuthorizationOf(withoutAUser.proxy()));
    assertEquals(
        "Basic d2FsdGVyOnMzY3JldA==", RestClientSetup.proxyAuthorizationOf(withAUser.proxy()));
    assertNull(RestClientSetup.proxyAuthorizationOf(null));

  }

}
