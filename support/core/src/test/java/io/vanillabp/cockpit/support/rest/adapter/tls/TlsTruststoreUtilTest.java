package io.vanillabp.cockpit.support.rest.adapter.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The TLS helpers read a PKCS12 file, and the helpers for tests turn the checks off.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TlsTruststoreUtilTest {

  private static final String PASSWORD = "secret";

  @TempDir
  Path folder;

  @Test
  public void thePkcs12FileGivesTrustManagersAndKeyManagers() throws Exception {

    final var file = anEmptyPkcs12File();

    assertThat(TlsTruststoreUtil.clientCertificateCheckTrustManagers(file, PASSWORD)).isNotEmpty();
    assertThat(TlsTruststoreUtil.clientCertificateCheckKeyManagers(file, PASSWORD)).isNotEmpty();

  }

  @Test
  public void aMissingFileIsReportedAsItIs() {

    final var missing = folder.resolve("missing.p12").toString();

    assertThatThrownBy(() -> TlsTruststoreUtil.clientCertificateCheckTrustManagers(missing, PASSWORD))
        .hasRootCauseInstanceOf(FileNotFoundException.class);
    assertThatThrownBy(() -> TlsTruststoreUtil.clientCertificateCheckKeyManagers(missing, PASSWORD))
        .hasRootCauseInstanceOf(FileNotFoundException.class);

  }

  @Test
  public void theTrustManagerForTestsTrustsEveryCertificate() throws Exception {

    final X509TrustManager trustManager = TlsTruststoreUtil.noCertificateCheckTrustManager();
    final var chain = new X509Certificate[0];

    trustManager.checkClientTrusted(chain, "RSA");
    trustManager.checkServerTrusted(chain, "RSA");
    assertThat(trustManager.getAcceptedIssuers()).isEmpty();

  }

  @Test
  public void theHostnameVerifierForTestsAcceptsEveryHost() {

    assertThat(TlsTruststoreUtil.noHostnameCheckVerifier().verify("anywhere.example.com", null)).isTrue();

  }

  private String anEmptyPkcs12File() throws Exception {

    final var keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, PASSWORD.toCharArray());
    final var file = folder.resolve("empty.p12");
    try (var out = new FileOutputStream(file.toFile())) {
      keyStore.store(out, PASSWORD.toCharArray());
    }
    return file.toString();

  }

}
