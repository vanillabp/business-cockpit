package io.vanillabp.cockpit.bpms.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";


    @Autowired
    private KafkaProperties properties;

    @Bean
    public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers) {
        Map<String, Object> configs = this.properties.buildConsumerProperties();

        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(configs);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
        return factory;
    }
}
