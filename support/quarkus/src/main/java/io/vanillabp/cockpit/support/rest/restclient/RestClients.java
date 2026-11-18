package io.vanillabp.cockpit.support.rest.restclient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import jakarta.inject.Singleton;

/**
 * Builds REST clients of Quarkus' REST client from the keys at a prefix the caller chooses, for
 * example {@code loan-approval.cockpit}. It is the twin of the HTTP service clients of the Spring
 * Boot module, with the same keys and the same defaults:
 * <ul>
 * <li>{@code base-url}, {@code connect-timeout}, {@code read-timeout}, {@code redirects} and
 * {@code default-header.<name>};</li>
 * <li>{@code verify-ssl}, and {@code tls-configuration-name}, which names a configuration of the
 * Quarkus TLS registry where Spring Boot has {@code ssl.bundle};</li>
 * <li>a proxy, with or without login: {@code proxy.*};</li>
 * <li>retries ({@code retry.*}) and logging with bodies ({@code log});</li>
 * <li>{@code connection-pool-size}, how many connections the clients open at most at the same
 * time;</li>
 * <li>a bearer token of the OAuth client credentials grant: {@code oauth.*}.</li>
 * </ul>
 * A workflow module produces each client it needs, so that a missing key stops the start and not
 * the first call:
 *
 * <pre>
 * &#64;Produces
 * &#64;Singleton
 * &#64;Startup
 * InventoryApi inventory(final RestClients restClients) {
 *   return restClients.build(InventoryApi.class, "loan-approval.inventory");
 * }
 * </pre>
 *
 * Two workflow modules of one application may call the same service through the same interface
 * with different keys: each builds its client at a prefix of its own, and tells the two beans apart
 * by a qualifier. What the two share, they write twice or bring in a library of their own.
 * <p>
 * A client module may declare the path of its API as a {@link RestClientApiPath} bean, for example
 * {@code /gui/api/v1}. It is added to {@code base-url} for every client of an interface of that API,
 * so {@code base-url} is the address of the server only. A {@code base-url} which ends with the path
 * stops the start, because each call would hold it twice.
 * <p>
 * All interfaces built at one prefix share its settings and its OAuth token. Every problem of a
 * prefix, a key nobody reads included, ends in one {@link RestClientsMisconfigured} which lists
 * them all.
 * <p>
 * The keys are read through {@link Config} and not by a {@code @ConfigMapping}, which would refuse
 * an unknown key before any guiding message. A prefix below {@code quarkus.},
 * {@code business-cockpit.} or {@code vanillabp.} is refused: Quarkus warns about keys below its
 * own, the check of the login token refuses unknown keys below {@code business-cockpit.}, and the
 * keys of a REST client belong to the module which calls the service.
 */
@Singleton
public class RestClients {

  private static final Logger logger = LoggerFactory.getLogger(RestClients.class);

  private static final Pattern VALID_PREFIX = Pattern.compile("[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*");

  private static final List<String> TAKEN_PREFIXES = List.of("quarkus", "business-cockpit", "vanillabp");

  private final Config config;

  /** The settings of each prefix a client was built at, read once. */
  private final Map<String, Conventions> prefixes = new ConcurrentHashMap<>();

  /**
   * Creates the builder of clients, which reads the keys of the running application. Quarkus calls
   * this constructor, and so may a test or a class which is no bean: the configuration comes from
   * {@link ConfigProvider} and not by injection, so nothing is missing without CDI.
   */
  public RestClients() {
    this.config = ConfigProvider.getConfig();
  }

  /**
   * Builds a client from the keys at a prefix.
   *
   * @param <T>    the type of the client
   * @param api    the client interface, for example one the generator {@code jaxrs-spec} writes
   * @param prefix the key all keys of the client start with, for example
   *               {@code loan-approval.cockpit}
   * @return the client
   * @throws RestClientsMisconfigured if the keys at the prefix cannot work
   */
  public <T> T build(
      final Class<T> api,
      final String prefix) {

    return build(api, prefix, builder -> {
    });

  }

  /**
   * Builds a client from the keys at a prefix, and lets the caller change the builder after the
   * keys are applied, for example to register a filter of its own.
   *
   * @param <T>        the type of the client
   * @param api        the client interface
   * @param prefix     the key all keys of the client start with
   * @param customizer called with the builder before the client is built
   * @return the client
   * @throws RestClientsMisconfigured if the keys at the prefix cannot work
   */
  public <T> T build(
      final Class<T> api,
      final String prefix,
      final Consumer<QuarkusRestClientBuilder> customizer) {

    final var conventions = prefixes.computeIfAbsent(checked(prefix), this::conventionsOf);
    final var builder = conventions.clientBuilder();
    final var apiPath = apiPathOf(api);
    if (apiPath != null) {
      builder.baseUri(URI.create(baseUrlWith(apiPath, conventions.keys, api)));
    }
    customizer.accept(builder);
    final var client = builder.build(api);
    final var retry = conventions.keys.retry;
    return retry.enabled()
        ? Retries.around(api, client, retry, conventions.keys.oauth.isOn())
        : client;

  }

  private static String checked(
      final String prefix) {

    if ((prefix == null) || !VALID_PREFIX.matcher(prefix).matches()) {
      throw new IllegalArgumentException("""
          The prefix '%s' of REST clients is no valid key. Use words joined by dots and dashes, for \
          example 'loan-approval.cockpit'."""
          .formatted(prefix));
    }
    final var first = prefix.split("\\.", 2)[0];
    if (TAKEN_PREFIXES.contains(first)) {
      throw new IllegalArgumentException("""
          The prefix '%s' of REST clients starts with '%s'. The keys of a REST client belong to the \
          module which calls the service, so choose a prefix of the module, for example \
          'loan-approval.cockpit'. Keys below 'quarkus.' are Quarkus' own, unknown keys below \
          'business-cockpit.' stop the start, and nothing reads REST client keys below 'vanillabp.'."""
          .formatted(prefix, first));
    }
    return prefix;

  }

