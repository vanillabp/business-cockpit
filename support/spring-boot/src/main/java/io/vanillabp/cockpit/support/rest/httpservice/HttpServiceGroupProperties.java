package io.vanillabp.cockpit.support.rest.httpservice;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;

/**
 * All keys of one HTTP service group, below the prefix of the group (see
 * {@link HttpServiceGroupPrefix}). The keys of the superclass are Spring Boot's own:
 * {@code base-url}, {@code connect-timeout}, {@code read-timeout}, {@code redirects},
 * {@code cookie-handling}, {@code ssl.bundle}, {@code default-header.*} and {@code apiversion.*}.
 * This class adds the settings Spring Boot has no key for.
 */
public class HttpServiceGroupProperties extends HttpClientProperties {

  private final Proxy proxy = new Proxy();

  private boolean log;

  private boolean verifySsl = true;

  private @Nullable Integer connectionPoolSize;

  private final Retry retry = new Retry();

  private final OAuth oauth = new OAuth();

  /**
   * Creates the settings of a group without keys: no proxy, no logging, no retry, no OAuth, and
   * the server's certificate is checked.
   */
  public HttpServiceGroupProperties() {
  }

  /**
   * The HTTP proxy of the group, the keys {@code proxy.*}.
   *
   * @return The proxy settings, never {@code null}
   */
  public Proxy getProxy() {
    return proxy;
  }

  /**
   * Whether each request and its answer are logged with headers and bodies at level INFO, the key
   * {@code log}.
   *
   * @return {@code true} to log, {@code false} by default
   */
  public boolean isLog() {
    return log;
  }

  /**
   * Switches the logging of requests and answers on or off.
   *
   * @param log {@code true} to log
   */
  public void setLog(
      final boolean log) {
    this.log = log;
  }

  /**
   * Whether the server's certificate and host name are checked, the key {@code verify-ssl}.
   * {@code false} trusts every server, for test systems with a self-signed certificate.
   *
   * @return {@code true} by default
   */
  public boolean isVerifySsl() {
    return verifySsl;
  }

  /**
   * Switches the check of the server's certificate on or off.
   *
   * @param verifySsl {@code false} to trust every server
   */
  public void setVerifySsl(
      final boolean verifySsl) {
    this.verifySsl = verifySsl;
  }

  /**
   * How many connections to the server the group opens at most at the same time, the key
   * {@code connection-pool-size}. Apache HttpClient 5 opens five by default and lets further
   * requests wait. The JDK's HttpClient, which sends the requests unless the application has another
   * HTTP library, has no such limit, so the key changes nothing there.
   *
   * @return The number of connections, or {@code null} to keep the default of the HTTP library
   */
  public @Nullable Integer getConnectionPoolSize() {
    return connectionPoolSize;
  }

  /**
   * Sets how many connections to the server the group opens at most at the same time.
   *
   * @param connectionPoolSize The number of connections, at least 1
   */
  public void setConnectionPoolSize(
      final @Nullable Integer connectionPoolSize) {
    this.connectionPoolSize = connectionPoolSize;
  }

  /**
   * The retries of the group, the keys {@code retry.*}.
   *
   * @return The retry settings, never {@code null}
   */
  public Retry getRetry() {
    return retry;
  }

  /**
   * The bearer token of the OAuth client credentials grant, the keys {@code oauth.*}.
   *
   * @return The OAuth settings, never {@code null}
   */
  public OAuth getOauth() {
    return oauth;
  }

  /** An HTTP proxy, with or without login. Without a host, no proxy is used. */
  public static class Proxy {

    private @Nullable String host;

    private @Nullable Integer port;

    private @Nullable String username;

    private @Nullable String password;

    /**
     * Creates empty settings. Without a host no proxy is used.
     */
    public Proxy() {
    }

    /**
     * The host name of the proxy, the key {@code proxy.host}.
     *
     * @return The host, or {@code null} if there is no proxy
     */
    public @Nullable String getHost() {
      return host;
    }

