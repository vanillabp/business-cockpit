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
5. A transport which fails throws, the entry stays, and the outbox tries again with a backoff -
   unless the cockpit server refused the report itself, which ends the entry rather than repeating
   it forever.

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
task belongs to that aggregate.

Which answer a BPMS half gives decides whether a report happens at all. An engine which cannot be
reached throws, and the outbox tries again. An engine whose read model has not caught up with the
event it just sent throws `PhaseTwoRetryLater`, and the entry comes back after the window it names.
An empty answer means the engine does not know the task or the workflow any more, and the report is
dropped for good - which is right for a task somebody completed a second ago and wrong for one a
remote engine has merely not made searchable yet.

`BusinessCockpitEventPublisher` is the other direction, produced as a bean by the platform module. A
BPMS half calls it when its engine reported something, saying which transaction the entry belongs in
- the current one for an embedded engine invoking its listeners inside the engine's transaction, a
new one for a remote engine's worker thread.

Both are published contracts. A change to them is a change three repositories have to follow.

A BPMS half hands over nothing else. Which of the application's outbox stores an entry is written
into is VanillaBP's own answer on both platforms: the store the workflow aggregate's transaction
reaches, and the transaction is that aggregate's too - `PhaseTwoOutboxResolver` and
`TransactionRunnerResolver` answer both per aggregate class. A report of `BusinessCockpitService`
names that class outright. An event a BPMS observed names a workflow module, a BPMN process and a
serialized id instead, and the class is looked up: `ExtensionHandlers#workflowAggregateOf` answers
what VanillaBP read off `@WorkflowService` while it built the process services. So an application
whose aggregates live in two persistences has each of its reports written into the store of its own
workflow, in the transaction that workflow is written in. The pair is what the store is looked up
by, because two workflow modules may serve a process of the same name. Decisions 12, 13, 15 and 16
of the [decision log](../DECISIONS.md) say why.

`EventTransaction.CURRENT` joins the transaction running on the calling thread, which on Quarkus is
a JTA transaction. Joining one means every resource it reaches has to be enlistable, so an
application whose engine runs on a data source of its own needs EVERY data source declared as XA
(`quarkus.datasource.<name>.jdbc.transactions=xa`), the one the outbox store writes into included.
Without that the entry fails with "Failed to enlist" the moment a BPMS half reports with that value,
and the report is lost together with the engine's own transaction.

## Configuration

The keys are the ones version 1 of the Business Cockpit read, in the places it read them. An
application upgrades by changing a dependency, and what the cockpit has become inside is nothing it
has to know about.

```yaml
vanillabp:
  cockpit:
    user-tasks-enabled: true              # false stops user tasks from being reported
    workflow-list-enabled: true           # false stops workflows from being reported
    template-loader-path: classpath:cockpit-templates
    rest:
      base-url: http://localhost:8080     # the cockpit server, and with it the REST transport
      connect-timeout: 1500               # milliseconds, or written with a unit: 1500ms, PT1.5S
      read-timeout: 10000
      verify-ssl: true
      ssl-truststore-filename:
      ssl-truststore-password:
      proxy:
        host:
        port:
        username:
        password:
      authentication:
        basic: false                      # true sends a basic authentication
        username:
        password:
        oauth:
          base-url:                       # the token endpoint, and with it the flow
          client-id:
          client-secret:
          basic: false                    # true sends the client in an Authorization header
          # and its own connection: connect-timeout, read-timeout, verify-ssl,
          # ssl-truststore-filename, ssl-truststore-password, proxy.*
    kafka:
      bootstrap-servers:                  # the brokers, and with them the Kafka transport
      topics:
        user-task:
        workflow:
        workflow-module:
      properties:                         # anything else the producer is given
        security.protocol: SSL
    process-engine-api:
      remembered-user-tasks: 1000         # read by the cockpit's Process-Engine-API half
  workflow-modules:
    taxi-ride:
      cockpit:
        workflow-module-uri: http://localhost:8081/taxi-ride
        ui-uri-type: WEBPACK_MF_REACT
        ui-uri-path: /remoteEntry.js
        i18n-languages:
          - de
          - en
        bpmn-description-language: en
        template-path: rides
        group-hierarchy:
          TEAM_LEAD:
            - TEAM_MEMBER
            - ASSISTANT
      workflows:
        TaxiRide:
          cockpit:                        # what this workflow differs from its module in
            i18n-languages:
              - fr
            bpmn-description-language: fr
            template-path: taxi
          user-tasks:
            approve:
              cockpit:
                template-path: approval
```

Exactly one transport is configured. Neither of them, or both, ends the boot with a message naming
the keys of both. So does a workflow module which configured some of its settings and not the rest,
and so does a `ui-uri-type` naming something which does not exist: every gap the application has is
reported in one boot, each line naming the key which fixes it. A workflow module which configures
nothing at all reports nothing to the cockpit, which the log says while the application starts,
naming the key which would let it report.

The most specific value wins per key: a user task beats its workflow, and a workflow beats its
workflow module. What holds for the whole application is written once at the top and exists nowhere
else, so `ui-uri-path` has no global default. Version 1 had none either, and the modules answer at
different addresses.

### How the two platforms carry these keys

