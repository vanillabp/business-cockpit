package io.vanillabp.cockpit.config;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class MicroserviceProxyConfiguration {

    @Autowired
    private RouteLocatorBuilder routeLocatorBuilder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Bean
    // needed because routes causes event to be triggered which seems to bypass auto-configure-before from ChangeSetAutoConfiguration
    @DependsOn("changesetAutoConfiguration")
    public MicroserviceProxyRegistry microserviceProxyRegistry() {

        return new MicroserviceProxyRegistry(
                routeLocatorBuilder,
                applicationEventPublisher);

    }
    
}
