package io.vanillabp.cockpit.config.web;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * @see https://www.baeldung.com/spring-webflux-errors#global
 */
@Component
@Order(-2)
public class SpaNoHandlerFoundExceptionHandler
        extends AbstractErrorWebExceptionHandler
        implements ErrorWebExceptionHandler {
    
    @Value("${application.spa-default-file:classpath:/static/index.html}")
    private String defaultFile;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    public SpaNoHandlerFoundExceptionHandler(
            final ErrorAttributes g,
            final ApplicationContext applicationContext,
            final ServerCodecConfigurer serverCodecConfigurer) {
        
        super(g, new WebProperties.Resources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        
    }
    
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            final ErrorAttributes errorAttributes) {
        
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
        
    }

    private Mono<ServerResponse> renderErrorResponse(
            final ServerRequest request) {

        final var errorPropertiesMap = getErrorAttributes(
                request, ErrorAttributeOptions.defaults());
        final var responseStatus = (Integer) errorPropertiesMap.get("status");
        
        if (responseStatus == HttpStatus.NOT_FOUND.value()) {
            return handleNotFound();
        }

        return ServerResponse
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
        
    }
    
    private Mono<ServerResponse> handleNotFound() {
        
        final var resource = resourceLoader.getResource(defaultFile);
        
        return ServerResponse
                .status(HttpStatus.OK)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS))
                .bodyValue(resource);
        
    }
    
}
