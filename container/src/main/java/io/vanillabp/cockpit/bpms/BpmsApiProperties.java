package io.vanillabp.cockpit.bpms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bpms-api", ignoreUnknownFields = false)
public class BpmsApiProperties {

	private String realmName;
	
	private String username;
	
	private String password;

	public String getRealmName() {
		return realmName;
	}
	
	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
