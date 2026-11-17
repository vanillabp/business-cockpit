package io.vanillabp.cockpit.support.rest.restclient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;

/**
 * All keys of the REST clients at one prefix, for example {@code loan-approval.cockpit}. They are
 * the keys of the HTTP service clients of the Spring Boot module, with the same names and the same
 * defaults, so a workflow module moves between the platforms without renaming a key. Two differ
 * because the platforms differ: {@code tls-configuration-name} names a configuration of the Quarkus
 * TLS registry where Spring Boot has {@code ssl.bundle}, and Spring Boot's {@code cookie-handling}
 * and {@code apiversion.*} have no counterpart.
 * <p>
 * The keys are read one by one through {@link Config}, not by a {@code @ConfigMapping}. A mapping
 * makes Quarkus refuse every other key below its prefix before any bean starts, with a message
 * which does not say what was meant. Read this way, {@link UnreadKeys} can name the key a typo was
 * meant to be.
 * <p>
 * Every key which cannot be read and every missing key becomes one problem in the list the reader
 * collects, so the developer sees all of them after one start.
 */
final class RestClientKeys {

  /** The timeouts of the Spring Boot module, which took them from {@code ClientsConfigurationBase}. */
  static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMillis(1500);

  static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

  static final int DEFAULT_MAX_ATTEMPTS = 5;

  static final Duration DEFAULT_PERIOD = Duration.ofMillis(100);

  static final Duration DEFAULT_MAX_PERIOD = Duration.ofSeconds(1);

  /** Client id and secret go into the form of the token request. */
  static final String CLIENT_SECRET_POST = "client_secret_post";

  /** Client id and secret go into an HTTP basic authentication header of the token request. */
  static final String CLIENT_SECRET_BASIC = "client_secret_basic";

  static final Duration DEFAULT_RENEW_BEFORE = Duration.ofSeconds(30);

  static final Duration DEFAULT_LIFETIME_WITHOUT_EXPIRES_IN = Duration.ofHours(1);

  /** Spring Boot's values of {@code redirects}, the first two follow a redirect. */
  private static final List<String> REDIRECTS = List.of("follow-when-possible", "follow", "dont-follow");

  final String prefix;

  final Optional<String> baseUrl;

  final Duration connectTimeout;

  final Duration readTimeout;

  final boolean followRedirects;

  final boolean verifySsl;

  final Optional<String> tlsConfigurationName;

  final Map<String, String> defaultHeaders;

  final boolean log;

  final Optional<Integer> connectionPoolSize;

  final Proxy proxy;

  final Retry retry;

  final OAuth oauth;

  /** The settings of an HTTP proxy. Without a host no proxy is used. */
  record Proxy(
               Optional<String> host,
               Optional<Integer> port,
               Optional<String> username,
               Optional<String> password) {
  }

  /** The retries. Off by default. */
  record Retry(
               boolean enabled,
               int maxAttempts,
               Duration period,
               Duration maxPeriod) {
  }

  /** The bearer token of the OAuth client credentials grant. {@code token-uri} switches it on. */
  record OAuth(
               Optional<String> tokenUri,
               Optional<String> clientId,
               Optional<String> clientSecret,
               String clientAuthenticationMethod,
               List<String> scope,
               Duration renewBefore,
               Duration lifetimeWithoutExpiresIn) {

    boolean isOn() {
      return tokenUri.isPresent();
    }

  }

  private final Config config;

  private final List<String> problems;

