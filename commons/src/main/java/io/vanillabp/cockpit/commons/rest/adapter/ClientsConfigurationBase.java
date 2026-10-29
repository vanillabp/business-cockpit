package io.vanillabp.cockpit.commons.rest.adapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import feign.DefaultRetryer;
import feign.Feign.Builder;
import feign.Request;
import feign.Retryer;
import feign.form.FormEncoder;
import feign.jackson3.Jackson3Decoder;
import feign.jackson3.Jackson3Encoder;
import io.vanillabp.cockpit.commons.rest.adapter.oauth.OauthBearerTokenHandler;
import io.vanillabp.cockpit.commons.rest.adapter.tls.TlsTruststoreUtil;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * The base for Spring configurations which build Feign REST clients out of {@link Client} settings.
 * It sets timeouts, TLS, proxy, logging, retries, JSON encoding and authentication.
 */
public abstract class ClientsConfigurationBase {

  /**
   * The mapper for the JSON bodies of the requests and responses. If there is none, Feign keeps
   * its own encoder and decoder.
   * <p>
   * Jackson 3: the Feign codecs take a JsonMapper rather than the ObjectMapper base class. Spring Boot 4
   * auto-configures exactly that bean ("jacksonJsonMapper"), so this stays a plain injection.
   */
  @Autowired
  protected Optional<JsonMapper> objectMapper;

  /**
   * Creates the base. The JSON mapper is injected by Spring afterwards.
   */
  public ClientsConfigurationBase() {
  }

  /**
   * Applies the TLS, proxy and logging settings to an OkHttp client.
   *
   * @param clientClass            The class the log messages of the client are written for
   * @param properties             The settings of the client
   * @param httpClientBuilder      The builder of the OkHttp client to change
   * @param adoptHttpClientBuilder Changes the builder after the TLS settings and before the proxy
   *                               and logging settings, or {@code null}
   */
  protected void configureOkHttpClient(
      final Class<?> clientClass,
      final Client properties,
      final OkHttpClient.Builder httpClientBuilder,
      final Consumer<OkHttpClient.Builder> adoptHttpClientBuilder) {

    configureTls(properties, httpClientBuilder);

    if (adoptHttpClientBuilder != null) {
      adoptHttpClientBuilder.accept(httpClientBuilder);
    }
    configureProxyAndLogger(httpClientBuilder, clientClass, properties);

  }

  /**
   * Applies all settings to a Feign builder.
   *
   * @param clientClass The class the log messages of the client are written for
   * @param builder     The Feign builder to change
   * @param properties  The settings of the client
   */
  protected void configureFeignBuilder(
      final Class<?> clientClass,
      final Builder builder,
      final Client properties) {

    configureFeignBuilder(clientClass, builder, properties, null);

  }

  /**
   * Tells Feign whether and how often to send a failed request again. Without retry settings, or
   * with retries turned off, a request is sent only once.
   *
   * @param builder    The Feign builder to change
   * @param properties The settings of the client
   */
  protected void configureRetry(
      final Builder builder,
      final Client properties) {

    final var retryProperties = properties.getRetry();
    if ((retryProperties == null) || !retryProperties.isEnabled()) {

      builder.retryer(Retryer.NEVER_RETRY);
      return;

    }

    builder.retryer(new DefaultRetryer(
        retryProperties.getPeriod().toMillis(), retryProperties.getMaxPeriod().toMillis(), retryProperties
            .getMaxAttempts()));

  }

  /**
   * Applies all settings to a Feign builder: timeouts, the OkHttp client, retries, JSON encoding
   * and authentication.
   *
   * @param clientClass            The class the log messages of the client are written for
   * @param builder                The Feign builder to change
   * @param properties             The settings of the client
   * @param adoptHttpClientBuilder Changes the OkHttp builder after the TLS settings and before the
   *                               proxy and logging settings, or {@code null}
   */
  protected void configureFeignBuilder(
      final Class<?> clientClass,
      final Builder builder,
      final Client properties,
      final Consumer<OkHttpClient.Builder> adoptHttpClientBuilder) {

    builder.options(new Request.Options(
        properties.getConnectTimeout(), TimeUnit.MILLISECONDS, properties
            .getReadTimeout(), TimeUnit.MILLISECONDS, true));

    OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
    configureOkHttpClient(clientClass, properties, httpClientBuilder, adoptHttpClientBuilder);

    builder.client(new feign.okhttp.OkHttpClient(httpClientBuilder.build()));

    configureRetry(builder, properties);

    configureJsonEncoding(builder);

    if (configureBasicAuthentication(builder, properties)) {
      return;
    }
    if (configureOauthAuthentication(builder, properties)) {
      return;
    }

  }

  /**
   * Lets Feign read and write JSON bodies with the injected JSON mapper. Form bodies are written as
   * forms. Without a JSON mapper Feign keeps its own encoder and decoder.
   *
   * @param builder The Feign builder to change
   */
  protected void configureJsonEncoding(
      final Builder builder) {

    if (objectMapper.isPresent()) {
      builder.decoder(
          new Jackson3Decoder(objectMapper.get()));
      builder.encoder(
          new FormEncoder(new Jackson3Encoder(objectMapper.get())));
    }

  }

