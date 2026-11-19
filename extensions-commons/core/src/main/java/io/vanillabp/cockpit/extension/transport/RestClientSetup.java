package io.vanillabp.cockpit.extension.transport;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.extension.config.RestConnection;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;

/**
 * What the client talking to the cockpit server is built with, beyond the address it talks to.
 * <p>
 * The requests go through the JDK's own {@link HttpClient}, so the transport brings no HTTP
 * library. A deployment which reaches the server directly, over a certificate the JVM trusts,
 * needs nothing of this beyond the timeouts. What a deployment cannot arrange any other way is
 * arranged here: the proxy in front of it, the certificate its own authority signed, and how long
 * it is willing to wait. Every one of these was configurable in version 1 of the Business
 * Cockpit, and an application which passed a proxy could not upgrade without them.
 */
public final class RestClientSetup {

  private RestClientSetup() {
  }

  /**
   * How long the client waits for a connection. A configuration read from an application always
   * carries it, version 1's where nothing was written. A configuration built in a test may leave
   * it out, and the same default fills it in.
   *
   * @param configuration What the application configured about this connection
   * @return The timeout
   */
  public static Duration connectTimeoutOf(
      final RestConnection configuration) {

    return configuration.connectTimeout() == null
        ? RestConnection.DEFAULT_CONNECT_TIMEOUT
        : configuration.connectTimeout();

  }

  /**
   * How long the client waits for the answer, filled in like {@link #connectTimeoutOf}.
   *
   * @param configuration What the application configured about this connection
   * @return The timeout
   */
  public static Duration readTimeoutOf(
      final RestConnection configuration) {

    return configuration.readTimeout() == null
        ? RestConnection.DEFAULT_READ_TIMEOUT
        : configuration.readTimeout();

  }

  /**
   * The HTTP stack the requests go through.
   * <p>
   * It speaks HTTP/1.1, like the client of version 1. The JDK's client would otherwise ask a
   * server reached over plain HTTP with every request to switch to HTTP/2. A proxy, a
   * truststore of the deployment and a certificate check switched off are set only where the
   * configuration asks for them. The login for the proxy is not part of the stack. A request
   * carries it, see {@link #proxyAuthorizationOf}.
   *
   * @param configuration What the application configured
   * @return The builder of the client, for the caller to build
   */
  public static HttpClient.Builder httpClientOf(
      final RestConnection configuration) {

    final var builder = HttpClient
        .newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(connectTimeoutOf(configuration));
    final var proxy = configuration.proxy();
    if (proxy != null) {
      builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.host(), proxy.port())));
    }
    final var sslContext = sslContextOf(configuration);
    if (sslContext != null) {
      builder.sslContext(sslContext);
    }
    return builder;

  }

  /**
   * What turns a report into the JSON body of its request.
   * <p>
   * It is the generated client's own mapper with one change: a field without a value is sent as
   * <code>null</code> instead of being left out. The generated client leaves such fields out,
   * the client of version 1 sent them, and the cockpit server and the tests of every adapter
   * read the body the way version 1 wrote it. A completed case without a creation time, for
   * example, carries <code>"createdAt":null</code>.
   *
   * @return The mapper for the client to use
   */
  public static ObjectMapper objectMapper() {

    final var mapper = ApiClient.createDefaultObjectMapper();
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
    return mapper;

  }

  /**
   * The certificates the cockpit server's is checked against.
   *
   * @param configuration What the application configured
   * @return The context, or <code>null</code> where the JVM's own certificates are trusted and
   *         the check is left on
   */
  public static SSLContext sslContextOf(
      final RestConnection configuration) {

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
   * Builds the login for the proxy.
   *
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
   * Builds the value of a basic authentication.
   *
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

  private static TrustManager[] trustManagersOf(
      final RestConnection configuration) throws Exception {

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
   * What a deployment gets which switched the certificate check off, for example a test setup,
   * or a cockpit server inside a network which is trusted as a whole. It trusts everything, the
   * name of the host included, which is why 'verify-ssl' is the one key of the REST section whose
   * message says what it costs.
   * <p>
   * It is an {@link X509ExtendedTrustManager} on purpose. The JDK wraps a plain
   * {@code X509TrustManager} into one which checks the host name all the same, and the JDK's
   * {@link HttpClient} has no other way to switch that check off.
   */
  private static X509ExtendedTrustManager trustingEverything() {

    return new X509ExtendedTrustManager() {

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

        return new X509Certificate[]{};

      }

    };

  }

}
