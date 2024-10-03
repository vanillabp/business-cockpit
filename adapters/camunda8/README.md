![](../../readme/vanillabp-headline.png)

# Camunda 8 adapter

This adapter is used for business applications using [Camunda 8](https://docs.camunda.org).

## Usage

```xml
<dependency>
   <groupId>io.vanillabp.businesscockpit</groupId>
   <artifactId>camunda8-spring-boot-adapter</artifactId>
</dependency>
```

For details see documentation of [SPI for Java](../../spi-for-java).

### Configuration of Kafka

The user tasks and workflow lifecycle events consumed for *VanillaBP Business Cockpit* data propagation
are created by consuming Kafka events [exported by Zeebe](https://github.com/Phactum/zeebe-kafka-exporter).
There are other APIs of Camunda which particially provide the events needed, but non of them is
as fast and reliable as using the events from [Zeebe](https://docs.camunda.io/docs/components/zeebe/zeebe-overview/).

Use this [docker-compose.yaml](https://github.com/Phactum/zeebe-kafka-exporter/blob/main/docker-compose.yml)
as a reference for a local setup or read
[Camunda`s documentation](https://docs.camunda.io/docs/self-managed/zeebe-deployment/exporters/install-zeebe-exporters/)
how to achieve this for non-local setups.

All Zeebe events are expected to be published in a single Kafka topic. Use this configuration to specify the
topic's name:

```yaml
camunda:
  zeebe:
    kafka-exporter:
      topic-name: zeebe
```

### Configuration of Liquibase

This adapter needs to store data about BPMN resources deployed in a database
since Camunda's APIs do not provide all information necessary. Therefore, one has to
add a predefined Liquibase changeset configuration:

```yaml
databaseChangeLog:
  - include:
      file: classpath:/io/vanillabp/camunda8/liquibase/main.yaml
```

*Hint:* This is a global configuration and has to be placed in the `application.yaml` of the
Spring Boot runtime container of the workflow module.