  /**
   * Applies the TLS settings to an OkHttp client. With a truststore file, the client trusts the
   * certificates in it and shows its own client certificate. Without a truststore file and with
   * {@code verifySsl} off, the client trusts every certificate.
   *
   * @param properties        The settings of the client
   * @param httpClientBuilder The builder of the OkHttp client to change
   */
  protected void configureTls(
      final Client properties,
      OkHttpClient.Builder httpClientBuilder) {

    if (StringUtils.hasText(properties.getSslTruststoreFilename())) {

      if (!properties.isVerifySsl()) {
        httpClientBuilder.hostnameVerifier(TlsTruststoreUtil.noHostnameCheckVerifier());
      }

      final var trustManagers = TlsTruststoreUtil
          .clientCertificateCheckTrustManagers(
              properties.getSslTruststoreFilename(),
              properties.getSslTruststorePassword());
      final var x509TrustManager = Arrays.stream(trustManagers)
          .filter(manager -> manager instanceof X509TrustManager)
          .map(manager -> (X509TrustManager) manager)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("truststore file '"
              + properties.getSslTruststoreFilename()
              + "' does not contain at least one X509 certificate required for HTTPS clients"));
      final var keyManagers = TlsTruststoreUtil
          .clientCertificateCheckKeyManagers(
              properties.getSslTruststoreFilename(),
              properties.getSslTruststorePassword());
      httpClientBuilder.sslSocketFactory(getSSLSocketFactory(trustManagers, keyManagers), x509TrustManager);


    } else if (!properties.isVerifySsl()) {

      httpClientBuilder.hostnameVerifier(TlsTruststoreUtil.noHostnameCheckVerifier());
      final var trustManager = TlsTruststoreUtil.noCertificateCheckTrustManager();
      httpClientBuilder
          .sslSocketFactory(getSSLSocketFactory(new TrustManager[]{
              trustManager
      }, null), trustManager);

    }

  }

  /**
   * Applies the proxy settings to an OkHttp client and, if wanted, logs each request and response.
   *
   * @param builder     The builder of the OkHttp client to change
   * @param clientClass The class the log messages are written for
   * @param properties  The settings of the client
   * @return The same builder
   */
  protected OkHttpClient.Builder configureProxyAndLogger(
      final OkHttpClient.Builder builder,
      final Class<?> clientClass,
      final Client properties) {

    final var clientLogger = LoggerFactory.getLogger(clientClass);

    if (properties.useProxy()) {
      final var proxy = properties.getProxy();

      builder
          .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));

      if (proxy.getUsername() != null) {
        builder.proxyAuthenticator(new Authenticator() {
          @Override
          public okhttp3.Request authenticate(
              Route route,
              Response response) throws IOException {
            final var credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
            return response.request().newBuilder().header("Proxy-Authorization", credential).build();
          }
        });
      }
    }

    if (properties.isLog()) {
      final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor((
          msg) -> {
        clientLogger.info(msg);
      });
      interceptor.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);
      builder.addInterceptor(interceptor);
    }

    return builder;

  }

  /**
   * Sends an OAuth bearer token with each request, if OAuth is configured. The token is fetched with
   * the client credentials grant.
   *
   * @param builder    The Feign builder to change
   * @param properties The settings of the client
   * @return Whether OAuth is used
   */
  protected boolean configureOauthAuthentication(
      final Builder builder,
      final Client properties) {

    if (properties.getAuthentication() == null) {
      return false;
    }
    if (properties.getAuthentication().getOauth() == null) {
      return false;
    }
    if (!properties.getAuthentication().getOauth().isInitialized()) {
      return false;
    }

    final var tokenHandler = new OauthBearerTokenHandler(properties.getAuthentication());
    builder.requestInterceptor(tokenHandler);
    builder.errorDecoder(tokenHandler);

    return true;

  }

  /**
   * Sends user name and password with each request, if HTTP basic authentication is configured.
   *
   * @param builder    The Feign builder to change
   * @param properties The settings of the client
   * @return Whether basic authentication is used
   */
  protected boolean configureBasicAuthentication(
      final Builder builder,
      final Client properties) {

    if (properties.getAuthentication() == null) {
      return false;
    }
    if (!properties.getAuthentication().isBasic()) {
      return false;
    }

    final var credential = Credentials
        .basic(properties.getAuthentication().getUsername(), properties.getAuthentication().getPassword());
    builder.requestInterceptor((
        template) -> {
      template.header("Authorization", credential);
    });

    return true;

  }

  private SSLSocketFactory getSSLSocketFactory(
      final TrustManager[] trustManagers,
      final KeyManager[] keyManagers) {

    try {
      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(keyManagers, trustManagers, new SecureRandom());
      return sslContext.getSocketFactory();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Finds the properties bean which implements the given interface and belongs to the same
   * workflow module as the class at the injection point.
   * 
   * @param <T>                 The type of the desired properties bean
   * @param injectionPoint      The injection point to identify the workflow
   *                            module
   * @param propertiesInterface The interface the properties bean has to implement
   * @param properties          All properties found at runtime
   * @return The desired properties bean
   */
  protected <T> T getProperties(
      final InjectionPoint injectionPoint,
      final Class<T> propertiesInterface,
      final List<T> properties) {

    final var classPackageName = injectionPoint
        .getMember()
        .getDeclaringClass()
        .getPackageName();

    // find properties having the closest package-name
    return properties
        .stream()
        .map(props -> {
          final var propsPackageName = props.getClass().getPackageName();
          final var minLength = Math.min(classPackageName.length(), propsPackageName.length());
          for (int i = 0; i < minLength; i++) {
            if (classPackageName.charAt(i) != propsPackageName.charAt(i)) {
              return Map.entry(props, i);
            }
          }
          return Map.entry(props, minLength);
        })
        .sorted((
            p1,
            p2) -> 0 - p1.getValue().compareTo(p2.getValue()))
        .findFirst()
        .map(entry -> entry.getKey())
        .orElseThrow(() -> new NoSuchElementException(
            "There is no @ConfigurationProperties implementing '"
                + propertiesInterface.getName()
                + "' in the workflow-module of class '"
                + injectionPoint.getMember().getDeclaringClass().getName()
                + "'!"));

  }

}
