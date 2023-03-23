package io.vanillabp.cockpit.commons.rest.adapter;

import java.util.Map;

public class Client {
	
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
	
    public boolean isInitialized() {
        return (baseUrl != null) && !TO_BE_DEFINED_URL.equals(baseUrl);
    }

	public boolean useProxy() {
		return proxy != null && proxy.getHost() != null;
	}

	public Proxy getProxy() {
		return proxy;
	}
	
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
	
	public Authentication getAuthentication() {
		return authentication;
	}
	
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public Map<String, String> getAdditionalGetParameters() {
		return additionalGetParameters;
	}
	
	public void setAdditionalGetParameters(Map<String, String> additionalGetParameters) {
		this.additionalGetParameters = additionalGetParameters;
	}

    public boolean isVerifySsl() {
        return verifySsl;
    }

    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    public String getSslTruststoreFilename() {
        return sslTruststoreFilename;
    }

    public void setSslTruststoreFilename(String sslTruststoreFilename) {
        this.sslTruststoreFilename = sslTruststoreFilename;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

}
