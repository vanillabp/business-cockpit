![VanillaBP](../readme/vanillabp-headline.png)

# Business Cockpit extension - Commons

The platform-neutral half of the Business Cockpit's integration into
[VanillaBP](https://www.vanillabp.io) Version 2, plus the two thin modules registering it on Spring
Boot and on Quarkus.

An extension joins the deployment pipeline of the VanillaBP core, watches what a workflow module
does and reports it somewhere. This one reports user tasks and workflows to a Business Cockpit
server. It knows no BPMS: the three halves which do live in their own repositories and consume this
one as a published artifact.

Extension is the core's word for it. A user adds a dependency and calls the result a cockpit
adapter, which is why the wiki uses that word and this file uses both,
[as `AGENTS.md` explains](../AGENTS.md#two-names-for-the-same-thing).

**Contents:**

1. [What is here](#what-is-here)
2. [What a BPMS half implements](#what-a-bpms-half-implements)
3. [How the two platforms carry the cockpit's keys](#how-the-two-platforms-carry-the-cockpits-keys)
4. [How it is tested](#how-it-is-tested)
5. [Noteworthy & Contributors](#noteworthy--contributors)
6. [License](#license)

What an application configures, what a details provider may set, how templates are looked up and
what changed against version 1 are in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki), under
[Configuration](https://github.com/vanillabp/business-cockpit/wiki/Configuration),
[Reporting workflows and user tasks](https://github.com/vanillabp/business-cockpit/wiki/Reporting-workflows-and-user-tasks),
[Templates](https://github.com/vanillabp/business-cockpit/wiki/Templates) and
[Migrating from version 1](https://github.com/vanillabp/business-cockpit/wiki/Migrating-from-version-1).
This file is about the module.

## What is here

|                   Module                   |                Artifact                 |                             What it is                             |
|--------------------------------------------|-----------------------------------------|--------------------------------------------------------------------|
| [core](./core)                             | `extensions-commons`                    | Everything below, without Spring and without CDI                   |
| [test-support](./test-support)             | `extensions-commons-test-support`       | The cockpit server every extension repository tests against        |
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

The path an event takes is drawn in the wiki under
[Architecture](https://github.com/vanillabp/business-cockpit/wiki/Architecture#the-path-a-report-takes).
Two things about it are decisions of this module rather than facts a user needs. The report is put
together while the BPMS event is observed and travels as the payload of the outbox entry, so what
the cockpit is told is the state of the event; the entry itself stays a row of identifiers, because
VanillaBP keeps the bytes beside it. Waiting reports about one task still collapse into one, and the
one which is left is the youngest, which is why they are planned through
`PhaseTwoOutbox#scheduleReplacingWhatIsStillWaiting`. And a transport which fails throws, so the
entry stays and the outbox repeats it, unless the cockpit server refused the report itself, which
ends the entry rather than repeating it forever. Decisions 26 and 11 of the
[decision log](../DECISIONS.md) say why.

The transport itself is a bean of both platforms, and an application which brings one of its own
reports through that instead. Whether it did is the platform's answer rather than a property key, so
`BusinessCockpitConfiguration.readAndValidate` takes it as an argument and an application reporting
its own way needs neither of the two shipped transports. Decision 22 says how each platform finds the
answer without building a transport to get it.

## What a BPMS half implements

Two interfaces in `io.vanillabp.cockpit.extension.spi`, and nothing else.

`BusinessCockpitBpmsBridge` is what the neutral half asks an engine. One implementation per
configured adapter id, because during a migration each id holds workflows of its own. It answers
five questions: what the BPMS knows about a user task, what it knows about a workflow, which
workflows of a workflow aggregate it holds, which user tasks of one it holds, and whether one named
task belongs to that aggregate. Two more have a default and are asked by `aggregateChanged`
only, see below.

Every reference a BPMS half hands over or is asked about carries the version of the deployed BPMN
process, spelled the way the engine reports it. It is what picks between details providers which
serve different generations of one model, so it is the plain version and never a version dressed up
for a screen. A BPMS which reports none leaves it empty, and only the providers naming no version
then run.

The two `prefilled…` questions are asked at the moment of the event, inside the transaction it
arrived in, because that is where the report is built. A half answers them out of the event it is
reporting rather than out of a storage which runs behind the engine. `prefilledUserTaskDetails`
serves a second moment as well, the read of `BusinessCockpitService.getUserTask`, and that one is
about now.

Both answers have an optional `createdAt`: when the BPMS created the task or started the workflow.
Only the report of an end sends it. The cockpit reads it where the end is the first report about the
record it gets, because the end overtook the creation, and the record then starts at that time.
A half fills it where the event at hand tells it, and leaves it empty where it could only ask the
engine. The cockpit then starts the record at its end. Both records keep their constructors without
the field, so a half which does not fill it needs no change.

Which answer a BPMS half gives decides whether a report happens at all. A half which cannot read
what it was asked for throws, and the exception reaches the engine reporting the event, so its work
fails and says so. An empty answer means the engine says nothing about the task or the workflow. A
report about a task or a case which is still running is then dropped for good, and a report of an
end is sent all the same, with its identifiers and without details: a completion which never
arrives leaves a task the cockpit shows as open forever. `PhaseTwoRetryLater` is no answer at the
moment of an event, because there is no entry to dispatch again yet.

One way is the exception: `BusinessCockpitService.aggregateChanged(aggregate)`. It runs in the
application's transaction, and a half whose engine writes a storage behind it cannot always name
the workflows of the case there. So the half is asked `workflowsOfAggregateRightAway` first. The
default answers what `workflowsOfAggregate` answers, and the report is built right away as on every
other way. A half which answers nothing gets an outbox entry without a report instead. When that
entry is dispatched, the half is asked `workflowsOfAggregate` and `prefilledWorkflowDetails`, and
the details provider runs in a transaction of the dispatch. An empty answer there means "not yet",
and the entry comes back after two seconds, with the distance growing by a tenth of the time it has
waited so far. So a BPMS which stays behind for minutes is not asked every two seconds. The
extension gives up after ten minutes, long before the outbox would block the entry on its own. A
change which still has no workflow then is not dropped. Its entry is blocked, like an entry which
used up its attempts, and the log says so at ERROR and names what to check in the BPMS. The outbox
does not dispatch a blocked entry again by itself. The entry keeps the reason in the column
`LAST_FAILURE`, on MongoDB in the field `lastFailure`. The ERROR links to
[Blocked outbox entries](https://github.com/vanillabp/adapter-platform-integration/wiki/Blocked-outbox-entries),
which shows how to find such an entry and open it again or delete it. A `PhaseTwoRetryLater` of
the half is understood on this way, and any other exception goes to the outbox, which repeats the
entry with its own backoff. The adapter of the change comes from VanillaBP's note of the start,
or is the one adapter the application configured. Only an application with several adapters and no
note asks the election, which may ask a BPMS.

`aggregateChanged(aggregate, userTaskIds)` works the same way for user tasks. The half is asked
`reportsAChangedUserTaskRightAway` first, and the default answers `true`, so the reports are built
right away. A half which answers `false` is asked nothing in the application's transaction. The
extension takes the open tasks from what VanillaBP wrote down when it delivered them, and writes
an entry without a report for each of them, or for each task the application named. When the
entry is dispatched, the half is asked `prefilledUserTaskDetails`. A task VanillaBP wrote nothing
down about is looked up there with `userTaskOfAggregate` first. Where the application named no
task and VanillaBP knows of no open one, the dispatch asks `userTasksOfAggregate` for every open
task, and an empty answer of that search sends nothing. An empty answer about a task waits and
blocks the same way as a workflow, and the log names the task.

An empty answer is silence and never the end of a task. No BPMS can promise the stronger reading. A
half whose engine publishes what it holds through a storage of its own writes that storage behind
the engine, so a task created a moment ago is missing from the answer exactly like a task which
ended. A half answering out of what its own node was served has the same gap for a case another node
serves. Only an engine asked inside the caller's transaction really knows. So a half says both
readings in the log where its answer is empty, the neutral half writes no report and leaves the
stored data alone, and `BusinessCockpitService.getUserTask` hands the empty answer to the
application with a javadoc saying what may be read into it. A task leaves a cockpit list when the
half reports an end, never because a search found nothing.

`BusinessCockpitEventPublisher` is the other direction, produced as a bean by the platform module. A
BPMS half calls it when its engine reported something, saying which transaction the entry belongs in
- the current one for an embedded engine invoking its listeners inside the engine's transaction, a
new one for a remote engine's worker thread.

The publisher also says which process variables the application's user-task details providers read
with `@TaskParam` (`variablesTheDetailsProvidersRead`). Some engines hand out only the variables a
half asked for. A Camunda 8 worker is one of them. Such a half asks for these names and passes on
what the engine sent as `UserTaskDetailsPrefill.variables`. The half names the workflow module, the
BPMN process, the element id and the task definition, and VanillaBP answers from the providers it
bound there (`ExtensionHandlers#taskParameterNames`). A half whose engine hands out every variable of
a task does not need to ask.

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

## How the two platforms carry the cockpit's keys

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

One key has no default on purpose: `initiator-source`. It says who answers the initiator of a
report, and a workflow module which reports to the cockpit and says it nowhere does not start. Every
other missing value shows an empty column until somebody configures it. This one cannot be put right
later: a report is built at the moment of the event, when the security context of the request is
already gone, so a case which ran without an initiator never learns who started it. Giving the key a
default would turn a field which was optional in version 1 into an exception without anybody saying
so. The runtime half of the rule sits in `BusinessCockpitExtension`, after the details provider and
before the titles. It runs at all three places a details object is built, the read path of
`BusinessCockpitService.getUserTask` included.

## How it is tested

The neutral core is tested where it can be tested without a platform: the configuration matrix, the
templating including a record as its data model, both transports against a cockpit server the test
runs itself, and the keys the outbox deduplicates by.

Each platform module then boots an application: a workflow aggregate, a workflow service with
details providers, VanillaBP's BPMS double, a BPMS half played by the test, and the same cockpit
server. An event is reported, the transaction commits, and the test reads what arrived. That
duplication between the two platforms is deliberate - the neutral core being right says nothing
about a platform's glue ever calling it.

That cockpit server is `test-support`, and the three BPMS repositories read it from there rather
than holding a copy. It is a published module because a test classpath cannot read another
repository's test classes, and it lives in `src/main/java` for the same reason. A test asks it
everything over HTTP: a Quarkus extension test initializes its test class twice, once while the
application is built and again inside the class loader of the running application, and the two
copies share no static field. Three numbers are system properties, so a repository whose reports
travel through a cluster waits longer without a class of its own:
`businesscockpit.test-server.wait-millis`, `businesscockpit.test-server.quiet-window-millis` and
`businesscockpit.test-server.quiet-wait-millis`. Its port comes from `OneFreePortPerJvm` of
`io.vanillabp:test-utils`.

What VanillaBP says about this extension's own annotations is asserted on both platforms too: two
providers of one user task whose versions overlap keep the application from starting, a provider
naming a version the BPMS does not hold is named while it boots, and one which is not public is
named in the startup report of the handler methods nobody sees. All three are the platform's doing,
and only a booted application shows whether the platform's glue registers the contract carrying
them.

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
