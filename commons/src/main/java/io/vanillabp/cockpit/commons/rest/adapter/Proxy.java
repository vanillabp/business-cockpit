package io.vanillabp.cockpit.commons.rest.adapter;

/**
 * The HTTP proxy a REST client sends its requests through.
 */
public class Proxy {

	private String host = null;
	
	private int port;

    private String username;

    private String password;

    /**
     * Creates empty settings. Without a host no proxy is used.
     */
    public Proxy() {
    }

	/**
	 * The host name of the proxy.
	 *
	 * @return The host, or {@code null} if there is no proxy
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the host name of the proxy.
	 *
	 * @param host The host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port of the proxy.
	 *
	 * @return The port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets the port of the proxy.
	 *
	 * @param port The port
	 */
	public void setPort(int port) {
		this.port = port;
	}

    /**
     * The user name to log in at the proxy.
     *
     * @return The user name, or {@code null} if the proxy needs no login
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user name to log in at the proxy.
     *
     * @param username The user name
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * The password to log in at the proxy.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password to log in at the proxy.
     *
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }
	
}
