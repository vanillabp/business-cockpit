package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.ProcessingException;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

/**
 * TLS: a server with a certificate nobody trusts is refused. {@code tls-configuration-name} names a
 * configuration of the Quarkus TLS registry which trusts it, the Quarkus counterpart of Spring
 * Boot's {@code ssl.bundle}. {@code verify-ssl: false} trusts every server, and a token request
 * uses the TLS settings of its clients.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TlsTest {

  private static final String STUB = "tls";

  /**
   * The certificate of the server, made once and kept in a system property, so the copy of this
   * class which configures the application and the copy which starts the server use the same one.
   */
  static final HeldCertificate certificate = certificateOfTheServer();

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("untrusting.base-url", Stubs.url("https", STUB, "/api"))
      .overrideConfigKey("trusting.base-url", Stubs.url("https", STUB, "/api"))
      .overrideConfigKey("trusting.verify-ssl", "false")
      .overrideConfigKey("trusting.oauth.token-uri", Stubs.url("https", STUB, "/oauth/token"))
      .overrideConfigKey("trusting.oauth.client-id", "stock-service")
      .overrideConfigKey("trusting.oauth.client-secret", "s3cret")
      .overrideConfigKey("with-trust-store.base-url", Stubs.url("https", STUB, "/api"))
      .overrideConfigKey("with-trust-store.tls-configuration-name", "inventory")
      .overrideConfigKey("quarkus.tls.inventory.trust-store.pem.certs", certificateFile().toString())
      .overrideConfigKey("unknown-tls.base-url", Stubs.url("https", STUB, "/api"))
      .overrideConfigKey("unknown-tls.tls-configuration-name", "nowhere");

  static final MockWebServer server = new MockWebServer();

  private static HeldCertificate certificateOfTheServer() {

    synchronized (System.getProperties()) {
      final var pem = System.getProperty("restclient-test.tls.certificate");
      if (pem != null) {
        return HeldCertificate.decode(pem);
      }
      final var created = new HeldCertificate.Builder()
          .addSubjectAlternativeName("localhost")
          .addSubjectAlternativeName("127.0.0.1")
          .build();
      System.setProperty("restclient-test.tls.certificate", created.certificatePem() + created.privateKeyPkcs8Pem());
      return created;
    }

  }

  private static Path certificateFile() {

    try {
      final var file = Files.createTempFile("inventory", ".pem");
      Files.writeString(file, certificate.certificatePem());
      file.toFile().deleteOnExit();
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  @BeforeEach
  void startTheServer() {

    if (!server.getStarted()) {
      server.useHttps(new HandshakeCertificates.Builder()
          .heldCertificate(certificate)
          .build()
          .sslSocketFactory());
      server.setDispatcher(new Dispatcher() {

        @Override
        public MockResponse dispatch(
            final RecordedRequest request) {
          return request.getTarget().startsWith("/oauth/token")
              ? Stubs.json(200, """
                  {"access_token": "token-1", "expires_in": 3600}""")
              : Stubs.item("1");
        }

      });
    }
    Stubs.started(server, STUB);

  }

  private static InventoryApi client(
      final String prefix) {
    return TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, prefix);
  }

  @Test
  public void a_server_nobody_trusts_is_refused() {

    assertThatThrownBy(() -> client("untrusting").getItem("1")).isInstanceOf(ProcessingException.class);

  }

  @Test
  public void verify_ssl_false_trusts_every_server_also_for_token_requests() {

    assertThat(client("trusting").getItem("1").getId()).isEqualTo("1");

  }

  @Test
  public void a_tls_configuration_of_the_registry_holds_the_certificates_to_trust() {

    assertThat(client("with-trust-store").getItem("1").getId()).isEqualTo("1");

  }

  @Test
  public void an_unknown_tls_configuration_is_named() {

    assertThatThrownBy(() -> client("unknown-tls"))
        .isInstanceOf(RestClientsMisconfigured.class)
        .hasMessageContaining("name the TLS configuration 'nowhere' in 'unknown-tls.tls-configuration-name'")
        .hasMessageContaining("'quarkus.tls.nowhere.trust-store.pem.certs'");

  }

}
