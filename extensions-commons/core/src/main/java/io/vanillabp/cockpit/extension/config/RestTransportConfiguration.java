package io.vanillabp.cockpit.extension.config;

import java.time.Duration;

/**
 * Where the cockpit server's BPMS API is, how to authenticate against it, and what stands
 * between the workflow module and it.
 * <p>
 * Everything but the address has a working default, so an application which only names the
 * server keeps the client it always had. What is configurable here is what a deployment cannot
 * change any other way: a proxy it has to pass, a certificate its own authority signed, a
 * server which answers slowly, and an authorization server issuing the token the cockpit
 * expects.
 *
 * @param baseUrl The address the API is served under, without the API's own path
 * @param username The user of the basic authentication, or <code>null</code> where the server
 *          expects none
 * @param password The password belonging to the user
 * @param connectTimeout How long establishing the connection may take, which is what version 1
 *          waited where an application configured nothing
 * @param readTimeout How long the server may take to answer
 * @param proxy The HTTP proxy the server is reached through, or <code>null</code>
 * @param verifySsl Whether the certificate the server presents is checked at all
 * @param sslTruststoreFilename The PKCS12 file holding the certificates the server's is checked
 *          against, or <code>null</code> for the ones the JVM trusts
 * @param sslTruststorePassword The password of that file
 * @param oauth The client-credentials flow the bearer token is fetched with, or
 *          <code>null</code>
 */
public record RestTransportConfiguration(
                                         String baseUrl,
                                         String username,
                                         String password,
                                         Duration connectTimeout,
                                         Duration readTimeout,
                                         Proxy proxy,
                                         boolean verifySsl,
                                         String sslTruststoreFilename,
                                         String sslTruststorePassword,
                                         OAuth oauth) implements RestConnection {

  /**
   * The HTTP proxy every request to the cockpit server passes.
   *
   * @param host The proxy's host
   * @param port The proxy's port
   * @param username The user it expects, or <code>null</code> where it expects none
   * @param password The password belonging to the user
   */
  public record Proxy(
                      String host,
                      int port,
                      String username,
                      String password) {

    /**
     * @return Whether the proxy is given a user, i.e. whether requests carry a
     *         Proxy-Authorization header
     */
    public boolean authenticates() {

      return (username != null) && !username.isBlank();

    }

  }

  /**
   * The client-credentials flow the bearer token is fetched with. There is no user in this
   * flow: the workflow module is the client, and the token it gets says so.
   *
   * @param tokenUrl The address the authorization server issues tokens under
   * @param clientId The client the token is asked for
   * @param clientSecret The secret belonging to it
   * @param clientInAuthorizationHeader Whether the client identifies itself in an
   *          <code>Authorization</code> header rather than in the form of the token request -
   *          which of the two an authorization server accepts is its own decision
   * @param connectTimeout How long establishing the connection to the authorization server may
   *          take, which is this server's own setting and not the cockpit server's
   * @param readTimeout How long the authorization server may take to answer
   * @param proxy The HTTP proxy it is reached through, or <code>null</code>
   * @param verifySsl Whether its certificate is checked at all
   * @param sslTruststoreFilename The PKCS12 file its certificate is checked against
   * @param sslTruststorePassword The password of that file
   */
  public record OAuth(
                      String tokenUrl,
                      String clientId,
                      String clientSecret,
                      boolean clientInAuthorizationHeader,
                      Duration connectTimeout,
                      Duration readTimeout,
                      Proxy proxy,
                      boolean verifySsl,
                      String sslTruststoreFilename,
                      String sslTruststorePassword) implements RestConnection {
  }

  /**
   * @return Whether a user was configured, i.e. whether requests carry an Authorization header
   */
  public boolean authenticates() {

    return (username != null) && !username.isBlank();

  }

}
