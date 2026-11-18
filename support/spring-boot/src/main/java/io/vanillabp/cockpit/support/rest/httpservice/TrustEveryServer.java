package io.vanillabp.cockpit.support.rest.httpservice;

import java.net.Socket;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;

/**
 * {@code verify-ssl: false} as a Spring Boot SSL bundle: it trusts every server. Spring Boot has no
 * such switch, and should not have one for production. It is for test systems with self-signed
 * certificates.
 * <p>
 * The trust manager is an {@link X509ExtendedTrustManager} on purpose. The JDK wraps a plain
 * {@code X509TrustManager} and then still checks the host name. The REST client of version 1
 * did not check it either.
 */
final class TrustEveryServer {

  private TrustEveryServer() {
  }

  /**
   * A bundle which trusts every server. If the group names a bundle of its own, its client
   * certificate is kept, and only its trust is replaced.
   */
  static SslBundle bundle(
      final @Nullable SslBundle ownBundle) {

    final var trustEverything = SslManagerBundle.from(new AcceptingEveryCertificate());
    if (ownBundle == null) {
      return SslBundle.of(null, null, null, null, trustEverything);
    }
    return SslBundle.of(ownBundle.getStores(), ownBundle.getKey(), ownBundle.getOptions(),
        ownBundle.getProtocol(), SslManagerBundle.of(ownBundle.getManagers().getKeyManagerFactory(),
            trustEverything.getTrustManagerFactory()));

  }

  private static class AcceptingEveryCertificate extends X509ExtendedTrustManager {

    @Override
    public void checkClientTrusted(
        final X509Certificate[] chain,
        final String authType) {
    }

    @Override
    public void checkServerTrusted(
        final X509Certificate[] chain,
        final String authType) {
    }

    @Override
    public void checkClientTrusted(
        final X509Certificate[] chain,
        final String authType,
        final Socket socket) {
    }

    @Override
    public void checkServerTrusted(
        final X509Certificate[] chain,
        final String authType,
        final Socket socket) {
    }

    @Override
    public void checkClientTrusted(
        final X509Certificate[] chain,
        final String authType,
        final SSLEngine engine) {
    }

    @Override
    public void checkServerTrusted(
        final X509Certificate[] chain,
        final String authType,
        final SSLEngine engine) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

  }

}
