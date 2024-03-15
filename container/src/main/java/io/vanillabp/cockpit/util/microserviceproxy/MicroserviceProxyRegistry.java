package io.vanillabp.cockpit.util.microserviceproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MicroserviceProxyRegistry implements RouteLocator {

    private static final Logger logger = LoggerFactory.getLogger(
            MicroserviceProxyRegistry.class);
    
    public static final String WORKFLOW_MODULES_PATH_PREFIX = "/wm/";
    
    private final Lock readLock;
    
    private final Lock writeLock;
    
    private final Map<String, String> routes = new HashMap<>();

    private final RouteLocatorBuilder routeLocatorBuilder;

    private final ApplicationEventPublisher applicationEventPublisher;
    
    public MicroserviceProxyRegistry(
            final RouteLocatorBuilder routeLocatorBuilder,
            final ApplicationEventPublisher applicationEventPublisher) {

        this.routeLocatorBuilder = routeLocatorBuilder;
        this.applicationEventPublisher = applicationEventPublisher;

        final var readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        
    }

    public Flux<Route> getRoutes() {

        final var routesBuilder = routeLocatorBuilder.routes();
        
        try {
            
            readLock.lock();

            return Flux.fromStream(
                        routes.entrySet()
                                .stream()
                                .peek(entry -> logger.info(
                                        "Register microservice proxy for workflow module: {}",
                                        entry.getKey()))
                                .map(entry -> routesBuilder.route(
                                        entry.getKey(),
                                        predicateSpec -> {
                                            final var proxyPath = WORKFLOW_MODULES_PATH_PREFIX + entry.getKey() + "/";
                                            final var uri = URI.create(entry.getValue());
                                            final var targetUri = UriComponentsBuilder.fromUri(uri).replacePath("").toUriString();
                                            final var targetPath = uri.getPath() == null
                                                    ? "/"
                                                    : uri.getPath().endsWith("/")
                                                    ? uri.getPath()
                                                    : uri.getPath() + "/";
                                            return predicateSpec
                                                    .path(proxyPath + "**")
                                                    .filters(f -> f.rewritePath(proxyPath, targetPath))
                                                    .uri(targetUri);
                                        }))
                    )
                    .collectList()
                    .flatMapMany(builders -> routesBuilder.build().getRoutes());
            
        } finally {
            readLock.unlock();
        }
        
    }

    public void registerMicroservice(
            final String id,
            final String uri) {
        
        try {
            
            readLock.lock();
            if (routes.containsKey(id)) {
                return;
            }
            
        } finally {
            readLock.unlock();
        }
        
        try {
            
            writeLock.lock();
            if (routes.containsKey(id)) {
                return;
            }
            
            routes.put(id, uri);
            
        } finally {
            writeLock.unlock();
        }

        applicationEventPublisher.publishEvent(
                new RefreshRoutesEvent(this));

    }

    public void registerMicroservices(
            final Map<String, String> microserviceUris) {

        int numberOfPreviousKnownMicroservices = 0;
        int numberOfAfterwardsKnownMicroservices = 0;
        try {
            
            writeLock.lock();
            
            numberOfPreviousKnownMicroservices = routes.size();
            
            microserviceUris
                    .entrySet()
                    .stream()
                    .filter(entry -> !routes.containsKey(entry.getKey()))
                    .forEach(entry -> routes.put(entry.getKey(), entry.getValue()));
            
            numberOfAfterwardsKnownMicroservices = routes.size();
            
        } finally {
            writeLock.unlock();
        }

        if (numberOfPreviousKnownMicroservices != numberOfAfterwardsKnownMicroservices) {
            
            applicationEventPublisher.publishEvent(
                    new RefreshRoutesEvent(this));

        }
        
    }

}
