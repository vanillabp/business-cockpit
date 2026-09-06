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

### What the cockpit shows right after a workflow was started

Camunda 8 answers questions about running workflows from its secondary storage, which is filled by an
exporter and therefore lags behind the cluster. `BusinessCockpitService.aggregateChanged(..)` has to
ask that storage which workflows carry the changed aggregate, so the answer depends on how far the
exporter has come.

What follows for an application:

* Reporting a changed aggregate in the same transaction which started its workflow is allowed. The
  adapter does not look the workflow up while that transaction runs, because the workflow is created
  only after the commit. It looks it up afterwards and waits for the workflow to become visible.
* Waiting is bounded. Once `workflow-visibility-timeout` has passed, the change is not sent and the
  adapter writes a warning which says the cockpit is lagging behind and names the aggregate and the
  BPMN process. Nothing is lost silently.
* Ten seconds is the default, the same the VanillaBP Camunda 8 adapter allows its cluster. Raise it
  for a cluster whose exporter needs longer, set it to `PT0S` to switch the waiting off:

```yaml
vanillabp:
  workflow-modules:
    my-workflow-module:
      adapters:
        camunda8:
          workflow-visibility-timeout: PT10S
```

The waiting never delays a business operation, because it happens after the caller's transaction
committed. What it does cost is the calling thread's time, so an application which reports changes of
aggregates whose workflows have already ended pays that timeout for every such call. On Camunda 7 none
of this applies: the query there is answered by the embedded engine from its own tables.

`getUserTask(..)` is the exception, since it answers its caller and cannot be deferred. It reads the
same secondary storage, so a user task created moments ago may not be reported yet.

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
