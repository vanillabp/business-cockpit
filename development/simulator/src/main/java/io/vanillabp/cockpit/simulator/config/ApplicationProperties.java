package io.vanillabp.cockpit.simulator.config;

import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private Client businessCockpit;

    private JwtProperties jwt;
    
    public Client getBusinessCockpit() {
        return businessCockpit;
    }
    
    public void setBusinessCockpit(Client businessCockpit) {
        this.businessCockpit = businessCockpit;
    }

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

}