  private RestClientKeys(
      final Config config,
      final String prefix,
      final List<String> problems) {

    this.config = config;
    this.prefix = prefix;
    this.problems = problems;

    baseUrl = text("base-url");
    connectTimeout = duration("connect-timeout", DEFAULT_CONNECT_TIMEOUT);
    readTimeout = duration("read-timeout", DEFAULT_READ_TIMEOUT);
    followRedirects = redirects();
    verifySsl = value("verify-ssl", Boolean.class).orElse(true);
    tlsConfigurationName = text("tls-configuration-name");
    defaultHeaders = defaultHeaders();
    log = value("log", Boolean.class).orElse(false);
    connectionPoolSize = value("connection-pool-size", Integer.class);
    proxy = new Proxy(text("proxy.host"), value("proxy.port",
        Integer.class), text("proxy.username"), text("proxy.password"));
    retry = new Retry(
        value("retry.enabled", Boolean.class).orElse(false), value("retry.max-attempts", Integer.class)
            .orElse(DEFAULT_MAX_ATTEMPTS), duration("retry.period",
                DEFAULT_PERIOD), duration("retry.max-period", DEFAULT_MAX_PERIOD));
    oauth = new OAuth(
        text("oauth.token-uri"), text("oauth.client-id"), text(
            "oauth.client-secret"), text("oauth.client-authentication-method").orElse(CLIENT_SECRET_POST), values(
                "oauth.scope"), duration("oauth.renew-before", DEFAULT_RENEW_BEFORE), duration(
                    "oauth.lifetime-without-expires-in", DEFAULT_LIFETIME_WITHOUT_EXPIRES_IN));

  }

  /**
   * Reads the keys at a prefix and checks them. What is wrong goes into {@code problems}.
   *
   * @param config   where the keys are read from
   * @param prefix   the key all keys of the clients start with
   * @param problems where the problems are collected
   * @return the keys, with defaults for those which could not be read
   */
  static RestClientKeys read(
      final Config config,
      final String prefix,
      final List<String> problems) {

    final var keys = new RestClientKeys(config, prefix, problems);
    keys.checkBaseUrl();
    keys.checkProxy();
    keys.checkRetry();
    keys.checkConnectionPool();
    if (keys.oauth.isOn()) {
      keys.checkOAuth();
    }
    return keys;

  }

  /** The full name of a key. */
  String key(
      final String name) {

    return prefix
        + "."
        + name;

  }

  private Optional<String> text(
      final String name) {

    return value(name, String.class).filter(text -> !text.isBlank());

  }

  private <T> Optional<T> value(
      final String name,
      final Class<T> type) {

    return readOrProblem(name, () -> config.getOptionalValue(key(name), type)).flatMap(value -> value);

  }

  private List<String> values(
      final String name) {

    return readOrProblem(name, () -> config.getOptionalValues(key(name), String.class))
        .flatMap(value -> value)
        .orElse(List.of());

  }

  private Duration duration(
      final String name,
      final Duration defaultValue) {

    return value(name, Duration.class).orElse(defaultValue);

  }

  private <T> Optional<T> readOrProblem(
      final String name,
      final Supplier<T> read) {

    try {
      return Optional.ofNullable(read.get());
    } catch (IllegalArgumentException wrongValue) {
      problems.add("""
          The value of '%s' cannot be read: %s"""
          .formatted(key(name), wrongValue.getMessage()));
    }
    return Optional.empty();

  }

  private boolean redirects() {

    final var redirects = text("redirects");
    if (redirects.isEmpty()) {
      return true;
    }
    if (!REDIRECTS.contains(redirects.get())) {
      problems.add("""
          '%s' is '%s'. Set one of %s."""
          .formatted(key("redirects"), redirects.get(), String.join(", ", REDIRECTS)));
      return true;
    }
    return !"dont-follow".equals(redirects.get());

  }

  private Map<String, String> defaultHeaders() {

    final var headers = new TreeMap<String, String>();
    final var start = key("default-header.");
    for (final var propertyName : config.getPropertyNames()) {
      if (propertyName.startsWith(start)) {
        final var header = propertyName.substring(start.length());
        text("default-header."
            + header).ifPresent(value -> headers.put(header, value));
      }
    }
    return headers;

  }

  private void checkBaseUrl() {

    if (baseUrl.isEmpty()) {
      final var placeholder = unresolvedPlaceholderOf("base-url");
      if (placeholder != null) {
        problems.add("""
            The address of the REST clients at '%s' holds a placeholder nothing resolves. Set the \
            key or environment variable '%s', or write the address into '%s'."""
            .formatted(prefix, placeholder, key("base-url")));
      } else if (problems.stream().noneMatch(problem -> problem.contains("'"
          + key("base-url")
          + "'"))) {
            problems.add("""
                The REST clients at '%s' have no address. Set '%s' to where the service answers, for \
                example 'https://inventory.example.com/api'."""
                .formatted(prefix, key("base-url")));
          }
      return;
    }
    if (!isHttpAddress(baseUrl.get())) {
      problems.add("""
          The address '%s' in '%s' is no complete http or https address. Write it with scheme and \
          host, for example 'https://inventory.example.com/api'."""
          .formatted(baseUrl.get(), key("base-url")));
    }

  }

