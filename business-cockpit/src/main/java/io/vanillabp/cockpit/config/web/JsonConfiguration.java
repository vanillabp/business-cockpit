package io.vanillabp.cockpit.config.web;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JsonConfiguration {

    @Bean
    public JavaTimeModule javatimeModule() {
        
        return new JavaTimeModule();
        
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonFormatDateTimes() {
        
        return builder -> builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonMinimizeOutput() {
        
        return builder -> builder
                .indentOutput(true)
                .serializationInclusion(Include.NON_NULL);
        
    }

    @Bean
    public Jackson2JsonEncoder jackson2JsonEncoder(
            final ObjectMapper mapper) {
        
        return new Jackson2JsonEncoder(mapper);
        
    }

    @Bean
    public Jackson2JsonDecoder jackson2JsonDecoder(
            final ObjectMapper mapper) {
        
        return new Jackson2JsonDecoder(mapper);
        
    }

    @Bean
    public WebFluxConfigurer webFluxConfigurer(
            final Jackson2JsonEncoder encoder,
            final Jackson2JsonDecoder decoder) {
        
        return new WebFluxConfigurer() {
            @Override
            public void configureHttpMessageCodecs(
                    final ServerCodecConfigurer configurer) {
                
                configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                
            }
        };

    }

}
