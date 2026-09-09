package io.vanillabp.cockpit.extension.config;

import java.time.Duration;

/**
 * What stands between a workflow module and an HTTP server it talks to: how long it waits,
 * which proxy it passes, and which certificates it trusts.
 * <p>
 * Two servers are talked to, and each brings its own answer: the cockpit server, and the
 * authorization server issuing the token the cockpit expects. Version 1 configured both
 * separately and this version does too, so a deployment whose authorization server sits behind
 * another proxy or presents a certificate of another authority can still say so.
 */
public interface RestConnection {

  /** How long version 1 waited for a connection, and what an application saying nothing keeps. */
  Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMillis(1500);

  /** How long version 1 waited for an answer. */
  Duration DEFAULT_READ_TIMEOUT = Duration.ofMillis(10000);

  /**
   * @return How long establishing the connection may take
   */
  Duration connectTimeout();

  /**
   * @return How long the server may take to answer
   */
  Duration readTimeout();

  /**
   * @return Whether the certificate the server presents is checked at all
   */
  boolean verifySsl();

  /**
   * @return The PKCS12 file holding the certificates the server's is checked against, or
   *         <code>null</code> for the ones the JVM trusts
   */
  String sslTruststoreFilename();

  /**
   * @return The password of that file
   */
  String sslTruststorePassword();

  /**
   * @return The HTTP proxy the server is reached through, or <code>null</code>
   */
  RestTransportConfiguration.Proxy proxy();

  /**
   * @return Whether the client trusts a set of certificates of this deployment rather than the
   *         ones the JVM was installed with
   */
  default boolean hasOwnTruststore() {

    return (sslTruststoreFilename() != null) && !sslTruststoreFilename().isBlank();

  }

  /**
   * @return Whether the connection needs anything the client's defaults do not give it, which
   *         is what decides whether the generated client keeps its own HTTP stack
   */
  default boolean needsAClientOfItsOwn() {

    return (proxy() != null) || !verifySsl() || hasOwnTruststore();

  }

}
