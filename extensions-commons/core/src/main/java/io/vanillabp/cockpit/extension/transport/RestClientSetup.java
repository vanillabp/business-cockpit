package io.vanillabp.cockpit.extension.transport;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import feign.Client;
import feign.DefaultClient;
import feign.Request;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;

/**
 * What the client talking to the cockpit server is built with, beyond the address it talks to.
 * <p>
 * A deployment which reaches the server directly, over a certificate the JVM trusts, needs
 * none of this and gets the client the generated code brings. What a deployment cannot arrange
 * any other way is arranged here: the proxy in front of it, the certificate its own authority
 * signed, and how long it is willing to wait. Every one of these was configurable in version 1
 * of the Business Cockpit, and an application which passed a proxy could not upgrade without
 * them.
 */
public final class RestClientSetup {

  private RestClientSetup() {
  }

  /**
   * How long the client waits, with the client's own defaults for whatever was not configured.
   *
   * @param configuration What the application configured
   * @return The timeouts
   */
  public static Request.Options timeoutsOf(
      final RestTransportConfiguration configuration) {

    final var defaults = new Request.Options();
    return new Request.Options(
        configuration.connectTimeout() == null
            ? Duration.ofMillis(defaults.connectTimeoutMillis())
            : configuration.connectTimeout(), configuration.readTimeout() == null
                ? Duration.ofMillis(defaults.readTimeoutMillis())
                : configuration.readTimeout(), true);

  }

  /**
   * The HTTP stack the requests go through.
   * <p>
   * It is built only where the configuration asks for something the generated client's own
   * stack cannot be told: a proxy, a truststore of the deployment, or a certificate check
   * switched off. Everywhere else the generated client keeps the stack it was generated with,
   * so an application which configures nothing here also changes nothing about how it talks.
   *
   * @param configuration What the application configured
   * @return The client, or <code>null</code> where the generated one is kept
   */
  public static Client clientOf(
      final RestTransportConfiguration configuration) {

    if (!configuration.needsAClientOfItsOwn()) {
      return null;
    }
    final var socketFactory = socketFactoryOf(configuration);
    final var hostnameVerifier = hostnameVerifierOf(configuration);
    final var proxy = configuration.proxy();
    if (proxy == null) {
      return new DefaultClient(socketFactory, hostnameVerifier);
    }
    final var address = new Proxy(
        Proxy.Type.HTTP, new InetSocketAddress(proxy.host(), proxy.port()));
    return proxy.authenticates()
        ? new Client.Proxied(
            socketFactory, hostnameVerifier, address, proxy.username(), proxy.password())
        : new Client.Proxied(socketFactory, hostnameVerifier, address);

  }

  /**
   * The certificates the cockpit server's is checked against.
   *
   * @param configuration What the application configured
   * @return The context, or <code>null</code> where the JVM's own certificates are trusted and
   *         the check is left on
   */
  public static SSLContext sslContextOf(
      final RestTransportConfiguration configuration) {

    if (configuration.verifySsl() && !configuration.hasOwnTruststore()) {
      return null;
    }
    try {
      final var context = SSLContext.getInstance("TLS");
      context.init(null, trustManagersOf(configuration), null);
      return context;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalStateException(
          """
              The truststore '%s' could not be read. It has to be a PKCS12 file, and \
              'ssl-truststore-password' has to be its password."""
              .formatted(configuration.sslTruststoreFilename()), e);
    }

  }

  /**
   * @param configuration What the application configured
   * @return What every request against the cockpit server checks the server's name with, or
   *         <code>null</code> where the JVM's own check is left on
   */
  public static HostnameVerifier hostnameVerifierOf(
      final RestTransportConfiguration configuration) {

    return configuration.verifySsl()
        ? null
        : (
            hostname,
            session) -> true;

  }

  /**
   * @param proxy The proxy, or <code>null</code>
   * @return What a request carries as its <code>Proxy-Authorization</code>, or
   *         <code>null</code> where the proxy expects no user
   */
  public static String proxyAuthorizationOf(
      final RestTransportConfiguration.Proxy proxy) {

    if ((proxy == null) || !proxy.authenticates()) {
      return null;
    }
    return basic(proxy.username(), proxy.password());

  }

  /**
   * @param username The user
   * @param password Its password
   * @return The value of an <code>Authorization</code> header of the basic scheme
   */
  public static String basic(
      final String username,
      final String password) {

    return "Basic "
        + Base64
            .getEncoder()
            .encodeToString(
                "%s:%s"
                    .formatted(username, password == null ? "" : password)
                    .getBytes(StandardCharsets.UTF_8));

  }

  private static SSLSocketFactory socketFactoryOf(
      final RestTransportConfiguration configuration) {

    final var context = sslContextOf(configuration);
    return context == null ? null : context.getSocketFactory();

  }

  private static TrustManager[] trustManagersOf(
      final RestTransportConfiguration configuration) throws Exception {

    if (!configuration.verifySsl()) {
      return new TrustManager[]{
          trustingEverything()
      };
    }
    final var keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream file = Files
        .newInputStream(Path.of(configuration.sslTruststoreFilename()))) {
      keyStore
          .load(
              file,
              configuration.sslTruststorePassword() == null
                  ? null
                  : configuration.sslTruststorePassword().toCharArray());
    }
    final var factory = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(keyStore);
    return factory.getTrustManagers();

  }

  /**
   * What a deployment gets which switched the certificate check off - a test setup, or a
   * cockpit server inside a network which is trusted as a whole. It trusts everything, which is
   * why 'verify-ssl' is the one key of the REST section whose message says what it costs.
   */
  private static X509TrustManager trustingEverything() {

    return new X509TrustManager() {

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
      public X509Certificate[] getAcceptedIssuers() {

        return new X509Certificate[]{};

      }

    };

  }

}
