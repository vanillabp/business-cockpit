package io.vanillabp.cockpit.bpms;

import io.vanillabp.cockpit.commons.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = BpmsApiProperties.PREFIX, ignoreUnknownFields = false)
public class BpmsApiProperties {

	public static final String PREFIX = "bpms-api";

	private KafkaProperties kafka = new KafkaProperties();

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

	public KafkaProperties getKafka() {
		return kafka;
	}

	public void setKafka(KafkaProperties kafka) {
		this.kafka = kafka;
	}

}
