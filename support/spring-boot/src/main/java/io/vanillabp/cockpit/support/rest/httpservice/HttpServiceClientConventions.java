package io.vanillabp.cockpit.support.rest.httpservice;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ApiversionProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientSettingsPropertyMapper;
import org.springframework.boot.http.client.autoconfigure.PropertiesApiVersionInserter;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * Configures every HTTP service group of the application from the keys below the group's prefix
 * (see {@link HttpServiceGroupPrefix}), and adds what Spring Boot has no key for:
 * <ul>
 * <li>A missing or broken {@code base-url} stops the start and names the key. Spring Boot would
 * build the client and fail only at the first request.</li>
 * <li>A proxy, with or without login: {@code proxy.*}.</li>
 * <li>{@code verify-ssl: false}, which trusts every server.</li>
 * <li>Retries ({@code retry.*}) and logging with bodies ({@code log}).</li>
 * <li>The number of connections to the server at the same time: {@code connection-pool-size}.</li>
 * <li>A bearer token of the OAuth client credentials grant: {@code oauth.*}.</li>
 * <li>A key below the prefix which no setting reads stops the start, so a typo does not go
 * unnoticed.</li>
 * </ul>
 * It runs right after Spring Boot's own configurer, which applies
 * {@code spring.http.serviceclient.<group>.*}, and before {@code RestClientCustomizer} beans and
 * the application's own configurers, so those can still change everything.
 * <p>
 * Every problem of every group is collected first. Then one {@link HttpServiceClientsMisconfigured}
 * lists them all, so a developer sees every missing key after one restart.
 */
public class HttpServiceClientConventions implements RestClientHttpServiceGroupConfigurer {

  /** Spring Boot's properties configurer has HIGHEST_PRECEDENCE + 10, its customizer configurer 0. */
  public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

  /**
   * The timeouts of {@code ClientsConfigurationBase}, for a group which sets none, neither itself
   * nor for all clients in {@code spring.http.clients.*}. Without them the JDK's HttpClient waits
   * without end.
   */
  static final HttpClientSettings DEFAULT_SETTINGS = HttpClientSettings.defaults()
      .withTimeouts(Duration.ofMillis(1500), Duration.ofSeconds(10));

  private static final Logger logger = LoggerFactory.getLogger(HttpServiceClientConventions.class);

  private static final String OAUTH_CLASS = "org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor";

  private final Environment environment;

  private final @Nullable SslBundles sslBundles;

  private final HttpClientSettingsPropertyMapper settingsMapper;

  private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

  private final Map<String, String> prefixes;