Neither platform reads anything below `vanillabp.cockpit` by itself, so the extension binds that
tree with the means each of them has: a `@ConfigurationProperties("vanillabp")` overlay on Spring
Boot, a second `@ConfigMapping(prefix = "vanillabp")` on Quarkus. Both are the pattern VanillaBP
documents for an adapter contributing keys of its own, and both hand one neutral object to the core.
The core is what parses and validates it, so a port which is no number and a timeout which is no
span of time are answered by the same message on either platform.

On Quarkus the mapping is also what lets the application start: a key below `vanillabp` which no
mapping declares ends the startup there. That is what a misspelled key runs into, and the build ends
naming it and the key it was nearest to, or, where it is one of version 1's, what became of it. On
Spring Boot such a key is ignored, which is the platform integration's own decision about the two
frameworks.

A workflow module's own defaults file, `<module>.yaml` on the classpath root or below a directory of
the module's name, carries these keys as well, below everything the application itself writes. That
is how a module ships a template path it alone knows about.

### What changed against version 1

Every key not named here is the key it was. These are gone, one reason each:

|                            Version 1 key                             |                                                            Why it is gone                                                            |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `rest.log`                                                           | the client logs through SLF4J, so set the logger `io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi` to `DEBUG`                             |
| `rest.additional-get-parameters.<name>`                              | it appended query parameters to GET requests, and every report is a POST                                                             |
| `rest.retry.*`                                                       | a failed report waits in the outbox and is repeated from there, which `vanillabp.outbox.*` configures                                |
| `kafka.group-id-suffix`                                              | it made a consumer group unique, and the extension only produces                                                                     |
| `jwt.hmacSHA256-base64`, `jwt.cookie.*`                              | version 1 bound them and read them nowhere, and the cockpit server configures its own tokens                                         |
| `group-hierarchy-bean-name`                                          | the hierarchy is configuration, and which groups may see a module is answered by the `WorkflowModuleDetailsProvider` bean            |
| `workerId`                                                           | it named the instance a report came from, and the extension takes the host name for that                                             |
| `spring.application.name`                                            | only the Camunda 8 exporter path read it, and that path is gone                                                                      |
| `spring.kafka.consumer.*`, `camunda.zeebe.kafka-exporter.topic-name` | version 1 also consumed a topic a Camunda 8 exporter wrote, and version 2 learns the same from the listeners it puts into the models |
| `vanillabp.workflow-modules.<id>.adapters.camunda8.*`                | a worker of the cockpit is a worker on the same cluster, so the Camunda 8 adapter's own keys are read rather than copied             |

Three keys are not version 1's. `kafka.bootstrap-servers` and `kafka.properties.*` are what version
1 took from `spring.kafka.bootstrap-servers` and `spring.kafka.producer.*`: the extension builds its
own producer now, and does so on both platforms. `process-engine-api.remembered-user-tasks` is new
with the Process-Engine-API integration and sizes what that half remembers between a delivery and
its dispatch.

Two things behave differently while everything is spelled the same. The timeouts accept the number
of milliseconds version 1 expected and the spelling both platforms use (`1500ms`, `PT1.5S`), and
they keep version 1's defaults of 1.5 and 10 seconds. And every setting is read and validated while
the application starts rather than when the first event arrives, so the log of the first boot is
what a developer works from. What went and why is written down in decision 14 of the
[decision log](../DECISIONS.md).

The token client of the client-credentials flow configures its own connection, as in version 1: it
inherits nothing from `rest.*`, so an authorization server behind another proxy or with another
certificate stays reachable. What it does not say for itself is the default rather than what the
cockpit server's client uses.

## Templates

With `template-loader-path` set and Freemarker on the classpath, the texts the cockpit shows are
rendered from templates. The path is a directory of the file system, written plainly or with
`file:` in front of it, or a directory of the classpath, written `classpath:cockpit-templates` -
the spellings version 1 accepted. The templates are: `workflow-title.ftl`, `task-title.ftl`, `task-definition-title.ftl`,
`task-fulltext-search.ftl` and `workflow-fulltext-search.ftl`. Each is looked for in the module's
directory, narrowed by the BPMN process and then by the task definition, and the most specific one
which exists wins. Each of the three levels is named after its own id unless `template-path` says
otherwise, which is how two processes share one directory of templates.

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

What VanillaBP refuses about this extension's own annotations is asserted on both platforms too: a
`@UserTaskDetailsProvider` naming the reserved `version` attribute keeps the application from
starting, and one which is not public is named in the startup report of the handler methods nobody
sees. Both are the platform's doing now, and only a booted application shows whether the platform's
glue registers the contract carrying them.

The configuration is asserted twice for the same reason, and there it is the binding rather than the
glue: each platform boots an application whose configuration carries every shape version 1 had - a
list, a group hierarchy as a map of lists, a workflow and one of its user tasks with sections of
their own, a timeout written as a number of milliseconds next to one written with a unit, and a
workflow module's own defaults file. On Quarkus a misspelled key is asserted as well, because there
it keeps the application from starting.

Coverage is measured separately per platform for the same reason, and the two numbers are published
next to the repository's other two. The Kafka transport is asserted against the Kafka client's own
test double, which is where the content of a message belongs, plus one report through a broker in a
container - what only a broker shows is that the producer this extension configures connects at all
and that what it sends is readable by somebody else. That one test needs a Docker daemon.

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
