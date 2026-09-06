package io.vanillabp.cockpit.config;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class MicroserviceProxyConfiguration {

    @Bean
    // the registry is filled in bulk from MongoDB on startup, which has to happen after the
    // changesets created the collections it reads
    @DependsOn("changesetAutoConfiguration")
    public MicroserviceProxyRegistry microserviceProxyRegistry() {

        return new MicroserviceProxyRegistry();

    }

}
