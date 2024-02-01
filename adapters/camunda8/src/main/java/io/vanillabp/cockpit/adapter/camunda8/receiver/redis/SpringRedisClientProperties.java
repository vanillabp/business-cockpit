package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SpringRedisClientProperties.PREFIX)
public class SpringRedisClientProperties {
    public static final String PREFIX = "vanillabp.redis";

    private String uri;
    private String consumerGroup;
    private String consumerId;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
