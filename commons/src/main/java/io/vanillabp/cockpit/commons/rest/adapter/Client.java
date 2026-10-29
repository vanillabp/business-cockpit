package io.vanillabp.cockpit.commons.rest.adapter;

import java.util.Map;

/**
 * The settings of one outgoing REST client: where it connects, how long it waits, and how it
 * deals with proxy, TLS, authentication and retries. {@link ClientsConfigurationBase} turns them
 * into a Feign client.
 */
public class Client {
	
    /**
     * The placeholder base URL a client has until somebody configures the real one. A client with this
     * URL counts as not initialized.
     */
    public static final String TO_BE_DEFINED_URL = "to-be-defined";

    private String baseUrl = TO_BE_DEFINED_URL;

	private int connectTimeout = 1500;
	
	private int readTimeout = 10000;
	
	private boolean log;
	
	private Proxy proxy;
	
    private Authentication authentication;
	
    private boolean verifySsl = true;

    private String sslTruststoreFilename;

    private String sslTruststorePassword;

	private Map<String, String> additionalGetParameters;
	
	private Retry retry;

    /**
     * Creates the settings with their defaults: no base URL yet, 1.5 seconds to connect, 10 seconds
     * to read, TLS certificates checked, no proxy, no authentication and no retries.
     */
    public Client() {
    }
	
    /**
     * Tells whether a real base URL is configured.
     *
     * @return Whether the base URL is set and is not the placeholder
     */
    public boolean isInitialized() {
        return (baseUrl != null) && !TO_BE_DEFINED_URL.equals(baseUrl);
    }

	/**
	 * Tells whether requests go through a proxy. This is the case when a proxy host is configured.
	 *
	 * @return Whether a proxy is used
	 */
	public boolean useProxy() {
		return proxy != null && proxy.getHost() != null;
	}

	/**
	 * The HTTP proxy the requests go through.
	 *
	 * @return The proxy settings, or {@code null} if there is no proxy
	 */
	public Proxy getProxy() {
		return proxy;
	}
	
	/**
	 * Sets the HTTP proxy the requests go through.
	 *
	 * @param proxy The proxy settings
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
	
	/**
	 * How the client proves who it is to the server.
	 *
	 * @return The authentication settings, or {@code null} if no authentication is used
	 */
	public Authentication getAuthentication() {
		return authentication;
	}
	
	/**
	 * Sets how the client proves who it is to the server.
	 *
	 * @param authentication The authentication settings
	 */
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}
	
	/**
	 * The URL all requests of this client start with.
	 *
	 * @return The base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}
	
	/**
	 * Sets the URL all requests of this client start with.
	 *
	 * @param baseUrl The base URL
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * How long the client waits for a connection to the server.
	 *
	 * @return The timeout in milliseconds
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Sets how long the client waits for a connection to the server.
	 *
	 * @param connectTimeout The timeout in milliseconds
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * How long the client waits for the server's response once it is connected.
	 *
	 * @return The timeout in milliseconds
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * Sets how long the client waits for the server's response once it is connected.
	 *
	 * @param readTimeout The timeout in milliseconds
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Tells whether each request and response is logged, body included, at level INFO.
	 *
	 * @return Whether requests are logged
	 */
	public boolean isLog() {
		return log;
	}

	/**
	 * Sets whether each request and response is logged, body included, at level INFO.
	 *
	 * @param log Whether requests are logged
	 */
	public void setLog(boolean log) {
		this.log = log;
	}

	/**
	 * Extra query parameters meant for GET requests. Nothing in this module reads them.
	 *
	 * @return The parameters by name
	 */
	public Map<String, String> getAdditionalGetParameters() {
		return additionalGetParameters;
	}
	
	/**
	 * Sets extra query parameters meant for GET requests. Nothing in this module reads them.
	 *
	 * @param additionalGetParameters The parameters by name
	 */
	public void setAdditionalGetParameters(Map<String, String> additionalGetParameters) {
		this.additionalGetParameters = additionalGetParameters;
	}

    /**
     * Tells whether the server's TLS certificate and host name are checked. Turn this off only for
     * tests against servers with self-signed certificates.
     *
     * @return Whether certificates are checked
     */
    public boolean isVerifySsl() {
        return verifySsl;
    }

    /**
     * Sets whether the server's TLS certificate and host name are checked.
     *
     * @param verifySsl Whether certificates are checked
     */
    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    /**
     * The PKCS12 file which holds the certificates the client trusts and its own client certificate.
     *
     * @return The path of the file, or {@code null} to use the JVM's defaults
     */
    public String getSslTruststoreFilename() {
        return sslTruststoreFilename;
    }

    /**
     * Sets the PKCS12 file which holds the certificates the client trusts and its own client
     * certificate.
     *
     * @param sslTruststoreFilename The path of the file
     */
    public void setSslTruststoreFilename(String sslTruststoreFilename) {
        this.sslTruststoreFilename = sslTruststoreFilename;
    }

    /**
     * The password of the truststore file.
     *
     * @return The password
     */
    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    /**
     * Sets the password of the truststore file.
     *
     * @param sslTruststorePassword The password
     */
    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    /**
     * Whether and how often a failed request is sent again.
     *
     * @return The retry settings, or {@code null} if requests are not retried
     */
    public Retry getRetry() {
        return retry;
    }
    
    /**
     * Sets whether and how often a failed request is sent again.
     *
     * @param retry The retry settings
     */
    public void setRetry(Retry retry) {
        this.retry = retry;
    }
    
}