  /**
   * The path of the API the interface belongs to, from the {@link RestClientApiPath} beans, or
   * {@code null} if there is none. Without a running container, in a plain unit test for example,
   * there are no such beans.
   */
  private static String apiPathOf(
      final Class<?> api) {

    final var container = Arc.container();
    if (container == null) {
      return null;
    }
    for (final var apiPath : container.select(RestClientApiPath.class)) {
      if (apiPath.covers(api)) {
        return apiPath.path();
      }
    }
    return null;

  }

  /**
   * The address with the path of the API. A {@code base-url} which ends with the path already would
   * send every call to {@code /gui/api/v1/gui/api/v1/...} and fail only when it is made.
   */
  private static String baseUrlWith(
      final String apiPath,
      final RestClientKeys keys,
      final Class<?> api) {

    final var baseUrl = withoutTrailingSlash(keys.baseUrl.orElseThrow());
    if (withoutTrailingSlash(URI.create(baseUrl).getPath()).endsWith(apiPath)) {
      throw new RestClientsMisconfigured(keys.prefix, List.of("""
          The address '%s' in '%s' ends with '%s', the path of the API which the client %s adds to \
          the address itself. Set '%s' to the address without it: '%s'."""
          .formatted(keys.baseUrl.get(), keys.key("base-url"), apiPath, api.getSimpleName(), keys.key(
              "base-url"), baseUrl.substring(0, baseUrl.length() - apiPath.length()))));
    }
    return baseUrl + apiPath;

  }

  private static String withoutTrailingSlash(
      final String text) {

    var result = text;
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;

  }

  private Conventions conventionsOf(
      final String prefix) {

    final var problems = new ArrayList<>(UnreadKeys.below(config, prefix));
    final var keys = RestClientKeys.read(config, prefix, problems);
    final var tls = tlsConfigurationOf(keys, problems);
    if (!problems.isEmpty()) {
      throw new RestClientsMisconfigured(prefix, problems);
    }
    if (!keys.verifySsl) {
      logger.warn("""
          The REST clients at '{}' trust every server, because '{}' is false. Use this for test \
          systems only.""",
          prefix, keys.key("verify-ssl"));
    }
    return new Conventions(keys, tls);

  }

  private static Optional<TlsConfiguration> tlsConfigurationOf(
      final RestClientKeys keys,
      final List<String> problems) {

    if (keys.tlsConfigurationName.isEmpty()) {
      return Optional.empty();
    }
    final var name = keys.tlsConfigurationName.get();
    final var registry = Arc.container().select(TlsConfigurationRegistry.class);
    final var configuration = registry.isResolvable() ? registry.get().get(name) : Optional.<TlsConfiguration>empty();
    if (configuration.isEmpty()) {
      problems.add("""
          The REST clients at '%s' name the TLS configuration '%s' in '%s', but there is no such \
          configuration. Add it below 'quarkus.tls.%s', for example \
          'quarkus.tls.%s.trust-store.pem.certs'."""
          .formatted(keys.prefix, name, keys.key("tls-configuration-name"), name, name));
    }
    return configuration;

  }

  /** What the keys at one prefix say, applied to each new builder. */
  private static final class Conventions {

    private final RestClientKeys keys;

    private final Optional<TlsConfiguration> tls;

    private final ClientCredentials clientCredentials;

    Conventions(
        final RestClientKeys keys,
        final Optional<TlsConfiguration> tls) {

      this.keys = keys;
      this.tls = tls;
      this.clientCredentials = keys.oauth.isOn()
          ? new ClientCredentials(keys.prefix, keys.oauth, connection()
              .baseUri(URI.create(keys.oauth.tokenUri().orElseThrow()))
              .disableDefaultMapper(true)
              .build(TokenEndpoint.class))
          : null;

    }

    /** A builder with the keys of the prefix, for a client of the application. */
    QuarkusRestClientBuilder clientBuilder() {

      final var builder = connection()
          .baseUri(URI.create(keys.baseUrl.orElseThrow()))
          .followRedirects(keys.followRedirects);
      if (!keys.defaultHeaders.isEmpty()) {
        builder.register(new DefaultHeaders(keys.defaultHeaders));
      }
      if (clientCredentials != null) {
        builder.register(clientCredentials);
      }
      if (keys.log) {
        builder
            .loggingScope(LoggingScope.REQUEST_RESPONSE)
            .clientLogger(new ExchangeLog(keys.prefix));
      }
      keys.connectionPoolSize
          .ifPresent(size -> builder.property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, size));
      return builder;

    }

    /**
     * A builder with the timeouts, the TLS settings and the proxy of the prefix. The token
     * requests use them as well.
     */
    private QuarkusRestClientBuilder connection() {

      final var builder = QuarkusRestClientBuilder
          .newBuilder()
          .connectTimeout(keys.connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
          .readTimeout(keys.readTimeout.toMillis(), TimeUnit.MILLISECONDS);
      tls.ifPresent(builder::tlsConfiguration);
      if (!keys.verifySsl) {
        builder.trustAll(true).verifyHost(false);
      }
      final var proxy = keys.proxy;
      if (proxy.host().isPresent()) {
        builder.proxyAddress(proxy.host().get(), proxy.port().orElseThrow());
        proxy.username().ifPresent(username -> builder
            .proxyUser(username)
            .proxyPassword(proxy.password().orElse("")));
      }
      return builder;

    }

  }

}
