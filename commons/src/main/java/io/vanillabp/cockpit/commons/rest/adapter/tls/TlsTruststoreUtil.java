package io.vanillabp.cockpit.commons.rest.adapter.tls;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

/**
 * Builds the TLS helpers a REST client needs: trust managers and key managers out of a PKCS12
 * file, and helpers which turn off the certificate and host name checks.
 */
public class TlsTruststoreUtil {

  /**
   * There is no instance, the class only has static methods.
   */
  private TlsTruststoreUtil() {
  }

  /**
   * Builds a trust manager which trusts every certificate. Use it only for tests.
   *
   * @return The trust manager
   */
  public static X509TrustManager noCertificateCheckTrustManager() {

    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(
          X509Certificate[] chain,
          String authType) {

      }

      @Override
      public void checkServerTrusted(
          X509Certificate[] chain,
          String authType) {

      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{};
      }
    };

  }

  /**
   * Builds trust managers which trust the certificates in a PKCS12 file.
   *
   * @param filename         The path of the PKCS12 file
   * @param keystorePassword The password of the file
   * @return The trust managers
   */
  @SuppressWarnings("resource")
  public static TrustManager[] clientCertificateCheckTrustManagers(
      final String filename,
      final String keystorePassword) {

    try {
      final var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(new FileInputStream(filename), keystorePassword.toCharArray());
      final var trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustFactory.init(keyStore);
      return trustFactory.getTrustManagers();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds key managers out of a PKCS12 file, so the client can show its own client certificate.
   *
   * @param filename         The path of the PKCS12 file
   * @param keystorePassword The password of the file and of the private key in it
   * @return The key managers
   */
  @SuppressWarnings("resource")
  public static KeyManager[] clientCertificateCheckKeyManagers(
      final String filename,
      final String keystorePassword) {

    try {
      final var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(new FileInputStream(filename), keystorePassword.toCharArray());
      final var keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyFactory.init(keyStore, keystorePassword.toCharArray());
      return keyFactory.getKeyManagers();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Builds a host name verifier which accepts every host name. Use it only for tests.
   *
   * @return The verifier
   */
  public static HostnameVerifier noHostnameCheckVerifier() {

    return new HostnameVerifier() {
      @Override
      public boolean verify(
          String hostname,
          SSLSession session) {
        return true;
      }
    };

  }

}
