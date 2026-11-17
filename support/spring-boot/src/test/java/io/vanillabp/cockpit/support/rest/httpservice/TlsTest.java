package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

/**
 * TLS: a Spring Boot SSL bundle named in {@code ssl.bundle} holds the certificates the client
 * trusts and its own certificate, like the truststore file of {@code ClientsConfigurationBase}.
 * {@code verify-ssl: false} trusts every server and keeps the client certificate of the bundle. A
 * token request uses the TLS settings of its group.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, TlsTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "inventory.ssl.bundle=inventory", "trusting.verify-ssl=false", "trusting-with-certificate.verify-ssl=false", "trusting-with-certificate.ssl.bundle=certificate-only", "secured.verify-ssl=false", "secured.oauth.client-id=stock-service", "secured.oauth.client-secret=s3cret",
    })
@DirtiesContext
public class TlsTest {

  static final String PASSWORD = "changeit";

  static final HeldCertificate authority = new HeldCertificate.Builder()
      .certificateAuthority(0)
      .commonName("Test CA")
      .build();

  static final HeldCertificate serverCertificate = new HeldCertificate.Builder()
      .commonName("localhost")
      .addSubjectAlternativeName("localhost")
      .addSubjectAlternativeName("127.0.0.1")
      .signedBy(authority)
      .build();

  static final HeldCertificate clientCertificate = new HeldCertificate.Builder()
      .commonName("stock-service")
      .signedBy(authority)
      .build();

  /** Asks for the client's certificate and trusts only certificates of the test authority. */
  static final MockWebServer mutualTlsServer = new MockWebServer();

  /** Has a certificate nobody trusts, like a test server with a self-signed certificate. */
  static final MockWebServer selfSignedServer = new MockWebServer();

  @DynamicPropertySource
  static void tls(
      final DynamicPropertyRegistry registry) {

    if (!mutualTlsServer.getStarted()) {
      mutualTlsServer.useHttps(new HandshakeCertificates.Builder()
          .heldCertificate(serverCertificate, authority.certificate())
          .addTrustedCertificate(authority.certificate())
          .build()
          .sslSocketFactory());
      mutualTlsServer.requireClientAuth();

      final var selfSigned = new HeldCertificate.Builder()
          .addSubjectAlternativeName("localhost")
          .addSubjectAlternativeName("127.0.0.1")
          .build();
      selfSignedServer.useHttps(new HandshakeCertificates.Builder()
          .heldCertificate(selfSigned)
          .build()
          .sslSocketFactory());
      selfSignedServer.setDispatcher(new TokenOrItem());
    }

    final var file = "file:"
        + keystoreFile(true);
    registry.add("inventory.base-url", () -> Stubs.apiUrl(mutualTlsServer));
    registry.add("spring.ssl.bundle.jks.inventory.truststore.location", () -> file);
    registry.add("spring.ssl.bundle.jks.inventory.truststore.password", () -> PASSWORD);
    registry.add("spring.ssl.bundle.jks.inventory.truststore.type", () -> "PKCS12");
    registry.add("spring.ssl.bundle.jks.inventory.keystore.location", () -> file);
    registry.add("spring.ssl.bundle.jks.inventory.keystore.password", () -> PASSWORD);
    registry.add("spring.ssl.bundle.jks.inventory.keystore.type", () -> "PKCS12");

    final var certificateOnly = "file:"
        + keystoreFile(false);
    registry.add("spring.ssl.bundle.jks.certificate-only.keystore.location", () -> certificateOnly);
    registry.add("spring.ssl.bundle.jks.certificate-only.keystore.password", () -> PASSWORD);
    registry.add("spring.ssl.bundle.jks.certificate-only.keystore.type", () -> "PKCS12");

    registry.add("untrusting.base-url", () -> Stubs.apiUrl(selfSignedServer));
    registry.add("trusting.base-url", () -> Stubs.apiUrl(selfSignedServer));
    registry.add("trusting-with-certificate.base-url", () -> Stubs.apiUrl(mutualTlsServer));
    registry.add("secured.base-url", () -> Stubs.apiUrl(selfSignedServer));
    registry.add("secured.oauth.token-uri", () -> Stubs.started(selfSignedServer).url("/oauth/token").toString());

  }

  /** Answers the token endpoint and the API of the self-signed server. */
  static class TokenOrItem extends Dispatcher {

    @Override
    public MockResponse dispatch(
        final RecordedRequest request) {
      return request.getTarget().startsWith("/oauth/token")
          ? Stubs.json(200, """
              {"access_token": "token-1", "token_type": "Bearer", "expires_in": 3600}""")
          : Stubs.item("1");
    }

  }

  /** A PKCS12 file with the client's own certificate and key, and the authority to trust if asked. */
  static Path keystoreFile(
      final boolean withAuthority) {

    try {
      final var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      if (withAuthority) {
        keyStore.setCertificateEntry("test-ca", authority.certificate());
      }
      keyStore.setKeyEntry("client", clientCertificate.keyPair().getPrivate(), PASSWORD.toCharArray(),
          new Certificate[]{
              clientCertificate.certificate(), authority.certificate()
          });
      final var file = Files.createTempFile("http-service-keystore", ".p12");
      file.toFile().deleteOnExit();
      try (OutputStream out = Files.newOutputStream(file)) {
        keyStore.store(out, PASSWORD.toCharArray());
      }
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

  @AfterAll
  static void stop() {
    mutualTlsServer.close();
    selfSignedServer.close();
  }

  interface Inventory extends InventoryApi {
  }

  interface UntrustingClient extends InventoryApi {
  }

  interface TrustingClient extends InventoryApi {
  }

  interface TrustingWithCertificateClient extends InventoryApi {
  }

  interface SecuredClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "inventory", types = Inventory.class)
  @ImportHttpServices(group = "untrusting", types = UntrustingClient.class)
  @ImportHttpServices(group = "trusting", types = TrustingClient.class)
  @ImportHttpServices(group = "trusting-with-certificate", types = TrustingWithCertificateClient.class)
  @ImportHttpServices(group = "secured", types = SecuredClient.class)
  static class Clients {

    @Bean
    HttpServiceGroupPrefix inventoryKeys() {
      return HttpServiceGroupPrefix.of("inventory", "inventory");
    }

    @Bean
    HttpServiceGroupPrefix untrustingKeys() {
      return HttpServiceGroupPrefix.of("untrusting", "untrusting");
    }

    @Bean
    HttpServiceGroupPrefix trustingKeys() {
      return HttpServiceGroupPrefix.of("trusting", "trusting");
    }

    @Bean
    HttpServiceGroupPrefix trustingWithCertificateKeys() {
      return HttpServiceGroupPrefix.of("trusting-with-certificate", "trusting-with-certificate");
    }

    @Bean
    HttpServiceGroupPrefix securedKeys() {
      return HttpServiceGroupPrefix.of("secured", "secured");
    }

  }

  @Autowired
  Inventory inventory;

  @Autowired
  UntrustingClient untrusting;

  @Autowired
  TrustingClient trusting;

  @Autowired
  TrustingWithCertificateClient trustingWithCertificate;

  @Autowired
  SecuredClient secured;

  @BeforeEach
  void forgetEarlierRequests() {
    Stubs.drain(mutualTlsServer);
  }

  @Test
  public void the_bundle_makes_the_client_trust_the_server_and_show_its_own_certificate() {

    mutualTlsServer.enqueue(Stubs.item("1"));

    assertThat(inventory.getItem("1").getBody().getId()).isEqualTo("1");

    assertThat(certificateShownTo(mutualTlsServer)).contains("CN=stock-service");

  }

  @Test
  public void without_a_bundle_the_server_certificate_is_checked() {

    assertThatThrownBy(() -> untrusting.getItem("1"))
        .hasStackTraceContaining("PKIX path building failed");

  }

  @Test
  public void verify_ssl_false_trusts_every_server() {

    assertThat(trusting.getItem("1").getBody().getId()).isEqualTo("1");

  }

  @Test
  public void verify_ssl_false_keeps_the_client_certificate_of_the_bundle() {

    mutualTlsServer.enqueue(Stubs.item("1"));

    assertThat(trustingWithCertificate.getItem("1").getBody().getId()).isEqualTo("1");

    assertThat(certificateShownTo(mutualTlsServer)).contains("CN=stock-service");

  }

  @Test
  public void a_token_request_uses_the_tls_settings_of_its_group() {

    // Spring Security's own client would refuse the self-signed certificate of the token endpoint
    assertThat(secured.getItem("1").getBody().getId()).isEqualTo("1");

  }

  private static String certificateShownTo(
      final MockWebServer server) {

    final var certificate = (X509Certificate) Stubs.next(server).getHandshake().peerCertificates().getFirst();
    return certificate.getSubjectX500Principal().getName();

  }

}
