package io.vanillabp.cockpit.simulator.config;

import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The simulator's own properties, read from the prefix {@code application}.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private Client businessCockpit;

    private JwtProperties jwt;

    /**
     * Creates empty properties. Spring Boot fills them by the setters.
     */
    public ApplicationProperties() {
    }

    /**
     * How to reach the Business Cockpit: its base URL and the settings of the REST client.
     *
     * @return The client settings
     */
    public Client getBusinessCockpit() {
        return businessCockpit;
    }
    
    /**
     * Sets how to reach the Business Cockpit.
     *
     * @param businessCockpit The client settings
     */
    public void setBusinessCockpit(Client businessCockpit) {
        this.businessCockpit = businessCockpit;
    }

    /**
     * The settings of the JSON Web Tokens (JWT) which authenticate the users of the simulator.
     *
     * @return The JWT settings
     */
    public JwtProperties getJwt() {
        return jwt;
    }

    /**
     * Sets the settings of the JSON Web Tokens.
     *
     * @param jwt The JWT settings
     */
    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

}
