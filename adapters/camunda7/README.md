![](../../readme/vanillabp-headline.png)

# Camunda 7 adapter

This adapter is used for business applications using [Camunda 7](https://docs.camunda.org) greater than `7.17.0`.

The user tasks and workflow lifecycle events consumed for *VanillaBP Business Cockpit* data propagation
are created by adding Camunda's [event listeners](https://docs.camunda.org/manual/7.21/user-guide/process-applications/process-application-event-listeners/). 
One downside of event listeners is that they may be skipped for manual actions done in the
[Camunda cockpit](https://docs.camunda.org/manual/7.21/webapps/cockpit/).
However, this adapter uses `built-in-listeners` which cannot be skipped to ensure also manual actions are
reflected properly.

## Usage

```xml
<dependency>
   <groupId>io.vanillabp.businesscockpit</groupId>
   <artifactId>camunda7-spring-boot-adapter</artifactId>
</dependency>
```

For details see documentation of [SPI for Java](../../spi-for-java).
