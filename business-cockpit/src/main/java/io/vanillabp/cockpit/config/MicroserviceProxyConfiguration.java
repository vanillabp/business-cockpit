package io.vanillabp.cockpit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;

@Configuration
public class MicroserviceProxyConfiguration {

    @Autowired
    private RouteLocatorBuilder routeLocatorBuilder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Bean
    public MicroserviceProxyRegistry microserviceProxyRegistry() {

        return new MicroserviceProxyRegistry(
                routeLocatorBuilder,
                applicationEventPublisher);

    }
    
}
