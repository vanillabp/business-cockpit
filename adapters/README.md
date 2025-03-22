![](../readme/vanillabp-headline.png)

# Adapters

Business Cockpit adapters implement the [SPI for Java](../spi-for-java) which is used by business applications
to provide business data about user tasks and workflows. They are responsible for processing
events of the underlying Business Processing Management System (BPMS) to keep the data shown in the
*VanillaBP Business Cockpit* (list of user tasks, list of workflows) up to date.

**Contents:**
1. [General](#general)
1. [Common adapter functionality]()
1. [BPMS-specific Adapters](#bpms-specific-adapters)

## General

The business cockpit adapters are the ready-to-use components for *Spring Boot* and [VanillaBP based](https://github.com/vanillabp/spi-for-java)
business applications. The business workflow module should only use the SPI as a dependency but the
runtime container must provide the dependency for adapter specific to the BPMS used
(see ["Using an adapter"](../spi-for-java#using-an-adapter)).

*Hint:* For simple use cases the Maven module
for the business workflow module and the Maven module for the runtime container is the same
(see ["Workflow modules"](https://github.com/vanillabp/spring-boot-support?tab=readme-ov-file#workflow-modules)).

## Common adapter functionality

There are feature which are common to all adapters provided. These features are implemented in a
separate Maven module used as a dependency by adapters specific to a BPMS.

However, those features a described in [separate documentation](./commons):

1. [Configuration of event propagation](./commons#configuration-of-event-propagation)
1. [Using templates for event details](./commons#using-templates-for-event-details)

## BPMS-specific Adapters

There are configuration properties specific to the underlying BPMS. Checkout each adapter documentation
for details:

1. [Camunda 7](./camunda7)
1. [Camunda 8](./camunda8)