    /**
     * Sets the host name of the proxy.
     *
     * @param host The host
     */
    public void setHost(
        final @Nullable String host) {
      this.host = host;
    }

    /**
     * The port of the proxy, the key {@code proxy.port}. It has no default.
     *
     * @return The port, or {@code null} if not set
     */
    public @Nullable Integer getPort() {
      return port;
    }

    /**
     * Sets the port of the proxy.
     *
     * @param port The port
     */
    public void setPort(
        final @Nullable Integer port) {
      this.port = port;
    }

    /**
     * The user name to log in at the proxy, the key {@code proxy.username}.
     *
     * @return The user name, or {@code null} if the proxy needs no login
     */
    public @Nullable String getUsername() {
      return username;
    }

    /**
     * Sets the user name to log in at the proxy.
     *
     * @param username The user name
     */
    public void setUsername(
        final @Nullable String username) {
      this.username = username;
    }

    /**
     * The password to log in at the proxy, the key {@code proxy.password}.
     *
     * @return The password, or {@code null} for an empty one
     */
    public @Nullable String getPassword() {
      return password;
    }

    /**
     * Sets the password to log in at the proxy.
     *
     * @param password The password
     */
    public void setPassword(
        final @Nullable String password) {
      this.password = password;
    }

  }

  /**
   * Sends a request again when it got no answer. Off by default. The other defaults are the ones
   * of {@code ClientsConfigurationBase}, which took them from Feign's {@code Retryer.Default}.
   */
  public static class Retry {

    private boolean enabled;

    private int maxAttempts = 5;

    private Duration period = Duration.ofMillis(100);

    private Duration maxPeriod = Duration.ofSeconds(1);

    /**
     * Creates the defaults: off, and when switched on 5 attempts, 100 milliseconds, at most one
     * second.
     */
    public Retry() {
    }

