![VanillaBP](../readme/vanillabp-headline.png)

# Business Cockpit extension - Commons

The platform-neutral half of the Business Cockpit's integration into
[VanillaBP](https://www.vanillabp.io) Version 2, plus the two thin modules registering it on Spring
Boot and on Quarkus.

An extension joins the deployment pipeline of the VanillaBP core, watches what a workflow module
does and reports it somewhere. This one reports user tasks and workflows to a Business Cockpit
server. It knows no BPMS: the three halves which do live in their own repositories and consume this
one as a published artifact.

**Contents:**

1. [What is here](#what-is-here)
2. [How an event travels](#how-an-event-travels)
3. [What a BPMS half implements](#what-a-bpms-half-implements)
4. [Configuration](#configuration)
5. [Templates](#templates)
6. [How it is tested](#how-it-is-tested)
7. [Noteworthy & Contributors](#noteworthy--contributors)
8. [License](#license)

## What is here

|                   Module                   |                Artifact                 |                             What it is                             |
|--------------------------------------------|-----------------------------------------|--------------------------------------------------------------------|
| [core](./core)                             | `extensions-commons`                    | Everything below, without Spring and without CDI                   |
| [spring-boot](./spring-boot)               | `extensions-commons-spring-boot`        | One auto-configuration handing the beans over                      |
| [quarkus/runtime](./quarkus/runtime)       | `extensions-commons-quarkus`            | The producers doing the same with CDI                              |
| [quarkus/deployment](./quarkus/deployment) | `extensions-commons-quarkus-deployment` | The build steps of the Quarkus extension, and the tests running it |

The core carries the handler contracts of the details providers, the event model, the REST and
Kafka transports, the templating, the outbox operations, the `BusinessCockpitService` and the
interface a BPMS half implements. A platform module finds beans and hands them over; it decides
nothing, which is what keeps a feature from existing on one platform only.

The three BPMS halves live in
[businesscockpit-camunda7-adapter](https://github.com/vanillabp/businesscockpit-camunda7-adapter),
[businesscockpit-camunda8-adapter](https://github.com/vanillabp/businesscockpit-camunda8-adapter)
and
[businesscockpit-process-engine-api-adapter](https://github.com/vanillabp/businesscockpit-process-engine-api-adapter).

## How an event travels

1. A BPMS half observes something in its engine and calls `BusinessCockpitEventPublisher`.
2. One outbox entry is written, in the transaction the BPMS is in or in one of its own, carrying
   identifiers and nothing else.
3. The transaction commits. Nothing has been sent yet, and nothing will be sent if it does not
   commit.
4. VanillaBP's outbox dispatches the entry. The BPMS half is asked what it knows about the task or
   the workflow now, the application's `@UserTaskDetailsProvider` or `@WorkflowDetailsProvider`
   method is invoked to enrich it, the titles are rendered, and the result goes to the configured
   transport.
5. A transport which fails throws, the entry stays, and the outbox tries again with a backoff.

Reading at dispatch time rather than carrying the data through the outbox is what keeps an entry
inside the 2048 characters an outbox store holds, and it is what makes several pending reports about
one task collapse into one. Both consequences are written down as decision 3 of the
[decision log](../DECISIONS.md).

`BusinessCockpitService`, which a workflow service injects to report a change of its own, takes the
same way: it asks the election which BPMS holds the workflow, asks that BPMS half which workflows
and tasks belong to the aggregate, and writes an entry per answer. Only `getUserTask` is
synchronous, and it reports nothing.

## What a BPMS half implements

Two interfaces in `io.vanillabp.cockpit.extension.spi`, and nothing else.

`BusinessCockpitBpmsBridge` is what the neutral half asks an engine. One implementation per
configured adapter id, because during a migration each id holds workflows of its own. It answers
five questions: what the BPMS knows about a user task, what it knows about a workflow, which
workflows of a workflow aggregate it holds, which user tasks of one it holds, and whether one named
task belongs to that aggregate. An engine which cannot answer something leaves the value empty; an
engine which cannot be reached throws, and the outbox tries again.

`BusinessCockpitEventPublisher` is the other direction, produced as a bean by the platform module. A
BPMS half calls it when its engine reported something, saying which transaction the entry belongs in
- the current one for an embedded engine invoking its listeners inside the engine's transaction, a
new one for a remote engine's worker thread.

Both are published contracts. A change to them is a change three repositories have to follow.

## Configuration

Everything lives below the two locations the VanillaBP core reserves for an extension. A workflow
module's value wins per key, and it keeps whatever the global section says about the rest.

```yaml
vanillabp:
  extensions:
    business-cockpit:
      rest:
        base-url: http://localhost:8080
  workflow-modules:
    taxi-ride:
      extensions:
        business-cockpit:
          workflow-module-uri: http://localhost:8081/taxi-ride
          ui-uri-type: WEBPACK_MF_REACT
          ui-uri-path: /remoteEntry.js
          i18n-languages: en,de
          bpmn-description-language: en
```

Global keys:

|                                        Key                                        |                                         What it is                                         |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `rest.base-url`                                                                   | The cockpit server's address, and with it the choice of the REST transport                 |
| `rest.username`, `rest.password`                                                  | The basic authentication the server expects, where it expects one                          |
| `kafka.bootstrap-servers`                                                         | The brokers, and with them the choice of the Kafka transport                               |
| `kafka.topics.user-task`, `kafka.topics.workflow`, `kafka.topics.workflow-module` | One topic per kind of event                                                                |
| `kafka.properties.*`                                                              | Anything else the Kafka producer is to be given, e.g. `kafka.properties.security.protocol` |
| `template-loader-path`                                                            | The directory the templates are loaded from                                                |

Per workflow module:

|             Key             |                           What it is                            |
|-----------------------------|-----------------------------------------------------------------|
| `workflow-module-uri`       | Where the module answers the cockpit's provider APIs            |
| `ui-uri-type`               | `EXTERNAL` or `WEBPACK_MF_REACT`                                |
| `ui-uri-path`               | Where the module's forms are served from                        |
| `i18n-languages`            | The languages titles are reported in, comma separated           |
| `bpmn-description-language` | The language the names in the BPMN files are written in         |
| `group-hierarchy.<group>`   | Which groups a group stands for, comma separated                |
| `template-path`             | The segment this module contributes to the template lookup path |

Exactly one transport is configured. Neither of them, or both, ends the boot with a message naming
the keys of both. So does a workflow module missing a setting, and so does a `ui-uri-type` naming
something which does not exist - every gap the application has is reported in one boot, each line
naming the key which fixes it.

Version 1 of the Business Cockpit configured the same things below `vanillabp.cockpit` and
`vanillabp.workflow-modules.<id>.cockpit`. The keys map like this:

|                              Version 1                              |                                     Version 2                                     |
|---------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `vanillabp.cockpit.rest.base-url`                                   | `vanillabp.extensions.business-cockpit.rest.base-url`                             |
| `vanillabp.cockpit.kafka.topics.user-task`                          | `vanillabp.extensions.business-cockpit.kafka.topics.user-task`                    |
| `vanillabp.cockpit.kafka.topics.workflow`                           | `vanillabp.extensions.business-cockpit.kafka.topics.workflow`                     |
| `vanillabp.cockpit.kafka.topics.workflow-module`                    | `vanillabp.extensions.business-cockpit.kafka.topics.workflow-module`              |
| `vanillabp.cockpit.template-loader-path`                            | `vanillabp.extensions.business-cockpit.template-loader-path`                      |
| `vanillabp.workflow-modules.<id>.cockpit.workflow-module-uri`       | `vanillabp.workflow-modules.<id>.extensions.business-cockpit.workflow-module-uri` |
| `vanillabp.workflow-modules.<id>.cockpit.ui-uri-type`               | `…extensions.business-cockpit.ui-uri-type`                                        |
| `vanillabp.workflow-modules.<id>.cockpit.ui-uri-path`               | `…extensions.business-cockpit.ui-uri-path`                                        |
| `vanillabp.workflow-modules.<id>.cockpit.i18n-languages`            | `…extensions.business-cockpit.i18n-languages`                                     |
| `vanillabp.workflow-modules.<id>.cockpit.bpmn-description-language` | `…extensions.business-cockpit.bpmn-description-language`                          |
| `vanillabp.workflow-modules.<id>.cockpit.group-hierarchy`           | `…extensions.business-cockpit.group-hierarchy.<group>`                            |
| `vanillabp.workflow-modules.<id>.cockpit.template-path`             | `…extensions.business-cockpit.template-path`                                      |

The Kafka connection is configured by the extension itself now, rather than taken from the
application's Spring Kafka settings. Which listener types a BPMS puts into your BPMN files, and
what an upgrade does to a deployed process, is documented in the repository of that BPMS.

## Templates

With `template-loader-path` set and Freemarker on the classpath, the texts the cockpit shows are
rendered from templates: `workflow-title.ftl`, `task-title.ftl`, `task-definition-title.ftl`,
`task-fulltext-search.ftl` and `workflow-fulltext-search.ftl`. Each is looked for in the module's
directory, narrowed by the BPMN process and then by the task definition, and the most specific one
which exists wins.

The data model is what a details provider passed to `setTemplateContext`, which may be a map, a
POJO or a record. Without a template directory the names written in the BPMN files are reported
instead, in the one language `bpmn-description-language` declares them to be in - which is why that
key is mandatory without templates and optional with them.

A value a details provider already wrote is treated as the name of a template, and stays the literal
text where no template of that name exists. That is how a workflow module gives one task a fixed
title without shipping a template for it.

## How it is tested

The neutral core is tested where it can be tested without a platform: the configuration matrix, the
templating including a record as its data model, both transports against a cockpit server the test
runs itself, and the keys the outbox deduplicates by.

Each platform module then boots an application: a workflow aggregate, a workflow service with
details providers, VanillaBP's BPMS double, a BPMS half played by the test, and the same cockpit
server. An event is reported, the transaction commits, and the test reads what arrived. That
duplication between the two platforms is deliberate - the neutral core being right says nothing
about a platform's glue ever calling it.

Coverage is measured separately per platform for the same reason, and the two numbers are published
next to the repository's other two. The Kafka transport is asserted against the Kafka client's own
test double rather than a broker in a container: what is worth reading there is the message, and a
broker would only carry the same bytes back.

```bash
mvn -pl extensions-commons/core,extensions-commons/spring-boot -am install
```

Quarkus tests load the extension from the local Maven repository, so they need `install` rather than
`package`. `mvn spotless:apply` runs before every commit; the formatter is configured for this
subtree only.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to
the community as it has benefited the community in the past.

![Phactum](../readme/phactum.png)

## License

Copyright 2026 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0