  /**
   * The name in the first placeholder of a key's value, if Quarkus could not resolve it. Quarkus
   * then treats the key as missing, which would hide what the developer has to set.
   */
  private String unresolvedPlaceholderOf(
      final String name) {

    final var raw = config.getConfigValue(key(name)).getRawValue();
    if ((raw == null) || !raw.contains("${")) {
      return null;
    }
    final var start = raw.indexOf("${") + 2;
    final var end = raw.indexOf('}', start);
    final var placeholder = end < 0 ? raw.substring(start) : raw.substring(start, end);
    return placeholder.split(":", 2)[0];

  }

  private static boolean isHttpAddress(
      final String address) {

    try {
      final var uri = URI.create(address);
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && (uri
          .getHost() != null);
    } catch (IllegalArgumentException e) {
      return false;
    }

  }

  private void checkProxy() {

    if (proxy.host().isEmpty()) {
      if (proxy.port().isPresent() || proxy.username().isPresent()) {
        problems.add("""
            The REST clients at '%s' have proxy keys, but no '%s'. Set it, or remove the other keys \
            below '%s'."""
            .formatted(prefix, key("proxy.host"), key("proxy")));
      }
      return;
    }
    if (proxy.port().isEmpty()) {
      problems.add("""
          The proxy '%s' of the REST clients at '%s' has no port. Set '%s'."""
          .formatted(proxy.host().get(), prefix, key("proxy.port")));
    }

  }

  private void checkRetry() {

    if (retry.enabled() && (retry.maxAttempts() < 1)) {
      problems.add("""
          The REST clients at '%s' have '%s: %d'. Set at least 1, which sends each request once."""
          .formatted(prefix, key("retry.max-attempts"), retry.maxAttempts()));
    }

  }

  private void checkConnectionPool() {

    if (connectionPoolSize.isPresent() && (connectionPoolSize.get() < 1)) {
      problems.add("""
          The REST clients at '%s' have '%s: %d'. Set at least 1, or remove the key to keep the \
          default of Quarkus, 50 connections."""
          .formatted(prefix, key("connection-pool-size"), connectionPoolSize.get()));
    }

  }

  private void checkOAuth() {

    final var tokenUri = oauth.tokenUri().orElseThrow();
    if (!isHttpAddress(tokenUri)) {
      problems.add("""
          The address '%s' in '%s' is no complete http or https address. Write the token endpoint \
          of the OAuth server with scheme and host, for example 'https://login.example.com/oauth/token'."""
          .formatted(tokenUri, key("oauth.token-uri")));
    }
    if (oauth.clientId().isEmpty()) {
      problems.add("""
          The REST clients at '%s' have an OAuth token endpoint, but no client id. Set '%s'."""
          .formatted(prefix, key("oauth.client-id")));
    }
    if (oauth.clientSecret().isEmpty()) {
      problems.add("""
          The REST clients at '%s' have an OAuth token endpoint, but no client secret. Set '%s'."""
          .formatted(prefix, key("oauth.client-secret")));
    }
    final var method = oauth.clientAuthenticationMethod();
    if (!CLIENT_SECRET_POST.equals(method) && !CLIENT_SECRET_BASIC.equals(method)) {
      problems.add("""
          The REST clients at '%s' have '%s: %s'. Set '%s' to send client id and secret in the \
          form, or '%s' to send them in an HTTP basic authentication header."""
          .formatted(prefix, key("oauth.client-authentication-method"), method, CLIENT_SECRET_POST,
              CLIENT_SECRET_BASIC));
    }
    if (oauth.renewBefore().isNegative() || oauth.lifetimeWithoutExpiresIn().isNegative() || oauth
        .lifetimeWithoutExpiresIn().isZero()) {
      problems.add("""
          The REST clients at '%s' have a negative '%s' or no positive '%s'. Remove them to use the \
          defaults of 30 seconds and one hour."""
          .formatted(prefix, key("oauth.renew-before"), key("oauth.lifetime-without-expires-in")));
    }

  }

}