    /**
     * Whether a request without an answer is sent again, the key {@code retry.enabled}.
     *
     * @return {@code false} by default
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Switches the retries on or off.
     *
     * @param enabled {@code true} to retry
     */
    public void setEnabled(
        final boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * How often a request is sent at most, the first attempt included, the key
     * {@code retry.max-attempts}.
     *
     * @return 5 by default
     */
    public int getMaxAttempts() {
      return maxAttempts;
    }

    /**
     * Sets how often a request is sent at most.
     *
     * @param maxAttempts All attempts, at least 1
     */
    public void setMaxAttempts(
        final int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    /**
     * The pause before the first retry, the key {@code retry.period}. Each further pause is one and
     * a half times longer.
     *
     * @return 100 milliseconds by default
     */
    public Duration getPeriod() {
      return period;
    }

    /**
     * Sets the pause before the first retry.
     *
     * @param period The pause
     */
    public void setPeriod(
        final Duration period) {
      this.period = period;
    }

    /**
     * The longest pause between two attempts, the key {@code retry.max-period}.
     *
     * @return One second by default
     */
    public Duration getMaxPeriod() {
      return maxPeriod;
    }

    /**
     * Sets the longest pause between two attempts.
     *
     * @param maxPeriod The pause
     */
    public void setMaxPeriod(
        final Duration maxPeriod) {
      this.maxPeriod = maxPeriod;
    }

  }

  /**
   * A bearer token of the OAuth client credentials grant. {@code token-uri} switches it on. The
   * token is fetched with the group's proxy and TLS settings and shared by all requests of the
   * group.
   */
  public static class OAuth {

    /** Client id and secret go into the form of the token request. */
    public static final String CLIENT_SECRET_POST = "client_secret_post";

    /** Client id and secret go into an HTTP basic authentication header of the token request. */
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";

    private @Nullable String tokenUri;

    private @Nullable String clientId;

    private @Nullable String clientSecret;

    private String clientAuthenticationMethod = CLIENT_SECRET_POST;

    private List<String> scope = new ArrayList<>();

    private Duration renewBefore = Duration.ofSeconds(30);

    private Duration lifetimeWithoutExpiresIn = Duration.ofHours(1);

    /**
     * Creates empty settings. Without a token endpoint no token is sent.
     */
    public OAuth() {
    }

    /**
     * The token endpoint of the OAuth server, the key {@code oauth.token-uri}.
     *
     * @return The endpoint, or {@code null} if the group sends no token
     */
    public @Nullable String getTokenUri() {
      return tokenUri;
    }

    /**
     * Sets the token endpoint of the OAuth server.
     *
     * @param tokenUri The endpoint
     */
    public void setTokenUri(
        final @Nullable String tokenUri) {
      this.tokenUri = tokenUri;
    }

    /**
     * The id of the OAuth client, the key {@code oauth.client-id}.
     *
     * @return The id, or {@code null} if not set
     */
    public @Nullable String getClientId() {
      return clientId;
    }

    /**
     * Sets the id of the OAuth client.
     *
     * @param clientId The id
     */
    public void setClientId(
        final @Nullable String clientId) {
      this.clientId = clientId;
    }

    /**
     * The secret of the OAuth client, the key {@code oauth.client-secret}.
     *
     * @return The secret, or {@code null} if not set
     */
    public @Nullable String getClientSecret() {
      return clientSecret;
    }

    /**
     * Sets the secret of the OAuth client.
     *
     * @param clientSecret The secret
     */
    public void setClientSecret(
        final @Nullable String clientSecret) {
      this.clientSecret = clientSecret;
    }

    /**
     * How client id and secret reach the token endpoint, the key
     * {@code oauth.client-authentication-method}: {@value #CLIENT_SECRET_POST} in the form, as
     * {@code ClientsConfigurationBase} does by default, or {@value #CLIENT_SECRET_BASIC} in an
     * HTTP basic authentication header.
     *
     * @return {@value #CLIENT_SECRET_POST} by default
     */
    public String getClientAuthenticationMethod() {
      return clientAuthenticationMethod;
    }

    /**
     * Sets how client id and secret reach the token endpoint.
     *
     * @param clientAuthenticationMethod {@value #CLIENT_SECRET_POST} or {@value #CLIENT_SECRET_BASIC}
     */
    public void setClientAuthenticationMethod(
        final String clientAuthenticationMethod) {
      this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    /**
     * The scopes asked for, the key {@code oauth.scope}.
     *
     * @return The scopes. Empty by default, so the server gives its default scope.
     */
    public List<String> getScope() {
      return scope;
    }

    /**
     * Sets the scopes asked for.
     *
     * @param scope The scopes
     */
    public void setScope(
        final List<String> scope) {
      this.scope = scope;
    }

    /**
     * How long before it expires a token is replaced at most, the key {@code oauth.renew-before}. A
     * token is replaced when a tenth of its lifetime is left, but never earlier than this.
     *
     * @return 30 seconds by default
     */
    public Duration getRenewBefore() {
      return renewBefore;
    }

    /**
     * Sets how long before it expires a token is replaced at most.
     *
     * @param renewBefore The time
     */
    public void setRenewBefore(
        final Duration renewBefore) {
      this.renewBefore = renewBefore;
    }

    /**
     * How long a token is used if the token endpoint does not say ({@code expires_in} is missing),
     * the key {@code oauth.lifetime-without-expires-in}. A {@code 401} drops a token earlier.
     *
     * @return One hour by default
     */
    public Duration getLifetimeWithoutExpiresIn() {
      return lifetimeWithoutExpiresIn;
    }

    /**
     * Sets how long a token without {@code expires_in} is used.
     *
     * @param lifetimeWithoutExpiresIn The lifetime
     */
    public void setLifetimeWithoutExpiresIn(
        final Duration lifetimeWithoutExpiresIn) {
      this.lifetimeWithoutExpiresIn = lifetimeWithoutExpiresIn;
    }

  }

}
