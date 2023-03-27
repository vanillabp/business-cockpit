package io.vanillabp.cockpit.simulator.config;

import io.vanillabp.cockpit.commons.rest.adapter.Client;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private Client businessCockpit;
    
    public Client getBusinessCockpit() {
        return businessCockpit;
    }
    
    public void setBusinessCockpit(Client businessCockpit) {
        this.businessCockpit = businessCockpit;
    }
    
}
