package io.vanillabp.cockpit.simulator.config;

import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.rest.adapter.ClientsConfigurationBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

@Configuration
public class ClientsConfiguration extends ClientsConfigurationBase {

    @Autowired
    private ApplicationProperties properties;
    
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Profile("rest-sync")
    public BpmsApi businessCockpitClient() {
        
        Client businessCockpit = properties.getBusinessCockpit();
        
        final var client = new ApiClient();
        client.setBasePath(businessCockpit.getBaseUrl());
        
        super.configureFeignBuilder(
                client.getClass(),
                client.getFeignBuilder(),
                businessCockpit);
        
        return client.buildClient(BpmsApi.class);
        
    }
    
}
