# Adapters

## General

The business cockpit adapters are the way-to-go for automatically sending user-task and workflow information from your BPMS application to the business cockpit. They also allow you to add additional event-driven logic to your application via annotations. 

Currently, there is a Camunda 7 adapter available and a Camunda 8 adapter in development. 

## Event propagation to Business Cockpit

Two ways of event propagation of user task and workflow events are currently provided. 

- REST (default): The business-cockpit REST API is used for event transmission.

- Kafka: Events are encoded via Protobuf and then sent via a Kafka broker

Note that this functionality is not adapter specific, i.e. it is available in every readily implemented adapter.

## Configuration

### REST (default)

Per default, events are sent via REST. This means that if no other message transmission method is configured explicitly, connection properties for the business cockpit REST connection must be set:

```
application:  
  business-cockpit:  
    base-url: <business cockpit url>  
    authentication:  
      basic: true  
      username: <username>
      password: <password>
```

### Kafka

If you want to propagate events via a Kafka broker instead, your workflow application must add the spring-kafka dependency and additionally have topic names for both user-task and workflow events set. These must be equal to the Kafka topic names in your business-cockpit configuration.

```
vanillabp:  
  cockpit:  
    kafka-topics:  
      user-task: <user task topic name>  
      workflow: <workflow topic name>
```

All other connection properties for Kafka can be set as in the standard Spring Boot autoconfiguration (see [here](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.integration))

Additionally, optional Maven dependencies have to be added:

```xml
<dependency>
    <groupId>io.vanillabp.businesscockpit</groupId>
    <artifactId>bpms-protobuf-api</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