  /**
   * Creates the conventions. Two prefixes for one group stop the start here already.
   *
   * @param environment           where the keys are read from
   * @param sslBundles            the SSL bundles of the application, or {@code null} if it has none
   * @param defaultSettings       the settings for all clients, from {@code spring.http.clients.*}
   * @param requestFactoryBuilder Spring Boot's, so its {@code ClientHttpRequestFactoryBuilderCustomizer}
   *                              beans apply. {@code null} builds with the JDK's HttpClient.
   * @param prefixes              where the keys of the groups live which do not use Spring Boot's
   *                              place
   */
  public HttpServiceClientConventions(
      final Environment environment,
      final @Nullable SslBundles sslBundles,
      final @Nullable HttpClientSettings defaultSettings,
      final @Nullable ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
      final List<HttpServiceGroupPrefix> prefixes) {

    this.environment = environment;
    this.sslBundles = sslBundles;
    this.settingsMapper = new HttpClientSettingsPropertyMapper(sslBundles, defaultSettings == null ? DEFAULT_SETTINGS
        : defaultSettings.orElse(DEFAULT_SETTINGS));
    this.requestFactoryBuilder = requestFactoryBuilder == null
        ? ClientHttpRequestFactoryBuilder.jdk()
        : requestFactoryBuilder;
    this.prefixes = new HashMap<>();
    final var declaredTwice = new TreeSet<String>();
    prefixes.forEach(prefix -> {
      final var earlier = this.prefixes.put(prefix.group(), prefix.prefix());
      if ((earlier != null) && !earlier.equals(prefix.prefix())) {
        declaredTwice.add("""
            The HTTP service group '%s' has two prefixes, '%s' and '%s'. Keep one of the two \
            HttpServiceGroupPrefix beans. To give two workflow modules different keys for the same \
            API, import the API into one group per module, each with a prefix of its own."""
            .formatted(prefix.group(), earlier, prefix.prefix()));
      }
    });
    if (!declaredTwice.isEmpty()) {
      throw new HttpServiceClientsMisconfigured(List.copyOf(declaredTwice));
    }

  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void configureGroups(
      final Groups<RestClient.Builder> groups) {

    final var problems = new ArrayList<String>();
    final var configured = new HashSet<String>();
    groups.forEachClient((
        group,
        builder) -> {
      configured.add(group.name());
      problems.addAll(configure(group, builder));
    });

    final var unknownGroups = new TreeSet<>(prefixes.keySet());
    unknownGroups.removeAll(configured);
    unknownGroups.forEach(group -> problems.add("""
        An HttpServiceGroupPrefix names the HTTP service group '%s', but there is no such group. \
        The groups are %s. Check the name in '@ImportHttpServices(group = ...)'."""
        .formatted(group, new TreeSet<>(configured))));

    if (!problems.isEmpty()) {
      throw new HttpServiceClientsMisconfigured(problems);
    }

  }

  /** The prefix of the group's keys. */
  String prefixOf(
      final String groupName) {

    return prefixes.getOrDefault(groupName, HttpServiceGroupPrefix.springBootPrefixOf(groupName));

  }

  /** Configures one group and answers its problems, or nothing if there are none. */
  private List<String> configure(
      final HttpServiceGroup group,
      final RestClient.Builder builder) {

    final var name = group.name();
    final var prefix = prefixOf(name);
    final var problems = new ArrayList<String>();

    UnreadKeys.below(environment, prefix).forEach(key -> problems.add("""
        The key '%s' of the HTTP service group '%s' is read by nobody. Check its spelling. The \
        support README lists every key."""
        .formatted(key, name)));
    final var springBootPrefix = HttpServiceGroupPrefix.springBootPrefixOf(name);
    if (!prefix.equals(springBootPrefix) && UnreadKeys.anyBelow(environment, springBootPrefix)) {
      problems.add("""
          The HTTP service group '%s' reads its keys below '%s', but there are keys below '%s' as \
          well. Move them below '%s'."""
          .formatted(name, prefix, springBootPrefix, prefix));
    }

    final HttpServiceGroupProperties keys;
    try {
      keys = Binder.get(environment)
          .bind(prefix, Bindable.of(HttpServiceGroupProperties.class))
          .orElseGet(HttpServiceGroupProperties::new);
    } catch (RuntimeException e) {
      problems.add("""
          The keys of the HTTP service group '%s' below '%s' cannot be read: %s"""
          .formatted(name, prefix, messagesOf(e)));
      return problems;
    }

    checkBaseUrl(name, prefix, keys, problems);
    checkProxy(name, prefix, keys, problems);
    checkRetry(name, prefix, keys, problems);
    final var useOAuth = keys.getOauth().getTokenUri() != null;
    if (useOAuth) {
      checkOAuth(name, prefix, keys, problems);
    }
    final var requestFactory = requestFactory(name, prefix, keys, problems);
    if (!problems.isEmpty()) {
      return problems;
    }

    builder.requestFactory(requestFactory);
    builder.baseUrl(keys.getBaseUrl());
    if (!keys.getDefaultHeader().isEmpty()) {
      builder.defaultHeaders(headers -> headers.putAll(keys.getDefaultHeader()));
    }
    final var apiVersion = keys.getApiversion();
    if (apiVersion.getDefaultVersion() != null) {
      builder.defaultApiVersion(apiVersion.getDefaultVersion());
    }
    if (hasApiVersionInsert(apiVersion.getInsert())) {
      builder.apiVersionInserter(PropertiesApiVersionInserter.get(apiVersion.getInsert()));
    }

    // The first interceptor is the outermost: a retry runs OAuth and logging again.
    final var retry = keys.getRetry();
    if (retry.isEnabled()) {
      builder.requestInterceptor(
          new RetryInterceptor(retry.getMaxAttempts(), retry.getPeriod(), retry.getMaxPeriod(), useOAuth));
    }
    final var proxyLogin = proxyLoginOf(keys.getProxy());
    if (proxyLogin != null) {
      builder.requestInterceptor(proxyLogin);
    }
    if (useOAuth) {
      builder.requestInterceptor(
          ClientCredentials.interceptor(name, prefix, keys.getOauth(), requestFactory, proxyLogin));
    }
    if (keys.isLog()) {
      builder.bufferContent((
          uri,
          method) -> true);
      builder.requestInterceptor(new LoggingInterceptor(name));
    }
    return problems;

  }

  private static void checkBaseUrl(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    final var baseUrl = keys.getBaseUrl();
    if ((baseUrl == null) || baseUrl.isBlank()) {
      problems.add("""
          The HTTP service group '%s' has no address. Set '%s.base-url' to where the service \
          answers, for example 'https://inventory.example.com/api'."""
          .formatted(name, prefix));
      return;
    }
    final var placeholder = baseUrl.indexOf("${");
    if (placeholder >= 0) {
      final var end = baseUrl.indexOf('}', placeholder);
      final var unresolved = end < 0 ? baseUrl.substring(placeholder + 2) : baseUrl.substring(placeholder + 2, end);
      problems.add("""
          The address '%s' of the HTTP service group '%s' holds a placeholder nothing resolves. Set \
          the key or environment variable '%s', or write the address into '%s.base-url'."""
          .formatted(baseUrl, name, unresolved.split(":", 2)[0], prefix));
      return;
    }
    try {
      final var uri = URI.create(baseUrl);
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()) || (uri
          .getHost() == null)) {
        throw new IllegalArgumentException("no http or https address");
      }
    } catch (IllegalArgumentException e) {
      problems.add("""
          The address '%s' in '%s.base-url' is no complete http or https address. Write it with \
          scheme and host, for example 'https://inventory.example.com/api'."""
          .formatted(baseUrl, prefix));
    }

  }

  private static void checkProxy(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    final var proxy = keys.getProxy();
    if (proxy.getHost() == null) {
      if ((proxy.getPort() != null) || (proxy.getUsername() != null)) {
        problems.add("""
            The HTTP service group '%s' has proxy keys, but no '%s.proxy.host'. Set it, or remove \
            the other keys below '%s.proxy'."""
            .formatted(name, prefix, prefix));
      }
      return;
    }
    if (proxy.getPort() == null) {
      problems.add("""
          The proxy '%s' of the HTTP service group '%s' has no port. Set '%s.proxy.port'."""
          .formatted(proxy.getHost(), name, prefix));
    }

  }

  private static void checkRetry(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    final var retry = keys.getRetry();
    if (retry.isEnabled() && (retry.getMaxAttempts() < 1)) {
      problems.add("""
          The HTTP service group '%s' has '%s.retry.max-attempts: %d'. Set at least 1, which \
          sends each request once."""
          .formatted(name, prefix, retry.getMaxAttempts()));
    }

  }

  private static void checkOAuth(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    if (!ClassUtils.isPresent(OAUTH_CLASS, HttpServiceClientConventions.class.getClassLoader())) {
      problems.add("""
          The HTTP service group '%s' has '%s.oauth.token-uri', but Spring Security's OAuth client \
          is missing. Add the dependency 'org.springframework.security:spring-security-oauth2-client'."""
          .formatted(name, prefix));
      return;
    }
    final var oauth = keys.getOauth();
    if ((oauth.getClientId() == null) || oauth.getClientId().isBlank()) {
      problems.add("""
          The HTTP service group '%s' has an OAuth token endpoint, but no client id. Set \
          '%s.oauth.client-id'."""
          .formatted(name, prefix));
    }
    if ((oauth.getClientSecret() == null) || oauth.getClientSecret().isBlank()) {
      problems.add("""
          The HTTP service group '%s' has an OAuth token endpoint, but no client secret. Set \
          '%s.oauth.client-secret'."""
          .formatted(name, prefix));
    }
    final var method = oauth.getClientAuthenticationMethod();
    if (!HttpServiceGroupProperties.OAuth.CLIENT_SECRET_POST
        .equals(method) && !HttpServiceGroupProperties.OAuth.CLIENT_SECRET_BASIC.equals(method)) {
      problems.add("""
          The HTTP service group '%s' has '%s.oauth.client-authentication-method: %s'. Set '%s' \
          to send client id and secret in the form, or '%s' to send them in an HTTP basic \
          authentication header."""
          .formatted(name, prefix, method, HttpServiceGroupProperties.OAuth.CLIENT_SECRET_POST,
              HttpServiceGroupProperties.OAuth.CLIENT_SECRET_BASIC));
    }
    if (oauth.getRenewBefore().isNegative() || oauth.getLifetimeWithoutExpiresIn().isNegative() || oauth
        .getLifetimeWithoutExpiresIn().isZero()) {
      problems.add("""
          The HTTP service group '%s' has a negative '%s.oauth.renew-before' or no positive \
          '%s.oauth.lifetime-without-expires-in'. Remove them to use the defaults of 30 seconds \
          and one hour."""
          .formatted(name, prefix, prefix));
    }

  }

  /**
   * Spring Boot builds one request factory per group from its keys, but it reads them only below
   * {@code spring.http.serviceclient}, and it has no proxy, no {@code verify-ssl} and no size of the
   * connection pool. So the factory is built again here: from the group's keys, with the proxy and
   * the pool. The login for the proxy is added by {@link ProxyLogin}.
   */
  private @Nullable ClientHttpRequestFactory requestFactory(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    final var ownBundle = keys.getSsl().getBundle();
    final HttpClientSettings settings;
    try {
      settings = keys.isVerifySsl()
          ? settingsMapper.map(keys)
          : settingsMapper.map(keys).withSslBundle(TrustEveryServer.bundle(bundleOrNull(ownBundle)));
    } catch (NoSuchSslBundleException | IllegalStateException e) {
      problems.add("""
          The HTTP service group '%s' names the SSL bundle '%s' in '%s.ssl.bundle', but there is no \
          such bundle. Add it below 'spring.ssl.bundle.jks.%s' or 'spring.ssl.bundle.pem.%s'."""
          .formatted(name, ownBundle, prefix, ownBundle, ownBundle));
      return null;
    }
    if (!keys.isVerifySsl()) {
      logger.warn("""
          The HTTP service group '{}' trusts every server, because '{}.verify-ssl' is false. Use \
          this for test systems only.""",
          name, prefix);
    }

    final var builder = withConnectionPool(name, prefix, keys, problems);
    if (builder == null) {
      return null;
    }
    final var proxy = keys.getProxy();
    if (proxy.getHost() == null) {
      return builder.build(settings);
    }
    if (!(builder instanceof JdkClientHttpRequestFactoryBuilder jdkBuilder)) {
      problems.add("""
          The HTTP service group '%s' has a proxy, which works with the JDK's HttpClient only, but \
          the application sends with %s. Set 'spring.http.clients.imperative.factory: jdk'."""
          .formatted(name, builder.getClass().getSimpleName()));
      return null;
    }
    if (proxy.getPort() == null) {
      return null;
    }
    return jdkBuilder
        .withProxySelector(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())))
        .build(settings);

  }

  /**
   * The builder of the request factory, with the size of the connection pool if the group sets one.
   * The JDK's HttpClient opens as many connections as there are requests, so it needs no size.
   * Apache HttpClient 5 gets the size for each server and in total. The other HTTP libraries are
   * refused, so a key never seems to work while it does nothing.
   */
  private @Nullable ClientHttpRequestFactoryBuilder<?> withConnectionPool(
      final String name,
      final String prefix,
      final HttpServiceGroupProperties keys,
      final List<String> problems) {

    final var poolSize = keys.getConnectionPoolSize();
    if (poolSize == null) {
      return requestFactoryBuilder;
    }
    if (poolSize < 1) {
      problems.add("""
          The HTTP service group '%s' has '%s.connection-pool-size: %d'. Set at least 1, or remove the \
          key to keep the default of the HTTP library."""
          .formatted(name, prefix, poolSize));
      return null;
    }
    if (requestFactoryBuilder instanceof JdkClientHttpRequestFactoryBuilder) {
      return requestFactoryBuilder;
    }
    if (requestFactoryBuilder instanceof HttpComponentsClientHttpRequestFactoryBuilder httpComponents) {
      return HttpComponentsConnectionPool.ofSize(httpComponents, poolSize);
    }
    problems.add("""
        The HTTP service group '%s' has '%s.connection-pool-size', which works with the JDK's \
        HttpClient and Apache HttpClient 5 only, but the application sends with %s. Set \
        'spring.http.clients.imperative.factory' to 'jdk' or 'http-components', or remove the key."""
        .formatted(name, prefix, requestFactoryBuilder.getClass().getSimpleName()));
    return null;

  }

  private static @Nullable ProxyLogin proxyLoginOf(
      final HttpServiceGroupProperties.Proxy proxy) {

    if ((proxy.getHost() == null) || (proxy.getUsername() == null)) {
      return null;
    }
    return new ProxyLogin(proxy.getUsername(), proxy.getPassword() == null ? "" : proxy.getPassword());

  }

  private @Nullable SslBundle bundleOrNull(
      final @Nullable String bundleName) {

    if (bundleName == null) {
      return null;
    }
    if (sslBundles == null) {
      throw new IllegalStateException("no SSL bundles");
    }
    return sslBundles.getBundle(bundleName);

  }

  private static boolean hasApiVersionInsert(
      final ApiversionProperties.Insert insert) {

    return (insert.getHeader() != null) || (insert.getQueryParameter() != null) || (insert
        .getPathSegment() != null) || (insert.getMediaTypeParameter() != null);

  }

  private static String messagesOf(
      final Throwable e) {

    final var messages = new ArrayList<String>();
    final Set<Throwable> seen = new HashSet<>();
    for (Throwable cause = e; (cause != null) && seen.add(cause); cause = cause.getCause()) {
      if (cause.getMessage() != null) {
        messages.add(cause.getMessage());
      }
    }
    return String.join(": ", messages);

  }

}
