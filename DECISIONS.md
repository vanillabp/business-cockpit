# Decision log

The decisions this repository's code points at. A number is handed out once. It is never reused and
never renumbered, so a citation stays resolvable. A decision which is overturned keeps its entry.
That entry is marked as superseded and names the entry which replaced it.

A citation in code reads `see decision 3 in the repository's DECISIONS.md`. It names an entry of
THIS repository only. A decision the platform shares has its own entry in
`adapter-platform-integration`, written from that side. One a BPMS half of the extension shares has
its entry in that adapter's repository. A pointer into another repository is the fragile kind this
log exists to avoid.

### 1. The generated BPMS API client names its own dependencies

`apis/bpms-api/client` used to get everything it compiles against from the cockpit's `commons`
artifact. That artifact brings Spring Boot, Spring Data MongoDB and Spring Security with it. The
generated code touches none of them. The OpenAPI generator's Feign library with `interfaceOnly`
emits Feign, Jackson and `jakarta.annotation`, and nothing more.

The client now names those nine artifacts itself. `extensions-commons` has to stay free of both
platforms, and it cannot use a client which pulls a web framework into every consumer. The two
consumers the client already had declare `commons` on their own, so nothing they use went away.

### 2. The neutral half of the extension compiles against no platform integration - the price it names superseded by decision 15

`extensions-commons/core` sees the two VanillaBP SPI artifacts, the platform-neutral
`migration-adapter` and the cockpit's own API artifacts. It does not see
`vanillabp-spring-boot-integration`, `vanillabp-quarkus-integration`, Spring or CDI.

The price is easy to see. Each platform module carries a `TransactionRunner` of its own, doing what
the platform's own runner does. What it buys is that no feature can quietly exist on one platform
only. VanillaBP asks this of every extension, and its own sample extension is built that way, so a
reviewer of either repository expects this shape.

### 3. An outbox entry carries identifiers, and the event is built when it is dispatched - the moment the event is built superseded by decision 26

By contract, VanillaBP's outbox holds at most 2048 characters of arguments per entry. Version 1
serialized the whole event into the entry, and that does not fit any more. An entry now carries the
kind of event, the adapter id, the identifiers of the task and the workflow, the task definition,
the BPMN element id, the event's own id and its timestamp.

Everything else is read while the entry is dispatched. The BPMS half is asked what it knows now, the
application's details provider is called, the titles are rendered, and the result is sent.

Two things follow from that, and both are wanted. Several pending reports about one task collapse
into one, because the surviving entry reports what the discarded ones would have reported. And a
change a details provider makes to the workflow aggregate is committed while the entry is
dispatched, not with the transaction the BPMS event arrived in. A provider which writes is writing
later than it looks.

### 4. The idempotency key of a report names the operation, the BPMS, the entity and the kind - which of two entries of one key survives superseded by decision 26

A task or a workflow gets `<operation>|<adapterId>|<taskId or workflowId>|<eventKind>`. The
registration of a workflow module gets `<operation>|<workflowModuleId>`. Every key starts with the
operation, the way the core's own keys do.

So two pending updates of one task share a key and only one of them survives. That is the point and
not a side effect: by decision 3 the survivor reports the current state. The BPMS' own event id is
deliberately left out of the key, and so is any clock reading. Version 1 built an event id from
`System.nanoTime()`, which turned every repetition into a new event.

The keys are a persisted contract. An entry written before an upgrade is deduplicated after it. So
an operation is never renamed, and the rule a key is derived by never changes.

### 5. The extension dispatches its own operations, and routes them by adapter id

The operations are `businesscockpit:PUBLISH_USER_TASK_EVENT`,
`businesscockpit:PUBLISH_WORKFLOW_EVENT` and `businesscockpit:REGISTER_WORKFLOW_MODULE`. All of them
are `Election.OWN_DISPATCH`. The core elects nothing here, the extension dispatches them itself.

The adapter id an entry carries says which BPMS half serves the dispatch. The election answered that
id when the event was observed. If no BPMS half is registered for the id, the dispatch ends with a
message which names the registered ids and the three artifacts one comes from.

### 6. A workflow module registers itself through the same outbox, once the application is up

The registration is an outbox entry like every other report. So a cockpit server which is down while
the application starts does not stop the application from booting. The entry is written, the boot
goes on, and the outbox keeps trying.

The entry is written once the application is up, not while its workflow modules are deployed. On
Quarkus the outbox store creates its table in a startup observer of its own, and that observer runs
after the deployment pipeline. An entry written any earlier would find no table. Both platforms
write at the same point, so the two halves stay comparable.

### 7. The `version` attribute of `@UserTaskDetailsProvider` is reserved and refused - who refuses it superseded by decision 16, the refusal itself by decision 24

Version-aware matching means picking a different method for each deployed version of a process. The
events this extension reacts to carry no process version. A task listener says which task fired, not
which version of the model it came from.

Version 1 documented the attribute and never read it, so applications wrote it and believed it
worked. A value is now refused while the application starts. The message names the attribute and the
two attributes to match by instead. For an application which left the attribute alone, nothing about
the annotation changes.

Superseded by decision 24: the events do carry a version now. The platform reports it with every
call of a handler, so there is something to decide by, and refusing the attribute would keep the
Business Cockpit behind what `@WorkflowTask` has always been able to do.

### 8. The extension owns one outbox store, chosen at startup - superseded by decision 10

The extension writes its entries into the application's single `PhaseTwoOutbox`. An application
which runs several of them is told so while it starts, and the message names the stores. No store is
picked for it.

`PhaseTwoOutboxAware` is what per-aggregate stores are for. They could be resolved for the reports
made through `BusinessCockpitService`, where the aggregate class is known. They cannot be resolved
for an event a BPMS observed. What arrives there is a workflow module, a BPMN process and a
serialized id, but no class. One store for the whole extension is the shape which works in both
directions.

Superseded by decision 10: one store for the whole extension was one store too few. An application
whose stores all come from `PhaseTwoOutboxAware` beans has no plain store bean at all, and it was
told to add a data source it already had. An application with two persistences did not boot,
although the platform attributes a store to every aggregate of it. And where one of two stores was
marked as primary, entries were written into a store the aggregate's transaction never reaches,
which is the one thing an outbox must not do.

### 9. One event class per side, with the kind as a field

Version 1 had one class per kind of event, twelve of them, because its publishing dispatched on the
class. The extension has `UserTaskEvent` and `WorkflowEvent`. Each of them carries the kind, and the
transports switch on it.

In the mappers the four kinds still look alike. The cockpit's API declares one schema per kind, and
the generator made four classes for them. Folding the four mappings into one would mean converting
between generated classes, and converting a timestamp is the kind of thing which goes wrong without
anybody noticing.

### 10. The outbox store is the one the workflow aggregate's transaction reaches - the Quarkus half superseded by decision 12

An entry is written into the store VanillaBP attributes to the workflow aggregate the report is
about. That is what the platform does for its own operations. Wherever the extension knows the
aggregate's class, which is every report made through `BusinessCockpitService`, it asks for that
store and writes there. So an application with a store per persistence, or with a store of its own
for one aggregate, is served the way it expects.

An event a BPMS observed knows no class. It names a workflow module, a BPMN process and a serialized
id, and nothing in either SPI turns that into the class of the aggregate. Such an entry therefore
goes into the application's single store. An application which runs several is told that the report
cannot be attributed, rather than having a store picked for it. The registration of a workflow
module belongs to no aggregate at all and is written in a transaction of its own, so any store
carries it correctly. The first store by class name is taken, so a restart writes it into the same
one.

On Spring Boot the attribution is VanillaBP's own `PhaseTwoOutboxResolver` bean, taken as the
interface the platform-neutral core knows. On Quarkus there is no such bean, so the extension does
what it can reach: the store an application named for the aggregate, and otherwise the single store.
The platform goes further and attributes one of its two default stores to the persistence which
manages an aggregate. That needs classes from the Quarkus integration, and an extension does not
compile against one (decision 2). So a Quarkus application which runs two stores names the store of
an aggregate itself, with a `PhaseTwoOutboxAware` bean, and the message asks for that bean where it
is missing.

Superseded by decision 12: the Quarkus half of this was a second attribution next to the platform's
own, and it got wrong the same application the platform gets right. The Quarkus integration now
offers its resolver as a bean as well. Both platforms are served by the one answer, and the question
is asked while the application boots instead of at the first report.

### 11. A failed report says whether repeating it can help

The outbox repeats what a dispatch threw. That is right for a cockpit server which was restarting,
and wrong for a report the server will refuse every time. An entry retried forever sits in the store
and hides the defect. So the transports classify what they get back.

Over REST a status the server blames the client for ends the entry for good. That is anything from
400 up to 499, except the two which mean "later". 503 and 429 mean "not now". They give the entry
back with the time the server named in `Retry-After`, so the dispatching thread is free while the
entry waits. Everything else is repeated with the store's own backoff.

Over Kafka the client's own classification is used. It marks as retriable a broker which is busy,
gone for a moment, or leading another partition now. Everything else is about the record itself: a
topic which does not exist, a message larger than the broker takes, or a client which may not write
there.

An entry naming an adapter id no BPMS half serves stays repeatable, because the jar carrying that
half may be missing from one deployment and back in the next.

### 12. The store of an entry is VanillaBP's answer, asked while the application boots - the shared-store rule superseded by decision 13

The extension never attributes a store itself. It asks the platform's `PhaseTwoOutboxResolver`. That
is a bean on Spring Boot, and the Quarkus integration offers it as one too. So an entry of the
extension is written where an entry of the core is written: into the store the transaction of the
workflow aggregate reaches.

The extension's own resolution was a copy which could never be complete. Which of the platform's two
default stores serves which persistence, and whether a default is switched on and usable at all, is
knowledge of the platform integration. An extension does not compile against one (decision 2). So
the copy refused an application with two stores where the platform serves it. And where the platform
refuses, the copy returned the store of the other technology. That writes an entry next to the
aggregate instead of into its transaction, which is the one thing this outbox must not do.

Every workflow aggregate of the application is resolved once while it boots, the way VanillaBP
validates the outbox of its own process services. That settles two things. A store which cannot be
attributed ends the boot with the platform's own message, instead of showing up inside the
transaction of the first report. And the store all aggregates share is where an entry naming no
aggregate class is written. An event a BPMS observed names a workflow module, a BPMN process and a
serialized id, and nothing in either SPI turns that into a class. An application whose aggregates
live in different stores has no shared store, and it is told so while it boots. Letting it start and
fail at the first user task would be the same defect one report later.

Superseded by decision 13: "nothing turns that into a class" was wrong. The workflow service which
serves the BPMN process names the aggregate it is written for. So an application whose aggregates
live in two stores is served rather than refused.

### 13. The BPMN process an event names is what its outbox store is looked up by - how it is looked up superseded by decision 16

An event a BPMS observed carries a workflow module, a BPMN process and a serialized id. It belongs
in the store which holds the workflow aggregate. The class is not carried, and it does not have to
be. `@WorkflowService` declares both halves: the aggregate the service is written for, and the BPMN
processes it serves, the primary one and whatever it names as secondary. Reading those annotations
while the application boots turns every BPMN process into an aggregate class, and that class into
VanillaBP's answer about the store.

Two workflow services may declare the same process, one per generation of the model. They are
written for the same aggregate then. Two aggregates on one process would make the store of a report
depend on which class was scanned first, so that ends the boot.

The registration of a workflow module belongs to no aggregate at all and is written in a transaction
of its own, so any store carries it correctly. It has to be the same store after a restart, because
the same registration sitting in two stores would be sent twice. The store taken is the one of the
module's first aggregate by class name. The module's aggregates are known, because VanillaBP says
which module deployed which BPMN process while it wires them.

What is left of decision 12 is its point: the extension never attributes a store itself, it asks the
platform's resolver. What changes is that an application with two persistences no longer has to
choose between the Business Cockpit and its own architecture. A report whose BPMN process belongs to
no workflow service is still refused, and the message names the declared processes, because nobody
can say where that report goes. An application with a single store is spared even that. With one
store there is nothing to decide.

### 14. The configuration keeps the version 1 keys

An application upgrades to version 2 by changing a dependency. That the Business Cockpit is now an
extension of the VanillaBP platform is a detail of this repository. No application should have to
rewrite its configuration to learn about it. So the keys stay the ones version 1 read, in the places
it read them: `vanillabp.cockpit` for what holds for the whole application,
`vanillabp.workflow-modules.<id>.cockpit` for one workflow module, and
`vanillabp.workflow-modules.<id>.workflows.<process>.cockpit` with its `user-tasks.<task>.cockpit`
for the two levels below. Lists are lists again, and the group hierarchy is a map of lists again,
because that is how an application already wrote them.

Neither platform reads anything below `vanillabp.cockpit` by itself, so the extension binds that
tree. On Spring Boot it is a `@ConfigurationProperties("vanillabp")` overlay, on Quarkus a second
`@ConfigMapping(prefix = "vanillabp")`. That is the pattern the platform documents for an adapter
which contributes keys of its own. Both hand one neutral object to the core, and the core parses and
validates it. So a port which is no number, or a timeout which is no span of time, gets the same
message on either platform. On Quarkus the mapping is also what lets the application boot. A key
below `vanillabp` which no mapping declares ends the startup there, which is why every key the
cockpit reads is declared, including the one key its Process-Engine-API half reads.

A misspelled key runs into that strictness, and so does a version 1 key which is gone. On Quarkus
the build ends and names the key, together with the key it was nearest to or with whatever became of
it. On Spring Boot such a key is ignored. That difference is the platform integration's decision
about the two frameworks, not this repository's.

These keys do not come back. One reason each:

- `rest.log`: the client logs through SLF4J, so the logging configuration switches it on. Set the
  logger `io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi` to `DEBUG`.
- `rest.additional-get-parameters.<name>`: it appended query parameters to GET requests, and every
  report is a POST.
- `rest.retry.enabled`, `.max-attempts`, `.period`, `.max-period`: a failed report waits in the
  outbox and is repeated from there. `vanillabp.outbox.*` configures that.
- `kafka.group-id-suffix`: it made a consumer group unique, and the extension only produces.
- `jwt.hmacSHA256-base64` and `jwt.cookie.*`: version 1 bound them and read them nowhere, and the
  cockpit server configures its own tokens.
- `group-hierarchy-bean-name`: the hierarchy is configuration, and which groups may see a workflow
  module is answered by the `WorkflowModuleDetailsProvider` bean.
- `workerId`: it named the instance a report came from, and the extension takes the host name for
  that and asks for nothing.
- `spring.application.name`: only the Camunda 8 exporter path read it, and that path is gone.
- `spring.kafka.consumer.*` and `camunda.zeebe.kafka-exporter.topic-name`: version 1 also consumed a
  topic a Camunda 8 exporter wrote, and version 2 learns the same from the listeners it puts into
  the models.
- The Camunda 8 keys below `vanillabp.workflow-modules.<id>.adapters.camunda8`: a worker of the
  cockpit is a worker on the same cluster, and it is opened the way the Camunda 8 adapter opens its
  own, so the adapter's keys are read rather than copied. `workflow-visibility-timeout` is gone
  together with the waiting it configured. A report about something the cluster has not exported yet
  goes back into the outbox, and `vanillabp.outbox.block-after-attempts` says how long it keeps
  coming back. The adapter's key of that name is read in the one place where the extension does wait
  after all: on a Camunda 8.8 cluster a listener job has to ask for the call hierarchy of its
  process instance, because the job does not carry its root, and the answer comes from the same
  lagging storage the adapter waits out. The time between two attempts of a report is not that
  number. It stays the extension's own, because it is multiplied by the attempts the outbox allows:
  ten seconds there would mean five hundred seconds of waiting per report.
- `io.vanillabp.businesscockpit.tasklistener.prefixes`,
  `io.vanillabp.businesscockpit.executionlistener.prefixes` and `io.vanillabp.deployment.priority`:
  entries of an in-memory map of version 1's deployment, never keys of a configuration file. A job
  type of this extension is recognized by its own prefix.

One behaviour of version 1 comes back with its keys, and it is worth naming. The token client of the
client-credentials flow configures its own connection. It inherits nothing from `rest.*`. So an
authorization server behind another proxy, with another certificate or a slower answer, stays
reachable. What the flow does not say for itself falls back to the default, not to what the cockpit
server's client uses.

Where the keys stand is the cockpit's business. How the levels are asked is not. VanillaBP walks the
levels of every plug-in the same way (decision 53 of `adapter-platform-integration`), and the
cockpit hands it one section per level instead of writing a second walk. The names stay
`vanillabp.cockpit.*`, and the order lives in one place for every plug-in. That walk also knows a
position per configured adapter at every level. This extension does not use it yet: nothing binds
those keys, and a position nothing binds is not one an application can write.

Two keys move into the cockpit's own tree instead of being read out of Spring's.
`kafka.bootstrap-servers` and `kafka.properties.*` are what version 1 took from
`spring.kafka.bootstrap-servers` and `spring.kafka.producer.*`. The extension builds its own
producer now, on both platforms. One key is new with the Process-Engine-API integration:
`process-engine-api.remembered-user-tasks` sizes what that half remembers between a delivery and its
dispatch.

What was mandatory per workflow module stays mandatory per workflow module, and there is no global
default for it. Version 1 had none either. A `ui-uri-path` written once for every module would
promise something the cockpit cannot keep, because the modules answer at different addresses. Every
setting is read and validated while the application starts, not when the first event arrives, so a
developer works from the log of the first boot.

### 15. The extension is handed the transaction of the workflow aggregate, never one of its own

An outbox entry of the extension has to be written in the very transaction which persists the
workflow aggregate it reports about. The extension cannot say which transaction that is. An
application may have contributed a `TransactionRunnerAware` bean for that aggregate, or a runner
which serves every aggregate no aware bean covers. Neither is visible to something which compiles
against no platform integration.

So the extension injects `TransactionRunnerResolver`, a bean of both platforms, and asks
`#resolveFor(workflowAggregateClass)` for every entry. That is the same question the process
services ask, and where the application contributed nothing it is answered with the platform's own
runner. One entry belongs to no aggregate, the registration of a workflow module. It asks for the
root of the type hierarchy, which is the runner serving every aggregate nobody claimed.

The two runners the platform modules used to carry are gone with it. They opened a transaction of
their own, and that was the defect, not the duplication. Wherever an application had a runner of its
own, the entry then rode a different transaction than the aggregate. The copies also dropped
`beforeCommit`, the rollback-only verdict and the recognition of an optimistic-locking failure. A
caller which promised a running transaction and brought none now reads the platform's refusal
instead of one of ours, and that is the same message a workflow task produces.

### 16. What the application declared is VanillaBP's answer, per workflow module and BPMN process

Decisions 7 and 13 both rest on the `@WorkflowService` annotations of the application: which
aggregate a BPMN process works on, and what a method wrote into the reserved `version` attribute.
The extension used to read those annotations itself, once per platform, unwrapping whatever proxy
Spring or ArC had put around the bean. That second reading never saw the same methods as VanillaBP's
own.

It asks instead. `ExtensionHandlers#bpmnProcessesOf` names the BPMN processes a workflow module
holds, and `#workflowAggregateOf` names the aggregate one of them works on.
`HandlerContract.Builder#validatingAnnotation` runs the `version` check while the scan still holds
the method, so the refusal names the annotation, the class, the method and this extension.

Three things follow.

The store of an event is looked up by the workflow module AND the BPMN process. Decision 13 keyed it
by the process alone. Two modules of one application may serve a process of the same name, because a
module is the boundary which makes that legal, and their aggregates may live in different
persistences. So the old key wrote the reports of one module into the other's store.

A provider which is not public is no longer refused here. It is not wired either way, and
VanillaBP's startup report about the handler methods nobody sees names it. That is the answer the
defect deserves.

And the refusal of decision 13 does not come back. Two workflow services which declare one BPMN
process of one workflow module for different aggregates are VanillaBP's case to decide. It serves
the process with the class it found first, warns about the other, and tells an extension about that
first class alone. So the entry is written for that class, and a report naming another one is served
with it instead of being refused. Nothing is logged about it either. The boot said what there was to
say about the other class long before the first report was written.

The same rule holds everywhere else in this extension. It validates the way the core and the
adapters do. What they let pass with a warning it lets pass with a warning of the same kind, and
what ends the boot there ends the boot here. Seeing a defect from another angle does not make an
extension stricter, and being optional does not make it more lenient.

### 17. A details provider is asked a question, so the platform saves nothing after it

A `@UserTaskDetailsProvider` and a `@WorkflowDetailsProvider` are called to fill a report. They
answer what the cockpit should show, and answering a question does not change a case. VanillaBP
saves the workflow aggregate after a `@WorkflowTask` method returned. It does not save it after a
details provider returned, on any of the three ways a provider is reached: the report of a user
task, the report of a workflow, and the read behind `BusinessCockpitService.getUserTask`. Only the
last of them used to say so.

The old behaviour cost concurrency in a place which does not need it. A report is dispatched after
the engine's transaction committed, on the outbox's own thread, while the application may be working
on the same case. A save which belongs to the reporting run then competes with the writes of the
workflow itself, and one of the two reads the version conflict.

An application which really wants to change something in a provider calls `save` itself and carries
what follows from it. That differs from a `@WorkflowTask` method on purpose: the one is meant to
write, the other is not.

Only the save of the platform is switched off. A persistence which writes the changes of a managed
object by itself, which is what JPA's dirty checking does, still writes them when the transaction
commits. So the sentence for the reader is not "a change here has no effect". It is "change nothing
here, and if you do it anyway, it is your change with your consequences".

Two stricter ways were weighed and left alone.

The first was running the provider in a read-only transaction. Under JPA that would make the promise
true, and it would break what is allowed: an application which deliberately saves in a provider, and
every other write the same transaction carries, the outbox entry of the report among them. A
reporting run which may not write cannot record that it reported.

The second was detaching the aggregate before the call. That hides the writes, and it hides the
aggregate with them. A detached JPA entity throws as soon as a provider touches a lazy association,
which is what reading a case usually comes down to. A rule which turns the ordinary use of the
parameter into an error is worse than the leak it closes.

### 18. The timestamp of the event decides which report the cockpit stores - the state a report carries superseded by decision 26, what waits for its creation changed by decision 45

VanillaBP's outbox gives its entries no order. It dispatches them in parallel, and an entry whose
dispatch failed comes back after entries planned later have gone through. So the reports about one
user task or one case reach the cockpit in an order which is not the order they happened in. A
change can arrive after the change which followed it, and an end can arrive before the creation it
ends.

The cockpit weighs every report against what it already holds. It weighs by the timestamp of the
event, not by the moment the report arrived. A report is built while its outbox entry is dispatched,
which is as late as the outbox happens to get to it, so the arrival says nothing about the order. A
user task and a case each carry `latestEventAt`, the timestamp of the event behind the latest report
the cockpit stored. The next report is weighed against that. `updatedAt` cannot serve for it,
because it is audit information and every save overwrites it with the cockpit's own clock.

Four rules follow. The decision is made in those two services and not in a controller, so the REST
way in and the Kafka way in are held to it alike.

A report older than what is stored changes nothing. An end is never undone, so nothing which arrives
after it clears `endedAt`. An end of a task or a case the cockpit does not hold creates it, ended.
The creation may still be waiting in the outbox, and a dropped end would leave a task the cockpit
shows as open for good. A creation is the one report an older timestamp does not disqualify. Where
the cockpit knows a task from its end alone, the creation fills in what the end could not report. It
is the oldest report there is and the only one which says when the task began, so the end stays as
it is and everything it left empty is filled.

A record the cockpit holds from an end alone is recognizable by its empty `createdAt`. An end does
not say when a task began, and guessing it would make the same case look different depending on
which report came first.

Which state a report carries is a different question, and this decision leaves it alone. The state
is read while the entry is dispatched (decision 3), so it is the state of that moment and not the
state of the event. Which fields of a report the cockpit then writes is decision 19. The reports
also keep arriving in whatever order the outbox produces.

### 19. An end overwrites what it reports, and a field it left empty counts as not reported

An end carries the same fields as a change. That is what lets the list of finished work show the
data a case was finished with. A BPMS does not always still have those fields. Camunda 7 reads an
ended task from its history, and history keeps no variables. The Process-Engine-API answers out of a
store which may have been cleared by the time the report is sent. The report goes out anyway,
because a completion which never arrives leaves a task the cockpit shows as open for good. Such a
report then carries little more than the identifiers and the timestamp.

Such an end overwrites nothing. Every field it does not report keeps the value the cockpit holds,
and every field it does report replaces it.

Each mapper used to list field by field what an end may not write. Whatever nobody had thought of
fell through MapStruct's default and was written as `null`. What fell through was the due date the
task list sorts by, the titles a task is found under, the business id, the version of the process,
the sub workflow, the comment, the way notifications are to be delivered, and for a case who started
it and who may open it. Only the business data was held back.

A field which was not reported usually arrives as `null`, and there the mappers say the rule with
MapStruct's `IGNORE` strategy. Maps and lists arrive empty instead. A report builds its collections
whether it fills them or not, and over Kafka it could not do otherwise, because protobuf gives a map
and a repeated field no presence information. So an empty collection counts as nothing reported.
Both ways in answer alike. A workflow module writing to a topic reports what one calling the server
reports, and a rule which held on one of them only would turn the choice of transport into a
business decision.

The price is a report which can no longer empty a collection. Once the cockpit holds an answer, a
workflow module cannot say that a case has lost its title, or that nobody may open it any more. The
other way round would be far worse. A BPMS which has forgotten the case would erase what the cockpit
knows, at the moment somebody opens the finished list to look at it. A module which really wants the
readers of a case narrowed reports that as a change, before the end.

One field is cleared by an end on purpose. Who ended a user task is written as reported, and nobody
reported means the process ended it. The notification poller reads that field to tell a completion
by somebody else from one the reader did themselves, so a name left over from an earlier report
would name the wrong person.

### 20. The warning about a writing details provider is VanillaBP's, and this extension adds none - the warning reaching a cockpit application superseded by decision 23

A `@UserTaskDetailsProvider` and a `@WorkflowDetailsProvider` run while a report is dispatched, and
they are allowed to change the workflow aggregate. VanillaBP does not save after them (decision 17).
But a persistence which writes the changes of a managed object by itself still writes them when the
transaction of that dispatch commits. That transaction read the case before the application changed
it. So a case which cannot notice a second writer loses the application's change without a word. It
is the same defect as two branches of one model writing one aggregate, with the report in the place
of the second branch.

VanillaBP says so while the application boots, over the handler contracts an extension registered.
Take an application whose details provider serves a BPMN process, and whose aggregate notices no
concurrent change. It reads a warning which names that module and that process and says what to do
about it. A test on each platform pins that the warning reaches an application with the Business
Cockpit.

The extension adds no check next to it. The condition is the platform's to answer. Whether a
persistence notices a second writer is `AggregatePersistenceAware#detectsConcurrentModification`, a
method of the application's own persistence, and an extension which compiles against no platform
integration reaches neither the bean nor its answer. What it could read instead is the version
attribute on the class. That is the DEFAULT behind the method, not the method itself. A check of our
own would therefore warn about a store which notices a second writer some other way and said so, and
being stricter than the core is what decision 16 rules out. Two warnings about one aggregate would
also say one thing twice.

A declaration that these handlers only read would switch the warning off, and here that would be
wrong. What VanillaBP stopped doing is saving. A provider which writes anyway is still written to
the database by the persistence holding it. So this extension says that VanillaBP need not save
after a details provider. It does not say that a details provider cannot write.

What the platform's message cannot carry is the half which belongs to this repository: VanillaBP
does not save the provider, and the provider is a second writer all the same. That sentence is in
the wiki page `Architecture`, next to what a report carries.

### 21. A question of the service works in the caller's transaction, a report demands one

`BusinessCockpitService` offers two shapes in one interface. `aggregateChanged` reports something,
`getUserTask` asks something. They looked alike and behaved differently. The report was written into
the transaction the caller was in, while the read ran in a transaction of its own. So an application
could report a change and, in the same method call, read an answer which did not know that change.
Nothing in the interface said so, and nobody can guess it.

The read now runs in the form the platform calls `CURRENT_OR_NEW`: the transaction running on the
calling thread, and one of VanillaBP's own where nothing runs. Both halves are needed. Joining the
caller's transaction is what makes the answer match what the caller sees. The aggregate the details
provider is handed is then read in the caller's unit of work, which under JPA is the persistence
context holding the change nobody has written yet. Opening a transaction where nothing runs is what
keeps a REST controller answerable, because such a controller reads a task without a transaction of
any kind. The third form demands a running transaction. It would refuse that controller, and a
question may not do that.

One transaction wraps the whole read, not the aggregate alone. The BPMS is asked which task it
holds, then the aggregate is read for the provider. Two units of work would let those two describe
two different moments. On a BPMS whose store is a database of the application, which is the
Process-Engine-API, both reads then also sit in the caller's transaction instead of beside it.

A caller who brought no transaction pays for that with one open connection, for as long as the
engine takes to answer. For a remote engine that is an HTTP call. The exchange is worth it. A reader
who has to work out which of two moments an answer came from is worse off than one whose read is a
little more expensive. And a caller who already runs a transaction pays nothing, because the engine
was asked inside that transaction before.

Exactly one method of the service reads, so exactly one changed. The two reporting methods keep the
running transaction and nothing else, for the reason decision 15 gives: the entry belongs in the
transaction which persists the change it reports, and an entry committed on its own would tell the
cockpit of a change which is still free to roll back.

What they gained is a refusal of their own. The platform refuses a missing transaction with a
sentence about a BPMS half reporting from a worker thread. That is the right sentence for an adapter
and the wrong one for a workflow service which calls this interface. So the service asks whether a
transaction is open before it writes, and says what the application has to do. A runner an
application contributed itself may answer that it cannot tell, and then the refusal is left to that
runner.

Taking part in the caller's transaction has a price, and it is paid knowingly. A details provider
which changes the workflow aggregate now leaves that change where the caller will commit it, and a
persistence which writes the changes of a managed object by itself writes it there. So wherever a
provider writes on the read path, reading can end in a version conflict at the caller's commit.
Decision 17 weighed the two ways out and they stay rejected: a read-only transaction breaks the
outbox entry a reporting run has to write, and detaching the aggregate breaks every provider which
touches a lazy association. What is left is the sentence in the Javadoc of `getUserTask`, which a
provider reached by a read has to read: read only.

### 22. The transport is a bean an application replaces, and each platform answers whether it did

Version 1 declared its three publishing beans with `@ConditionalOnMissingBean`, and applications
used that seam to report their own way. Version 2 built the transport inside
`BusinessCockpitAssembly.transportOf` and handed it to the extension, so there was no bean left to
replace. Nobody decided that, it just happened, and a customer who used the seam could not upgrade.

The transport is a bean again. Spring Boot declares it `@Bean @ConditionalOnMissingBean`, Quarkus
`@Produces @DefaultBean`. The one interface carries the three methods version 1 spread over three
beans. The seam sits at the end of the outbox dispatch, not where the BPMS event arrives. So a
transport an application wrote is handed a finished report, and it inherits the repetition with a
backoff and the transaction of the workflow aggregate.

So `BusinessCockpitTransport` is a published contract, like the interfaces of
`io.vanillabp.cockpit.extension.spi` which the three BPMS halves implement. It lives in
`io.vanillabp.cockpit.extension.transport`, because that is where the two shipped transports are and
moving it would break the applications this decision is for. `BusinessCockpitConfiguration` is a
bean of both platforms for the same reason. An application which wants to wrap a shipped transport
builds it with `BusinessCockpitAssembly.transportOf(configuration)`, and without that bean it would
have to reimplement the transport instead of adding to it.

Whether an application brought a transport of its own is the platform's answer, not a property key.
`readAndValidate` takes it as an argument. The check stays in the neutral core, the message comes at
the same moment as every other configuration message, and no key can claim a bean which is not
there. Spring Boot reads the bean definitions of the type and counts the ones it did not contribute
itself. Quarkus asks the container for the beans of the type and looks for one which is not the
default bean. Both answers were settled while the application was built, and neither creates an
instance. The shipped transport loads a Kafka client an application may not have, and it is built
from the very configuration being read.

What such an application reads while it boots follows from that. Neither shipped transport is
missing any more, because the reports have a way out. A shipped transport configured next to a bean
of the application is named in one line, as a key the extension does not read. Saying nothing about
it would be the worst of the three answers. Both shipped transports next to a bean of the
application still end the boot. A shipped transport is what an own one wraps, and nothing says which
of the two was meant.

### 23. The details providers say while they are wired that they never write

VanillaBP warns while an application boots about a handler it may save the workflow aggregate after.
The warning is about what a handler is allowed to do, not about what one did, so it reached the
details providers of this extension as well. Nothing is saved after them any more (decision 17).
Every application with the Business Cockpit would have read that warning at every start, about
something which cannot happen. A warning which is always there is one people stop reading.

The statement sits on both handler contracts now, which is where VanillaBP can read it. VanillaBP
wires the methods long before anybody calls one, and the check about a second writer runs while it
wires them. The opt-out every call used to carry goes with it. The contract says the same thing
earlier and for every call, and two places saying one thing is one place too many.

The platform wrote the same rule down from its side, and it names the Business Cockpit as the case
the statement was built for. So a reading extension has a way of saying what it is, and the check
has a reason to pass over it.

Nothing changes for somebody writing a provider. A provider which writes the aggregate anyway is
written to the database by a persistence which writes managed objects by itself, and an aggregate
without a version attribute still loses the application's change without a word. What ended is the
report at every start, not the effect.

The warning is still alive for a handler which may write. An extension which registers a contract
without that statement is reported exactly as before. A test of this repository boots such an
extension next to the cockpit and reads both answers: the line about the writing handler, and no
line about the details providers.

### 24. Both details providers pick their method by the version of the deployed process

The `version` attribute of `@UserTaskDetailsProvider` is read, `@WorkflowDetailsProvider` has one
too, and both go through the selection VanillaBP's own `@WorkflowTask` goes through. This replaces
decision 7, which refused a value for the one attribute which existed.

What changed is the event. A task listener still says which task fired and not which model it came
from, but the reference an event travels in carries the version the BPMS reported, and the platform
takes it with the call. Camunda 7 knows the version of every process definition, Camunda 8 reports
it with every job, and an engine behind the Process-Engine-API fills a version tag where it wants
to. So there is something to pick a method by, wherever the BPMS says anything at all.

Nothing of the selection is built here. The platform holds the version ranges of one handler method
in one place and answers the three questions about them, for its own three annotations and for the
method of an extension alike. This extension names the attribute its versions stand in and says that
its calls carry a version. Which range covers which deployment, how a version tag is resolved and
when two methods are a duplicate is the platform's answer, and it is the same answer a
`@WorkflowTask` method gets.

The version travels with the outbox entry although the dispatch reads everything else again. An
entry names its event, and the version is part of that name: a deployment made while the entry
waited must not move the event to the method of the newer model.

Three things follow.

A method naming no version serves every version, so an application which never wrote the attribute
reports what it always reported. An application which wrote it under version 1, where it was
documented and never read, gets what it believed it had.

Without a version from the BPMS only a method naming no version runs. A call without a version is
served by the methods which name none, and a method which names one is named while the application
boots rather than waiting for an event which never comes. That is the platform's report about a
method which serves no deployed version, and it is a warning and not the end of the boot: a version
which is not deployed yet is a normal state during a rolling deployment.

A method of a details provider does not take the version range of its `@BpmnProcess`. A
`@WorkflowTask` method does, because that range says which generation of a model the workflow
service runs. Reporting to the cockpit is not running the workflow, and a range meant for the one
would silently narrow the other.

### 25. A report is to carry the state of its event, and the auditing id is the way there - superseded by decision 26 on 2026-09-17

A report is built when the outbox entry is dispatched, not when the event happened (decision 3).
Between the two the case can move on, so a report is right about the state of now and wrong about
the state the event left behind. VanillaBP offers two ways out of that, and this entry says which
one the Business Cockpit takes.

The first way is to carry the report in the entry. An outbox entry can hold bytes, which VanillaBP
keeps in a store of its own and hands back at the dispatch. The second way is to carry the name of a
state. An application which keeps a history of its workflow aggregates answers
`AggregatePersistenceAware#getAuditingId` with the state an aggregate stands at, and
`loadByIdAndAuditingId` gives that state back later.

The cockpit takes the second way.

The first one asks for something this extension cannot do. A report is not the workflow aggregate
alone. The assignee, the candidates, the due and the follow-up date, the business key, the version
of the deployed process and the variables a `@TaskParam` binds all come from the BPMS, and the
extension asks the BPMS half for them while the entry is dispatched. The details provider runs after
that, on the event those answers filled (`BusinessCockpitExtension#dispatchUserTaskEvent`). To put
the finished report into the entry, the BPMS would have to answer in the transaction of the event. A
remote engine cannot promise that. Its read model runs behind the engine, which is why a half which
may know the task in a moment asks for the entry to come back later. At the moment the entry is
written there is no entry to come back.

Building the report early would also end what decision 3 bought. Several pending reports about one
task collapse into one because they would all say the same thing. Reports which each carry their own
bytes say different things, so none of them may be dropped.

The choice belongs to the single call and not to a setting. Every report this extension plans is
about an event, and every one of them asks for the state of that event. There is nothing to switch
on, and no report of this extension asks for anything else.

What the BPMS answers keeps the state of the dispatch, and that is wanted. The assignee of a task
and the dates on it are facts of the engine and not of the workflow aggregate. The engine keeps no
history the extension could ask for by an auditing id, so these fields say what the engine says at
the moment the report goes out. The wiki page `Architecture` is where a reader finds it.

The extension cannot take this way yet, and the missing half is the platform's. When it plans an
entry it holds the id of the workflow aggregate and not the aggregate, so it has nothing to ask
`getAuditingId` about. A report of a BPMS event never has the aggregate in hand. When it dispatches
an entry it does not load the aggregate either: VanillaBP loads it inside the handler call which
runs the details provider (`ExtensionHandlerRegistry#invoke`), and a `HandlerCall` carries no
auditing id. The one door to `AggregateServiceContext`, which has both methods, is
`AggregateServiceFactory#createService`, and the service bean behind it is built on both platforms
only when an application injects it. So the platform gets an auditing id which can be asked for by
the id of an aggregate, and a handler call which can be told to load the state of the event. This
extension follows once it can.

### 26. The report is built at the event and travels with its entry - narrowed by decision 43, which builds a change the BPMS half cannot name right away when its entry is dispatched, and by decision 46, which does the same for a changed user task

This replaces decision 25. A report carries the state of its event, which 25 already decided, and it
gets there the other way: the report is put together while the BPMS event is being observed, and it
travels as the payload of the outbox entry. The dispatch sends what the entry carries and asks
nobody anything.

Decision 25 chose the auditing id because a report was thought to need an answer from the BPMS which
only a later read could give. That premise is wrong. What a BPMS says about a user task is an image
of what the application decided: the values reach the model, go to the application as
`PrefilledUserTaskDetails`, and may be changed there. Everything a report needs is at hand in the
event which triggers it.

Measured on 2026-09-17, in the three BPMS halves. The Process-Engine-API half already builds the
whole prefill at the moment of the delivery and parks it until the dispatch reads it. The Camunda 7
listeners are built-in task listeners which run inside the engine's own command, where every field
of the task is there without a single query. On Camunda 8 the activated job carries assignee,
candidates, due date and follow-up date, and the two BPMN names come from the model the adapter
deployed itself. A user-task report written as JSON is about a kilobyte, and a payload may be a
mebibyte.

What this buys is the case which used to lose data. While the cockpit server is down the entries
pile up, and every one of them used to be read hours later against a case which had moved on. Now
each of them says what was true when it was planned.

A failure while the report is built is meant to disturb. The details provider runs in the
transaction of the event, so a failure there fails the engine's work, and on Camunda 8 the listener
job holds the transition until it is answered. On the Process-Engine-API the failure leaves the
observer the adapter calls and fails the delivery, so the engine offers the task again until the
provider works. The end of a task is the one event that engine does not repeat, and decision 11 of
`businesscockpit-process-engine-api-adapter` says what that costs. Only reading happens here, but
what is read has to be right, and a defect which repetitions hide is a defect nobody fixes. The way
to the cockpit server is the other half and stays quiet: it runs over the outbox, and a cockpit
which is down for ten minutes is not worth a single error in anybody's log.

Corrected on 2026-09-18. The sentence about the end of a task puts two events into one pot, and the
Process-Engine-API keeps them apart. A completion ends a task because somebody asked the engine to
finish it, and a cancelation is the engine taking the task away, say through a boundary event.
Measured against the API's reference implementation for an embedded Camunda 7 and against the test
application of that half: a completion reaches the engine through a phase-two outbox entry, so a
report which fails there fails that entry, and the entry comes back. An engine which takes part in
the transaction of the dispatch even hands the task back, and the next attempt reports the end. A
cancelation is the one this extension gets a single shot at. Decision 12 of
`businesscockpit-process-engine-api-adapter` holds the numbers, and those two words are the ones to
use in every text of ours about tasks.

The idempotency key stays exactly as decision 4 wrote it, and only the direction of the discard
turns around. An entry used to be the intention to report, so the one which waited was as good as
the one which came after it, and the newer call was dropped. An entry carries its report now, so
dropping the newer call would keep the oldest state and a user would read the first report of a step
instead of the last. The reports of the two lists are therefore planned through
`PhaseTwoOutbox#scheduleReplacingWhatIsStillWaiting`, which puts the youngest call in the place of
the waiting entry, payload included. Waiting reports of one task still collapse into one, which is
what keeps a backlog from becoming a flood of notifications. Taking the step into the key would do
the opposite, because then every step would keep an entry of its own. An entry a dispatch has
already claimed is not replaced, so two reports arrive where one was asked for, and the timestamp of
the event holds the older one back at the server (decision 18).

The registration of a workflow module is planned the old way, with `schedule` and without a payload.
It says that a module exists and where it answers, which is read from the configuration and is the
same whenever the entry goes out. A second registration of one module says what the waiting one
says, so keeping the waiting one is right.

An entry which outlives its own payload is sent with its identifiers alone, and a line in the log
names the case. The retention of the outbox counts at the entry, which the platform decided on
2026-09-23: an entry which stands in the outbox keeps its payload, waiting or blocked, a dispatched
entry loses entry and payload together after `vanillabp.outbox.retention`, and a payload no entry
names any more is swept by age. Waiting is therefore no longer a way to meet this. What is left is a
write which never committed between the entry and its payload, which is possible where MongoDB
writes outside a transaction. It is the same answer this extension gives an end whose BPMS said
nothing: the cockpit keeps what it stored before, and a report which never arrives would leave a
task open in the list for good.

Two sentences of older entries stop being true with this. The event is no longer built when the
entry is dispatched, which is the second half of decision 3, and what the BPMS answers is no longer
the state of the dispatch, which is the closing paragraph of decision 18 and a paragraph of 25. Both
entries keep their number and their text and say in their headline which part of them this entry
replaced.

One effect is worth naming, because the wiki promised the opposite. A details provider which writes
the workflow aggregate used to be a second writer: it ran in the transaction of the dispatch, long
after the event, and wrote over what the application had changed in between. The provider runs in
the transaction of the event now. Where that transaction is the application's own, which is an
embedded engine and every report through `BusinessCockpitService`, the provider's write is the
application's write and there is no second writer left. Where the event arrives on a worker thread
of a remote engine, the extension opens a transaction for it, and a provider which writes there is
still a writer beside the application. A provider is asked a question and answers it, which is what
decision 17 says, and that is still the way out of both cases.

### 27. The description of a cockpit user interface lives in this repository

Version 1.0 does not ship a user interface per framework any more. It ships a description of what a
cockpit user interface has to do, precise enough that somebody can build one from it without
reading the React sources. The React application stays in the repository as the worked example of
that description.

The description is written in the skill format, as a `SKILL.md` with pages below `references/`, so
an agent can load it. It does not live in `vanillabp/skills`, where the other VanillaBP skills are.
It lives here, in `skills/business-cockpit-user-interface`.

#### Why not in the skills repository

The skills repository has one rule it stands on: a skill links the documentation instead of copying
it, so an agent ends up in the current wiki and not in a snapshot of it. A description of the GUI
API, of the module federation contract and of the types of `@vanillabp/bc-types` cannot follow that
rule. There is no other document it could link. It *is* the document, and what it describes is the
surface of this repository.

A copy of that surface kept in another repository goes stale on its own schedule. Nobody changing
`UserTaskService` here opens a pull request there, and nothing fails when the two drift apart. Kept
here, the description sits next to the code it describes, a change which breaks it is visible in
the same diff, and the reviewer of that change is the person who knows.

So `vanillabp/skills` gets a skill named `business-cockpit` which points here for everything about
the user interface. An agent finds the skill, and the skill leads it into this repository. That is
the rule of the skills repository applied, not bent.

#### Why the directory is named after the user interface

The directory is `skills/business-cockpit-user-interface`, not `skills/business-cockpit`. Two
reasons.

The name of a skill is matched against a task, and an agent should load this one when somebody
wants a front end, not whenever the Business Cockpit is mentioned. A skill called
`business-cockpit` living here would compete with the skill of the same name in `vanillabp/skills`,
and two skills with one name in one installation is a problem nobody needs to have.

And `skills/` here is a directory, not a repository. Other descriptions of this kind will follow,
for the cockpit server or for an extension of it, and each gets its own directory next to this one.
A directory named after the product would have taken the whole space.

#### What this means for maintenance

The description is a published surface of this repository, like the OpenAPI specifications and like
the wiki. Three consequences:

A change to the GUI API, to the module federation contract or to the types of the NPM packages is
not finished while the description still says the old thing. The description names the files it
describes, so `grep` finds what to read.

Where the description and the code disagree, the code is right and the description is the defect.
The description says so itself, at the top. That is the same rule the wiki follows.

Nothing in the description is allowed to be an unproven promise. Every sentence about behaviour in
it was read out of the code, and where that reading found nothing, the text names the gap instead
of guessing. A gap somebody can see is cheaper than a promise which turns out to be wrong, because
whoever builds a user interface from this text has no second source to check it against.

#### Where this decision is referred to

- `skills/business-cockpit-user-interface/SKILL.md`, the paragraph saying why the text lives in
  this repository.
- the skill `business-cockpit` in `vanillabp/skills`, which points here instead of repeating.
- the wiki page `Customizing the user interface`.

#### Still open when this was written

The text is German until it has been reviewed, and the translation into English is work of its
own. Front matter stays English, because an agent matches a task against the `description`
field. Every file of the description says this in its first lines, and those lines go away with the
translation.

### 28. An initiator is always set, and the application is the only one who knows it

The initiator is the user who caused something. A process started from a button in a user
interface has the person who pressed it as its initiator, and so does a user task which somebody
assigned to a candidate group or took for themselves. An action no user caused has no initiator,
and the answer for it is the constant `system`.

#### Why no BPMS answers this

Camunda 8 names nobody. The Process-Engine-API names nobody. Camunda 7 has
`HistoricProcessInstance#getStartUserId`, which `Camunda7CockpitBridge` reads, and the engine
fills it only where the application set an identity context of its own. None of the three says
anything about who changed a user task.

VanillaBP cannot fill the gap either. A details object is prefilled at the moment of the event,
and by then the security context of the logged-in user is gone. It is gone on a retry as well, so
there is no later moment which would know more. The application is the only party which can carry
the user from its own request into the report, and it can only do that by writing the user down
somewhere the report can read, which is the workflow aggregate.

#### Why it has to be forced

The cockpit is hard to use without it. A list of cases which does not say who started them, and a
list of tasks which does not say who last touched them, leaves a reader with nothing to sort by
and nothing to recognize.

What makes this different from other missing values is that it cannot be added afterwards. Once a
case has run without an initiator, nobody knows who started it. Switching the field on later means
new cases carry it and the older ones stay empty for good, which is a split nobody can explain to
a user. A developer who forgets this and finds out a year later has lost that year.

So the field is mandatory, and after a details provider has run there is a value in every details
object. Never `null`. A provider which wants to throw away what an adapter prefilled sets the
constant, and silence stops being an answer.

#### How it is forced

Two layers, because neither alone is enough.

The first is the start of the application. A property with no default, `initiator-source`, says
which of the two ways this workflow module goes: `by-application` where the application sets the
initiator itself, or `system` where this module knows no user-triggered action at all. The key is
named after where the value comes from, not after the value, because what it settles is who
answers and not who the initiator is. It resolves at three levels, the
application, a workflow module and a single workflow, most specific first, like every other
multi-level key of this extension. A module which reports to the cockpit and answers nowhere is a
defect, and the application does not start. The message names the module, says what the initiator
is, says that it cannot be reconstructed, and spells out both keys.

The second is the moment a report is built, and only where the first said `by-application`. If no
value stands after the provider ran, the extension throws. Since decision 26 the report is built
in the transaction of the event, so this fails the engine's work rather than only the delivery.
That is the intended weight. A missing initiator is not a report which arrives late, it is a case
which will never be able to name its initiator.

The second layer alone would miss the case which matters most. A details provider is optional, and
an application which has none still reports; a check living inside the provider would never see
the application which forgot to write one. The first layer alone would miss the provider which
exists and leaves the field alone.

The first layer is also what keeps the promise that defaults stay compatible with version 1. In
version 1 the initiator was optional, and turning that into an exception by changing a default
would be a silent change of behaviour. A property with no default is not a default. The developer
meets the question once, at the first start, and answers it for good.

There is no grace period. An application moving from version 1 writes one line of configuration,
and `UPGRADE.md` says which.

#### What stays untouched

The order is prefill, then provider, then the rule. An adapter fills what its BPMS knows, the
provider may set or overwrite it, and only what is still empty afterwards meets the rule.
Prefilling `system` before the provider would be wrong twice: the Camunda 7 value would have to be
overwritten back, and a provider reading the object would see a value nobody ever said.

One case is exempt. Where the BPMS answers nothing at all and the extension reports an end without
details, the provider never runs, and that report carries `system` whatever the property says. An
end has to be reported, and a configuration cannot answer for an event nobody was asked about.

One consequence is worth naming. A provider which deliberately calls `setInitiator(null)` to drop
a Camunda 7 value looks exactly like a provider which did nothing, and under `by-application` it
throws. The way out is the same constant, set on purpose.

The constant is `system`, the same string the cockpit server writes as `SYSTEM_USER`. The two live
in modules which do not see each other, so they stay two constants which name each other in their
Javadoc. `COCKPIT_USER`, which is `cockpit`, is a different answer and stays what it is: a change
the cockpit itself caused.

### 29. An empty answer of a BPMS is silence, and 1.0 says so in words rather than in a type

`BusinessCockpitService.getUserTask` promised that an empty `Optional` means the BPMS does not know
a task of that id. Two of the three BPMS cannot keep that promise. Camunda 8 answers out of a
searchable storage an exporter writes behind the engine, so a task born a moment ago is missing from
the answer in the same way a task which ended is. The Process-Engine-API answers out of what the
asking node was served, which is a second road to the same gap, and entry 7 of its `GAPS.md` already
says so. Camunda 7 asks its own engine inside the caller's transaction, so there empty really is
empty.

Measured on 2026-10-01, the window on Camunda 8 is 219 ms on 8.10.0-rc1, 649 ms on 8.9.21 and
1667 ms on 8.8.39, all on an idle machine with one cluster. Under an exporter backlog it is as wide
as the backlog.

#### What 1.0 does about it

No answer changes. What changes is what the two interfaces say about an empty answer, what the
Camunda 8 half writes into the log when it gives one, and what a test holds in place.

The javadoc of `getUserTask` says that empty means the BPMS said nothing about the task, that this
covers a task which ended and a task the BPMS has not published yet, and that a caller turning it
into "there is no such task" says more than it was told. The three `…OfAggregate` methods of
`BusinessCockpitBpmsBridge` say the same in their own words, and the type says that a half may not
answer an empty result to mean a task is over.

The two readers in `BusinessCockpitServiceFactory` carry a comment naming what an empty answer costs
and what it does not do, and two tests pin it: an empty answer reports nothing to the cockpit and
ends nothing, and a read of a task the BPMS says nothing about answers empty without reporting.

The Camunda 8 half logs both readings on the two paths which were silent, which is what decision 8
of that repository had already settled and only half applied.

#### Why there is no third answer state

A javadoc gives an application nothing to branch on. To let a caller tell "gone" from "not yet",
`getUserTask` would need a third state the way `WorkflowAwareness` has one. That is a shape in a
published SPI, so it is an open question and not a repair, and the release gate says the
cheap way has right of way for 1.0.

The cheap way is not a stopgap, because taking the third state later costs no more than taking it
now. Both interfaces can grow a question without breaking anybody.

`BusinessCockpitService` is implemented nowhere outside this repository. There is no `implements
BusinessCockpitService` in the whole workspace, and the only implementation is the anonymous class
`BusinessCockpitServiceFactory.createService` builds, so an application never writes that interface
and a method added beside `getUserTask` cannot break one.

`BusinessCockpitBpmsBridge` is implemented by the BPMS halves, and a question added there can be a
default method which answers "I cannot tell" until a half knows better. A half which never learns
stays correct.

So a third answer state is an additive option after 1.0, on both interfaces, and the words written
now stay true whether it is ever taken.

#### What was weighed and not taken

Asking the partition instead of the index on Camunda 8 answers in 12 to 21 ms, which is one to three
orders of magnitude faster than the index on this question. It is still the wrong question. An empty
`UpdateUserTask` answers existence and nothing else, while both callers need the process version,
the workflow id, the task definition and the BPMN element id, and reading those is what the index is
for. It also drops the promise that an id somebody guessed reads no task of another case, because
the aggregate's id is part of the index filter and no part of an update command. And it writes where
a read is expected: the command lands in the task's audit log, it fires a modelled `updating`
listener which changes nothing, and it holds the task in `UPDATING` while it runs, so every other
sender is refused for 60 to 145 ms. A screen refresh is the worst caller to pay that with.

Waiting out the export window inside the bridge was weighed too. A REST endpoint held open for up to
ten seconds is worse than a wrong 404, and the number to wait for lives on
`MigratableProcessService.workflowVisibilityDelay()`, which the cockpit bridge has no route to.

A tie-breaker probe, which searches the index and asks the partition once where the search found
nothing, is the one idea worth keeping. It costs a command only on the empty case and it answers
"is this task over" truly. It still produces no reference, so `getUserTask` could only say "I cannot
tell you". Whether a read may fire a modeller's `updating` listener is not settled, and the
Camunda 8 adapter keeps its whole-workflow probe off by default for exactly that reason.

### 30. A key of the cockpit application starts with `business-cockpit`

The cockpit application reads its own configuration below `business-cockpit`. That is the prefix of
`ApplicationProperties`. The other prefix, `vanillabp.cockpit`, belongs to the workflow module side,
where `ConfigurationKeys.GLOBAL_PREFIX` of `extensions-commons` names it. The two do not mix. One
configures the application which shows the lists, the other configures the extension which reports
to it.

The question came up because two `@Scheduled` placeholders named something else. The
collector of the update stream asked for `businessCockpit.guiSse.collectingInterval`, the follow-up
scheduler for `businesscockpit.follow-up.check-rate`. Both are now spelled the way the configuration
spells them.

A placeholder is not a bound property, so Spring's relaxed binding does not reach it. It is looked
up by its exact name. The lookup finds nothing and the default of the annotation is used, without a
word in the log. A wrong key here is therefore invisible: the application starts, the work runs, and
only the interval is not the one somebody configured.
`EveryScheduledPlaceholderNamesACockpitKeyTest` is what notices the next one.

The key of the follow-up scheduler was written down nowhere, neither in the wiki nor in this
repository, so spelling it correctly takes nothing away from anybody. Both keys are on the wiki page
`Configuration` now.

### 31. A change event is built without the document behind it

A notification about a changed user task or workflow is built from the change event alone. It
carries the kind of entity, its id and what happened to it. It does not carry the target groups any
more, and the change stream is not asked to look the document up.

#### What was measured

`ChangeStreamNotificationsTest` writes the same document three times and counts what arrives at a
stream subscribed the way the two services subscribe, without a lookup:

| write                                            | MongoDB operation | document in the event | notification before | after |
|--------------------------------------------------|-------------------|-----------------------|---------------------|-------|
| a newly reported task                            | `insert`          | yes                   | yes                 | yes |
| a claim, which Spring Data sends as `replaceOne` | `replace`         | yes                   | yes                 | yes |
| giving the task back, an `$unset`                | `update`          | no                    | no                  | yes |

Two of three before, three of three after. The workflow side counted three of three before as well,
because `WorkflowChangedNotification.build` already passed `null` for the target groups and never
touched the document.

The loss is narrower than it looked, and it is not a rare case. `UserTaskService.unclaimTask` and
`unassignTask` are the only two writes which send an update operator, and both of them are somebody
giving work back.

#### Why not ask for the document

The other way was to subscribe with `FullDocument.UPDATE_LOOKUP`, which MongoDB answers by reading
the document again after the change. That buys a value nobody reads. The target groups of an event
end up in `GuiEvent`, and `GuiEvent.getTargetGroups` has no caller: the filter in
`LoginApiController.updateClients` is a comment, and the `matchesTargetGroups` beside it compares a
collection with a string and can never be true. So every open stream gets every event today, with
or without the target groups in it.

What a browser sees changes with this: the event written to the server-sent-event stream has no
`targetGroups` property any more. Nothing reads it. The property is in no API description, and
`grep -rn targetGroups ui/ apis/` finds nothing.

The concept `concepts/scoping-the-update-stream.md` keeps it that way on purpose. An event is a
wake-up call there, who it concerns is decided per stream, and the stream asks the same visibility
question the list asks. Building the event without the document is the shape that concept needs, and
the filter itself is separate work which still waits for its review.

#### One place reports a lost change

A listener which throws used to be caught twice, in `UserTaskService.publishUserTaskChange` and in
`ChangeStreamUtils.catchExceptionsListener`, and neither line said which change was lost. The
wrapper in `ChangeStreamUtils` is the only one left, because it is the only one which knows all
three things worth knowing: the collection, the key of the document and the operation. A listener
does not catch any more, and whatever it throws ends up there.

### 32. `uiUriType` is carried and never read, and the address is built in the user interface

Decided by Stephan on 2026-10-03.

`uiUriType` is a string in all four published schemas. The cockpit server stores it, hands it out
and never compares it to anything. Which kind of user interface sits behind a workflow module is an
agreement between that module and the user interface which loads it, and the server is the post
office in between.

Two things follow, and both are the point of the change.

The address is built in the browser. Until now `GuiApiMapper` answered `uiUri` as
`/wm/<workflowModuleId>` plus the reported path, and left the path alone when the type was
`EXTERNAL`. Both mappers now answer the reported path, whatever the type says. The shipped user
interface puts the proxy route in front of it in one place, `addressOfFederatedBundle` in
`ui/bc-ui/src/utils/module-federation.ts`. It does that as an example of the convention and not as a
promise of the server.

The deep link of an `EXTERNAL` module is the same kind of agreement. It was never a property of the
server: the cockpit does not proxy that application and knows nothing about it. The rule that such a
path is already an address lives with the user interface which opens it.

### The field stays required

Making it optional was the obvious next step and it was turned down. A module whose user interface
is federated would leave the key out today, and the entries it reports would carry nothing. The day
a second value exists, and `EXTERNAL` is already that day for somebody else, those entries would
have to mean something, and nobody wrote down what. A required field forces one decision now, which
is cheaper than moving data later.

So the start of an application still refuses a workflow module which reports without
`ui-uri-type`. The check only asks that a value is there. It offers `EXTERNAL` and
`WEBPACK_MF_REACT` as a hint, because those are the two the shipped user interface knows, and it
accepts any other string without a word. The server does not ship the user interface, so it cannot
know which names that one understands. That the application thought about it at all is something it
can ask for.

### What is no longer a type

`io.vanillabp.cockpit.tasklist.model.UiUriType` is gone. It was the stored type of two fields and
the thing the two mappers compared against, and both reasons went away.
`io.vanillabp.cockpit.extension.config.UiUriType` stayed, as two string constants and the list of
names for that one message. It is a list of what is known and not a set of what is allowed.

`workflow-provider-api` called the same thing `UiComponentsType` and showed `WEBPACK_REACT` as its
example, against an enum named `WEBPACK_MF_REACT`. Three words for one thing would have gone into
1.0. All four schemas name it `uiUriType` now, as a string, with the same description.

### Where this decision is referred to

Each of these says it in its own words, so none of them has to cite a number:

- `business-cockpit/src/main/java/io/vanillabp/cockpit/tasklist/api/v1/GuiApiMapper.java`, the
  Javadoc of `toApi`
- `business-cockpit/src/main/java/io/vanillabp/cockpit/workflowlist/api/v1/GuiApiMapper.java`, the
  Javadoc of `toApi`
- `extensions-commons/core/src/main/java/io/vanillabp/cockpit/extension/config/UiUriType.java`
- `extensions-commons/core/src/main/java/io/vanillabp/cockpit/extension/config/BusinessCockpitConfiguration.java`,
  the comment above the `ui-uri-type` check
- `ui/bc-ui/src/utils/module-federation.ts`, `UiUriType` and `addressOfFederatedBundle`
- `UPGRADE.md`, the section about the address
- `skills/business-cockpit-user-interface/references/gui-api.md` and
  `references/module-federation.md`
- the wiki pages `User-task-forms-and-status-sites` and `Customizing-the-user-interface`

### 33. One property turns the whole npm side of the build off, and no registry runs beside it - superseded by decision 62

Decided on 2026-10-03.

A build of this repository which does not touch the user interface runs no npm step at all. The
switch is the property `skip.npm`, and the profile `java-install` is the name you pass on the
command line. The local npm registry stays what it is today: something a frontend developer starts
by hand, following `development/README.md`. It does not become part of building this repository and
it does not move into the dev container.

### What the three modules did before

`business-cockpit`, `container` and `development/simulator` are the modules which own a user
interface. Each of them reached npm in the middle of its own build, so each of them wanted the
registry at `http://localhost:4873/`. Nobody serves that in a dev container, and the error said
`ECONNREFUSED`, or in `development/simulator` only exit code 254 from `npm link`. Both read like a
mistake in the clone rather than a missing service.

The profile already existed and already covered the whole npm side of the three modules. What it
did not have was a reader. The guidance named it for a build of the whole reactor, so a single
module build went without it and walked into the wall. That is why story 1378 could measure eleven
modules of this repository and left these three unmeasured.

### Why one property and not seven

The profile used to set seven properties, and that was the part which could rot. `skip.npm` and
`skip.installnodenpm` are the npm plugin's own properties, so they already reach every execution
which carries no `skip` of its own. The other five exist because five executions do carry one, and
each of them had to be listed in the profile by hand. An execution added later with its own `skip`
would have escaped the switch, and the profile would have looked complete while a dev container
build died in that step.

The five now derive from `skip.npm` in the root POM. The profile sets one value, every skip follows
it, and `-Dskip.npm=true` does the same thing without the profile. Measured per module with
`help:evaluate`, a build which passes no switch sees the same seven values as before, and so does
every one of the five npm profiles.

### Why not run the registry in the container

A registry in the container would be one more service to start and to clean up afterwards, and it
would publish snapshot versions of the user interface packages that nothing reads. The
frontend is checked on its own, beside the Java build, by `bin/frontend-checks.sh`, and that script
is where a frontend developer needs the registry. Building the Java side does not need the
packages, it needs them absent.

### What points at this

| place                                 | what it says |
|---------------------------------------|--------------|
| `pom.xml`, the `skip.npm` properties  | the switch, and that the other skips must keep deriving from it |
| `pom.xml`, the profile `java-install` | what the switch is for and which three modules need it |
| `AGENTS.md`, section `Building`       | pass it for a single module as well, and what the errors look like without it |
| `README.md`, section `Building it`    | the registry belongs to a frontend build, not to building this repository |

### 34. A wait for a report has to name the report

Decided on 2026-10-03.

`CockpitServer.awaitAnyRequest` and `CockpitServer.awaitRequests` refuse a path which carries no
id of a case. A test which waits on `/usertask/created` or `/workflow/created` has to say which
report it waits for, through `awaitRequest` when it reads the report afterwards or through
`awaitRequestOf` when only the arrival matters. The refusal is thrown before the wait starts, so
the test which gets it wrong is the test which fails.

#### What went wrong

Every test of a module reports into one server, and the dispatch of an outbox entry outlives the
test which caused it. A report of an earlier test therefore arrives on a collecting path at any
moment. A wait which takes the next report of its kind is satisfied by it, and the test walks on
although nothing it provoked has happened yet.

Eight places in the Process-Engine-API adapter made that visible, because each of them called
`forgetRequests()` one line below the wait:

```java
aDeliveredUserTask(aggregate, "task-6");
CockpitServer.awaitAnyRequest("/usertask/created");
CockpitServer.forgetRequests();
```

The wait takes the stale report, the forget throws away the report of `task-6`, and the second
wait further down then waits for a report nobody owes it any more. The failure lands in a later
test or in a timeout, never where the mistake is.

The `forgetRequests()` in that pattern is right and stays. It is what tells two reports about the
same case apart: the test waits for the first report, forgets it, provokes the second and waits
again. The wait in front of it was the only broken part.

#### Why the mechanic cannot do it alone

A wait could record where the list of received reports stood and accept only what arrives after
that. It would be wrong. A test provokes its report and waits afterwards, so the report is often
already there when the wait runs, and a wait which refuses what it finds would never return. Only
a mark set before the trigger could have that rule, and setting it would mean splitting every one
of the 26 call sites into three steps.

And it would buy nothing. A late report of the previous test lands on the same path as the report
under test and differs from it in one thing: what it carries. Time does not tell them apart,
content does. A wait which names the content is safe from another case's report whenever that
report arrives, and a mark anchored in time is only safe from the ones which arrive early.

What the mechanic can do is refuse the wait which has no chance of being right, and that is what
it now does. The rule is read off the path: the cockpit is told about something new on a path of
two parts, because it does not know the id yet and the body carries it. Every later report about
that case has the id between the kind and what happened. So a suffix of two parts collects and a
longer one names its case.

#### What it covers

| repository                                   | collecting waits before | after |
|----------------------------------------------|-------------------------|-------|
| `business-cockpit`                           | 0                       | 0 |
| `businesscockpit-process-engine-api-adapter` | 17                      | 0 |
| `businesscockpit-camunda7-adapter`           | 9                       | 0 |
| `businesscockpit-camunda8-adapter`           | 0                       | 0 |

The main repository was already clean. The commit which introduced `awaitRequestOf` moved the
four waits of `BusinessCockpitExtensionTest` onto it at the same time, and the six
`awaitAnyRequest` calls left in `extensions-commons` all carry the id of their case. The Camunda 8
adapter was counted the same way and has nothing on a collecting path.

The two adapters go red the moment this snapshot reaches them, so their branches have to be in
before it is published.

#### References

- `extensions-commons/test-support/.../CockpitServer.java`: the refusal, and the Javadoc of
  `awaitAnyRequest`, `awaitRequest`, `awaitRequestOf`, `awaitRequests` and `forgetRequests`
- `extensions-commons/test-support/.../WaitMarkTest.java`: five tests, among them the wait which
  falls when its report stays away and the wait which is served by a report arriving while it runs
- `refuseRequestsAbout(marker, count)`, which asks for the event by name for the same reason: a
  refusal which took whatever arrived next was spent on a report of a class which had long
  finished
- story `1351`, which introduced `awaitRequest(pathSuffix, bodyPart)`, story `1373`, which moved
  the 30 waits reading a body onto it, and the commit beside them which added `awaitRequestOf`

### 35. The chain of cockpit repositories is checked once a night, from this repository

Decided on 2026-10-03, as the default of stories 1396 and 1400. The question below is open and
belongs to the maintainer.

One script in this repository, `bin/check-the-chain.sh`, looks at the main branches of
`business-cockpit`, `businesscockpit-camunda7-adapter`, `businesscockpit-camunda8-adapter` and
`businesscockpit-process-engine-api-adapter` from the outside. A nightly workflow,
`.github/workflows/chain-check.yaml`, runs it. It asks four questions:

1. Do the four repositories name the same version where they have to?
2. Has a pin stayed behind without anybody saying so? That is a release older than Renovate's
   waiting time, with no open pull request which moves the pin and no rule in `renovate.json`
   which tells Renovate to leave it alone. The same question asks whether Renovate still lists the
   pin on its dependency dashboard, with the version main has.
3. Was the newest run of every workflow on main green?
4. Does the published snapshot belong to the head of main?

The MongoDB changeset library, `Phactum/mongodb-changesets`, gets questions 3 and 4. It belongs to
another account and is released on its own, but this repository builds against its snapshot, so
a broken main or a stale snapshot there reaches us first. Its pins are not compared, because none
of them ends up next to an extension in a workflow module.

#### Why one script and why here

This repository is the root of the chain. Each adapter repository builds against its snapshot,
so a check which starts here reads the chain in the direction it is built. One script with four
questions keeps one list of repositories and one way of reporting. Four copies, one in each
repository, would compare the same pins four times and still leave open which repository is
right.

Nothing is built and no test runs again. Everything comes from the GitHub API, Maven Central and
GitHub Packages. A repeated test run would cost CI time every night and say less: one flaky test
would make the report untrustworthy.

#### Why it is no pull-request check

Every finding comes from something which did not happen. A pin drifts apart because one
repository got its update and the other did not. A main stays red because nobody pushed. A
snapshot stays old because its publish run failed and nobody looked. There is no pull request to
hang any of that on.

#### How a finding is reported

A finding becomes an issue with the label `chain-check`. A finding which is still there the next
night is a comment on that issue, never a second issue. A green night after a red one is a
comment as well, and the issue stays open until somebody closes it, because one green night does
not prove a fix.

Every report starts and ends with the time it was asked, a green one too, and goes into the
summary of its run. A question which could not be answered counts as red. Otherwise a check
which cannot see, for example because a token is missing, looks the same as a check which saw
nothing.

#### Which pins are a promise

A pin is compared when its value has to be the same for a reason. The script carries one sentence
per pin:

| pin                            | why the four have to agree |
|--------------------------------|----------------------------|
| release parent                 | it decides the plugins, the Javadoc gate and how a release is cut, and the four are released together |
| Java                           | the lowest Java an application needs is the highest of the four |
| Spring Boot                    | the extension and its BPMS half run in the same Spring Boot application |
| Quarkus                        | both are Quarkus extensions of the same application, and an extension is built for one Quarkus version |
| JaCoCo                         | the coverage gate is the same number everywhere, and means the same only when the same JaCoCo counts it |
| VanillaBP platform             | the extension and its BPMS half join the same platform in one application |
| `business-cockpit` in adapters | an adapter has to build against the extension this repository builds |

Not compared, and the report says so every night:

- The Camunda 8 client pins of the release lines, the Camunda 8 adapter per line and protobuf. The
  Camunda 8 cockpit adapter takes them from `camunda-community-hub/vanillabp-camunda8-adapter` and
  raises them by hand, and its own nightly matrix holds them against that repository. Comparing
  them here would make the check red every day.
- `testcontainers.version`. It is used in tests only, no jar of one of the four carries it into
  another, and only the Camunda 8 adapter names it.
- `lombok.version`. It is gone once the class file exists, so it never meets another repository.

This repository names its versions `version.<name>`, the adapters `<name>.version`. So Quarkus is
`version.quarkus` here and `quarkus.version` there. The name stays, because every other version
of this repository follows that pattern, and Renovate reads both names. The script knows both,
and a comment at the property in `extensions-commons/pom.xml` says why it differs.

#### Which version is right when they differ

Not decided by the check. The report lists the value of every repository and names the highest.
The default answer is the highest, because catching up costs less than going back. But the report
gives both numbers and does not claim an order.

#### The open question

Should this become one tool with the platform strand? Its main repositories have the same gap,
nothing on a schedule looks at their main branch, and one of its stories asks for a tool which
checks a set of repositories against itself. One tool for nine repositories would be one answer
instead of two. It costs agreement between the two strands, and the platform has more than one
chain. The default is this script, for the four cockpit repositories, until the maintainer decides
otherwise.

### 36. A change of a case the cockpit never saw created creates the case - the start of such a case narrowed by decision 45, and corrected by a late creation in decision 50, and who sees a user task changed by decision 54

Measured on 2026-10-03 for story 1423. Nothing was changed, the entry writes down what the server
already does.

A workflow module does not always report the start of a case. The Process-Engine-API half reports a
case with its first user task, and a case which never had one is reported first by a change. A
cockpit added to a running system hears of older cases the same way. So the server takes a change
of a case it does not hold as the first report of that case.

This holds on every way in. Version 1 and version 1.1 of the REST API, `workflow/{id}/updated`,
both answer `200 OK` and store the case. The Kafka way in does the same. The rule sits in
`WorkflowlistService.reportChangedWorkflow`, so the controllers have no say in it. User tasks follow
the same rule in `UserTaskService.reportChangedUserTask`.

#### What such a case holds

The case is built by `toNewWorkflow` from the change, the mapping a creation uses as well. So it
holds everything the change reports, and nothing else.

- `createdAt` is the timestamp of the change. A change does not say when the case began, and this
  is the closest answer there is.
- `latestEventAt` is the same timestamp, and `reportedAt` is the cockpit's own clock.
- `endedAt` stays empty. A change never ends a case.

#### What comes after it

A creation which arrives later stores nothing. The case has a `createdAt`, so it does not look like
a case known from its end alone, and only such a case waits for its creation. The case keeps the
time of the change as its start, and the data of the change, which is younger than the creation
anyway.

An end which arrives later ends the case as it ends any other.

A second change is laid onto the stored case by `toUpdatedWorkflow`. That mapping has no `IGNORE`
strategy, unlike the mapping of an end in decision 19. A change writes every field it carries, and a
field it leaves empty is written as empty. The fields are `initiator`, `updatedAt`, `updatedBy`,
`source`, `workflowModuleId`, `comment`, `bpmnProcessId`, `bpmnProcessVersion`, `businessId`,
`title`, `uiUriPath`, `uiUriType`, `accessibleToUsers`, `accessibleToGroups`, `details` and
`detailsFulltextSearch`. The cockpit keeps `id`, `version`, `reportedAt`, `latestEventAt`,
`createdAt`, `endedAt`, `targetGroups` and `dangling`. This entry leaves that rule as it is. It
matters here because a case created by a change is from then on only as complete as the latest
change.

#### What it costs

The start of such a case in the list is the time of its first change, not the time the case began.
A creation which was only late, and not missing, cannot correct that. The other way round would be
worse: a creation which never comes would leave the case out of the list for good.

### 37. A report whose path and body name different records is refused

Decided on 2026-10-03 for story 1426. Before, the server took such a report and answered `200 OK`.

A report about a user task or a case the cockpit may already hold carries the id twice. The path
names it, for example `workflow/{workflowId}/updated`, and the body names it again, in `workflowId`
or `userTaskId`. The schema of the body requires that field. The service looks the stored record up
by the id in the path. The mapper which builds a record the cockpit does not hold yet takes the id
from the body. Decision 36 lets a change create a case, so the two ids met in one call: a change
whose body named another case was stored under the id of the body, and the next change with the
same path did not find it.

So both ids have to be the same. If they differ, the server answers `400 Bad Request`, stores
nothing, and logs a warning which names both ids. This holds for version 1 and version 1.1 of the
REST API, and for every report whose path carries an id: `updated`, `completed` and `cancelled`, of
user tasks and of cases. A body which leaves the id out differs from the path as well, so it is
refused too. The schema asks for the id anyway.

The Kafka way in is not touched. A Kafka record has no path, and its body is the only place the id
is named.

#### Who sends such a report

Nobody we know of. The Version 2 extension, `RestTransport` in `extensions-commons/core`, takes the
id in the path and the id in the body from the same field of the same event. The Version 1
integration, at the `0.4.0` tag, did the same in `UserTaskRestPublishing` and
`WorkflowRestPublishing`. The three BPMS halves do not call the REST API themselves, they hand
their events to `extensions-commons`.

#### What it costs

A client which sent different ids got `200 OK` before and gets `400 Bad Request` now. Such a
client did not get what it asked for before either, because its report was stored under the id it
did not use for the next report.

### 38. A report the cockpit could not store is answered with 503 - the Kafka way in changed by decision 40, a save which fails every time changed by decision 41

Decided on 2026-10-03 for story 1433. Before, the server answered such a report with `400 Bad
Request`.

The services which store a report answer with `true` or `false`. They answer `false` in one case
only: the save into MongoDB threw. That happens when MongoDB cannot be reached for a moment, or
when another report about the same record was stored at the same time. A report older than what is
stored is no such case. The service keeps what it has, logs that the report changed nothing, and
answers `true`. So does an end of a record which has ended already, and a creation of a record the
cockpit holds.

The REST API of the BPMS side turned `false` into `400 Bad Request`. The sender, `RestTransport`
in `extensions-commons/core`, gives up a report answered with 400, because decision 11 says that
such a report would be refused again. So a report was lost for good while MongoDB was gone for a
moment, and only a person could take it out of the outbox and send it again.

Now the server answers `503 Service Unavailable` where the save failed. Decision 11 makes the
sender repeat a 503. The answer carries no `Retry-After`, because the server does not know when
MongoDB is back. Without the header the outbox store waits as long as its own backoff says. The
report still counts against `vanillabp.outbox.block-after-attempts`, so a MongoDB which stays away
for good ends in a blocked entry and not in a report repeated forever.

The other answers stay as they were:

- `200 OK` for a report which was stored, and for a report older than what is stored
- `400 Bad Request` for a report whose path and body name different records (decision 37). That
  check runs before anything is stored, so it answers 400 even while MongoDB is gone.

This holds for version 1 and version 1.1 of the REST API, for every report about a user task or a
case.

#### Why 503 and not 500

Both make the sender repeat the report. A 500 says that something went wrong which nobody
expected, and it is the answer the server gives to a defect. A 503 says that the server cannot take
the report right now, which is what happened. It is also the status decision 11 names for "not
now", so it keeps meaning that if the sender ever treats a 500 differently.

#### What is not changed

The Kafka way in. Its listeners do not read the answer of the service. A save which failed is
logged, and the record counts as consumed. A read from MongoDB which throws goes to the error
handler of Spring Kafka. Both are defects of their own, and this decision leaves them as they are.

A read which throws before the save is not changed either. It reaches the catch-all of the
exception handler and is answered with 500, which the sender repeats as well.

#### What it costs

A client which read 400 as "not stored" reads 503 now. A client which gave up on 400 repeats now,
which is the point of the change.

### 39. A request which breaks the rules of its schema is answered with 400 and the field

Decided on 2026-10-03 for story 1432. Before, the server answered such a request with `500
Internal Server Error`, and the body held the whole message of the exception.

The REST API of the BPMS side validates every report against its schema. A report which leaves out
a required field, like `timestamp` or `workflowId`, makes Spring throw a
`MethodArgumentNotValidException`. `RestfulExceptionHandler` in `commons` had no handler for it, so
its catch-all answered 500. The sender, `RestTransport` in `extensions-commons/core`, repeats a 500
(decision 11). So it repeated a report which could never go through, until the outbox blocked the
entry after its last attempt, and the entry did not say that repeating was pointless.

Now the handler answers `400 Bad Request` with a body of one line in plain text, for example:

> The request is not valid: 'bpmnProcessId' is missing, 'title' is missing.

The line names every field which breaks a rule, sorted by name. A missing field is "is missing",
any other rule is named after its constraint, like "breaks the rule @Size". The validator's own
message is left out, because it is written in the language of the server's locale. The value the
request sent is never repeated, because it can be large or personal. The log of the server gets the
same line as a warning which starts with `Returning HTTP 400 Bad Request`, without a stack trace.

The same answer is given to a `HandlerMethodValidationException`. Spring throws it when a
parameter of a controller method carries a constraint itself, like `@Size` on a path variable. No
API of the cockpit has such a parameter today. The handler is there so that one added later does
not end in the catch-all.

This holds for version 1 and version 1.1 of the REST API, for every report.

#### Why plain text and not Problem Details

Spring can answer with Problem Details (RFC 9457). Nothing in this repository uses them, and
every other handler of `RestfulExceptionHandler` answers with plain text. The sender shows the body
of a refusal in its log, and one line of text is what reads well there.

#### What is left out

A `ConstraintViolationException` keeps the answer of the catch-all. In the cockpit it can only come
from validation inside the server, like a service checking its own arguments. That is a defect of
the server and not of the request, so a 500 is the right answer for it.

#### The graphical user interface

The handler is global, so the API of the graphical user interface reaches it too. That API is
generated with `useBeanValidation` switched off, and none of its controllers validates a parameter.
So neither of the two exceptions can come from it, and its answers stay as they were. The same holds
for the simulator under `development/simulator`, which uses the handler with the classes of
`official-gui-api-server`. None of those classes carries a validation annotation.

#### What it costs

A client which read 500 as "invalid" reads 400 now, with a body which names the field instead of
the message of an exception.

### 40. A report from Kafka the cockpit could not store comes again until it is stored - a save which fails every time and what an attempt logs changed by decision 41, and the start of a case created by a change narrowed by decision 50

Decided on 2026-10-03 for story 1435. Before, such a report was lost.

Related entries: 11 (what a sender repeats), 36 (a change of a case the cockpit never saw creates
the case), 38 (a report the cockpit could not store is answered with 503). This entry does for the Kafka
way in what 38 does for REST. The part of 38 headed "What is not changed" describes the Kafka way in
as it was before this entry. Whoever moves this entry into the log decides whether that paragraph
gets a note which points here.

The Kafka listeners of the cockpit did not read the answer of the service. A save which failed was
logged, the listener returned, and the offset of the record was committed. A read from MongoDB which
threw went to the error handler Spring Kafka uses when nobody configures one. That handler tries a
record ten times in a row, without a pause, and then passes over it. Either way, a MongoDB which was
gone for a few seconds cost a report for good.

Now a listener throws where the service answers that it could not store the report. The three
listeners run in a listener container factory of their own, `businessCockpitKafkaListenerContainerFactory`.
Its error handler, `RepeatUntilStored`, hands the same record to the listener again, after a pause
which starts at one second, doubles each time and stops growing at one minute. There is no last
attempt. The factory is built with the Kafka settings of Spring Boot, so `spring.kafka.*` applies to
it as before. Other listeners of an application derived from the cockpit keep the factory and the
error handler of Spring Boot.

#### Why no dead-letter topic

A workflow module sends every report about one task or one case with its id as the key. So all of
them sit on one partition, in the order they were sent. A report parked on a topic of its own lets
the reports behind it overtake it. Decision 36 shows what that costs: a change which overtakes the
creation creates the case, and the creation which comes later stores nothing. The case then starts
at the time of the change. Repeating the report in place keeps the order.

The price is that one partition stops while MongoDB is gone. Nothing behind the record could be
stored anyway, because all of it goes to the same MongoDB.

#### What is repeated and what is passed over

Only a failure of storing is repeated: the answer of the service that the save failed, and an
exception of Spring's data access or of the MongoDB driver anywhere in the causes. Everything else
is about the record itself. That is bytes which are not a protobuf message, an event type this
cockpit does not know, or a field the mapping cannot take. Such a record fails the same way each
time, and repeating it would stop the partition for good. So it is passed over at once, and the log
gets an error which starts with `Passing over a Kafka record the cockpit cannot read` and names the
topic, the partition, the offset and the key, so that somebody can find it on the topic.

#### Why a report repeated does not count twice

A report which was not stored left nothing behind. When it comes again it is weighed against what is
stored, like any report (decision 18 and decision 26): a creation of a record the cockpit holds
stores nothing, a change older than what is stored is dropped, and an end of a record which has
ended changes nothing. A record which was stored and still comes again, because the commit of its
offset was lost, is weighed the same way.

#### What it costs

The listener and the error handler log an error each time a report fails, so a MongoDB which is gone
for an hour writes about sixty errors per listener. A record whose save fails for a reason which
never goes away, like a document larger than MongoDB takes, stops its partition until somebody acts.
The REST way in answers the same case with 503 and its sender repeats it as well, until its outbox
blocks the entry.

Nothing is configurable. The pauses are short enough for a MongoDB which restarts, and long enough
not to flood the log. The longest pause stays well below the five minutes Kafka allows between two
polls by default, because the container waits in the thread which polls.

### 41. A report MongoDB refuses every time is given up, and every other failed save comes again - a key with a dot narrowed by decision 44

Decided on 2026-10-04 for story 1437. Before, every failed save was repeated: over REST with 503,
over Kafka without end.

Related entries: 11 (what a sender repeats), 38 (a report the cockpit could not store is answered
with 503), 39 (a request which breaks the rules of its schema is answered with 400), 40 (a report
from Kafka the cockpit could not store comes again until it is stored). This entry narrows 38 and
40. Both stay true for a save which fails for now, and neither holds any longer for a save which
fails every time. Whoever moves this entry into the log adds a note to the headings of 38 and 40,
in the form the log uses already, like "- a save which fails every time changed by decision <n>".
The part of 40 headed "What it costs" says that the listener and the error handler log an error
each time. That was not true when it was written, see "One error for each attempt" below. It
gets a note which points here as well.

The services which store a report caught every exception of the save and answered `false`. So a
document larger than MongoDB takes was repeated like a MongoDB which was gone for a moment. On
Kafka it stopped its partition until somebody acted. Over REST the sender repeated it until its
outbox blocked the entry.

Now the services answer with an `OutcomeOfStoring`. It says whether the cockpit is up to date about
the record, and if not, whether the failure goes away or comes back every time.

#### Which failures come back every time

Three, and each of them depends on nothing but the document:

- `BsonMaximumSizeExceededException`. The driver throws it before it sends anything, because the
  document is larger than the 16 MB MongoDB takes. Spring does not translate it.
- A write which breaks the validation rules of the collection. MongoDB answers it with the error
  code 121, the driver throws a `MongoWriteException` with that code, and Spring wraps it into a
  `DataIntegrityViolationException`. The cockpit sets no such rules. An operator can add them.
- A `MappingException` of Spring Data. It is thrown while Spring Data turns the record into a
  document, before anything is sent. The case known is a key of the business data with a dot in it,
  like `order.id`. The cockpit configures no replacement for the dot, so Spring Data refuses the
  key.

The first two are what was decided as "the size and errors of the schema while writing". The third
is the schema error which comes before MongoDB sees the document. A test against a real MongoDB,
`WhatMongoDbRefusesEveryTimeTest`, shows which exception arrives at the service in each case.

Everything else counts as a failure for now: a MongoDB which cannot be reached, a report about the
same record which was stored at the same time, a duplicate key, a timeout. Repeating a report which
could have gone through costs a few attempts. Giving it up loses it.

#### What REST answers

`422 Unprocessable Content` with one line of plain text, for example:

> The user task 'task-1' cannot be stored: it is larger than the 16 MB MongoDB takes for one document.

The sender, `RestTransport` in `extensions-commons/core`, gives up a 422 as it gives up any status
from 400 to 499 apart from 408 and 429 (decision 11). So 400 would end the entry as well. 422 was
chosen because a 400 of this API says that the report breaks a rule of the API and has to be
changed. Decision 39 gives such a 400 a body which names the field, and the sender's log points at
the fields, the path and the version of the API. A report MongoDB cannot store breaks no rule of the
API. The cockpit understood it and still cannot keep it, which is what 422 means. The body is one
line of text like the body of decision 39, so the sender's log shows it.

The body names the record and the reason. It never repeats a value of the report. For a key with a
dot it repeats the key, because the sender needs it to find what to change.

A save which fails for now is still answered with `503 Service Unavailable` (decision 38).

#### What Kafka does

The listener throws a `ReportCannotBeStoredException`. The error handler `RepeatUntilStored` never
repeats it and passes the record over at once, like a record it cannot read (decision 40). The log
gets one error which starts with `Passing over a Kafka record the cockpit cannot store` and names
the topic, the partition, the offset and the key, with the reason and the stack trace of the save.

The exception carries the failure of the save as its cause, and that cause is a
`DataAccessException`, which the error handler repeats. The handler stops at the first class it
knows when it looks through the causes. So `ReportCannotBeStoredException` is named as one it never
repeats, and it is found before its cause.

A save which fails for now still comes again until it is stored (decision 40).

#### One error for each attempt

Each attempt which failed is logged once, as an error, by the way the report came in. Only that way
knows what happens to the report next.

- Over REST, `AnswerToAReport` logs `Returning HTTP 503 Service Unavailable` or `Returning HTTP 422
  Unprocessable Content`, with the reason and the stack trace.
- Over Kafka, a retry listener of `RepeatUntilStored` logs `Handing a Kafka record over again,
  attempt <n> failed`, with the record, the attempt and the stack trace. It does so for every
  failure it repeats, a failed save and a failed read alike.

The services log nothing any more where a save fails. Before, `UserTaskService` and
`WorkflowlistService` logged `Could not save ...` as an error. Over REST that line moved into the
answer. Over Kafka the line of the retry listener has everything it had, and the record and the
attempt besides.

Measured with Spring Kafka 4.1.1 before the change, one attempt which failed logged:

| Way in and failure           | Errors before | Errors now |
|------------------------------|---------------|------------|
| REST, save fails for now     | 1             | 1 |
| Kafka, save fails for now    | 1             | 1 |
| Kafka, read fails            | 0             | 1 |
| REST, save fails every time  | 1             | 1 |
| Kafka, save fails every time | 1             | 1 |

So the listener container never logged an error of its own. It logs `Record in retry and not yet
recovered` at INFO, without the cause. A read which failed was not logged above INFO at all, and
nobody could see why a partition waited.

#### What it costs

A sender which repeated a report MongoDB refuses every time gives it up now, and the report is
lost. It was not stored before either. Now the log of the cockpit and the log of the sender say
which record it was and why.

A defect of the cockpit which throws a `MappingException` for every record would pass every record
over, instead of holding them up until a fixed release is deployed. No such defect is known. A
mapping the cockpit cannot write at all would fail its own tests.

### 42. A report answered with 501 is given up, and suspended and activated say that they answer it

Decided on 2026-10-04 for story 1438. Before, the sender repeated a report answered with 501, and
the BPMS API did not say that two of its operations answer it.

Related entries: 11 (a failed report says whether repeating it can help), 38 (a report the cockpit
could not store is answered with 503), 41 (a report MongoDB refuses every time is given up). This
entry adds one status to what 11 calls "not like this". Entry 11 stays as it is. Whoever moves
this entry into the log decides whether 11 gets a note in its heading, in the form the log uses
already, like "- 501 changed by decision <n>".

The REST API of the BPMS side has two operations which the server does not implement:
`usertask/{userTaskId}/suspended` and `usertask/{userTaskId}/activated`, in version 1 and version
1.1. The controllers leave them to the generated interface, and that answers `501 Not Implemented`
without looking at the report. The sender, `RestTransport` in `extensions-commons/core`, gave up only
a status from 400 to 499, so it repeated a 501 until its outbox blocked the entry. Repeating it
never helps, because the server answers the same report the same way every time.

Now `RestTransport` gives up a report answered with 501, like a status from 400 to 499 apart from
408 and 429. Its message names two causes, one after the other. The workflow module and the cockpit
server speak different versions of the BPMS API. Or something other than the cockpit server
answered, for example a proxy which does not pass the request on, and the message names the key of
the base URL to check.

Both specifications say for the two operations that they are not implemented and answer 501, and
they describe the 501 as a shared response, `NotImplemented`. That changes nothing but the
Javadoc of the generated client and the server, and the `@Operation` and `@ApiResponse`
annotations of the generated server.

#### Who sends the two reports

Nobody. `RestTransport` and `KafkaTransport` send a user task only as created, updated, completed or
cancelled. The three BPMS halves hand their events to `extensions-commons` and call neither API
themselves. The simulator under `development/simulator` has code which sends both reports, but it
sits behind `doUpdate = true` and never runs.

The Kafka way in carries the same two events in `v1.proto`. `KafkaUserTaskController` does not
handle them and throws, so such a record is passed over at once like any record the cockpit cannot
read (decision 40). This entry leaves that as it is.

#### Why not implement them

Nothing sends them, so nothing would show whether an implementation does what a sender expects.
They are implemented when a sender needs them.

#### What it costs

A sender which repeated a report answered with 501 gives it up now. It was never stored before
either. A server which answers 501 for a moment, and would take the report later, loses it. No
server of the cockpit does that.

### 43. A change the BPMS half cannot name right away is built when its entry is dispatched - the election moved to the dispatch by decision 47

This entry narrows decision 26 for one way: `BusinessCockpitService.aggregateChanged(aggregate)`.
The headline of decision 26 says so.

#### What was wrong

`aggregateChanged` runs in the application's transaction, and the report was built there. On
Camunda 8 the BPMS half can only name the workflows of a case out of the cluster's searchable
storage, unless VanillaBP wrote down key and version at the start. That storage is written by an
exporter which runs behind the engine. So a change reported while the exporter was behind was
dropped with a warning, and nothing reported it later. The last change of a case, and every change
while the exporter stood still, never reached the cockpit. A storage which was down failed the
application's transaction as well, so a report rolled back the application's own work.

#### What is decided

A BPMS half says itself whether it can name the workflows of an aggregate in the application's
transaction, through `BusinessCockpitBpmsBridge#workflowsOfAggregateRightAway`. The default answers
what `workflowsOfAggregate` answers, so a half which does not override it works as before. Where the
half answers, the report is built right away, and decision 26 holds unchanged.

Where the half answers nothing, the entry is written without a report. It carries what is known
without the BPMS: aggregate, module, process, adapter, event id, the time of the change, and the
workflow and its version where VanillaBP wrote them down. When the entry is dispatched, the half is
asked `workflowsOfAggregate` and `prefilledWorkflowDetails`, and the report is built then. The
application's details provider runs in a transaction of the dispatch on this way. It reads the
aggregate as it was committed, which is what decision 26 wanted to get away from, and the price is
paid only where the report cannot be built at the event.

The entry is one of the same operation, `PUBLISH_WORKFLOW_EVENT`, marked by the argument
`resolvedWhenDispatched`. A new operation would get a key of its own, so a waiting entry of one kind
and a younger one of the other would both go out. With the same operation the youngest still takes
the place of the waiting one, which is the brake against a flood that decision 26 set up. An entry
which does not know its workflow yet is keyed by its aggregate instead of the workflow. Every entry
which names its workflow keeps the key decision 4 gave it.

An empty answer at the dispatch means "not written yet". The entry is given back to the outbox with
`PhaseTwoRetryLater`, and a `PhaseTwoRetryLater` the half throws there is passed on as well. The
first distance is two seconds. From then on it is a tenth of the time the change has waited, so it
grows by a tenth per attempt after twenty seconds. So an exporter which stands still for minutes
does not get its BPMS asked every two seconds for every waiting change: ten minutes take about 46
attempts instead of 300. The maintainer chose to keep the distance growing. Any other exception goes
to the outbox, which repeats the entry with its own backoff.

The extension ends the window itself, after ten minutes, which is how long an exporter may stand
still without a report getting lost. The outbox does not count a `PhaseTwoRetryLater` as an attempt.
It blocks such an entry only after `vanillabp.outbox.wait-for-visibility-at-most`, which is hours by
default, so the ten minutes always end first.

A change which has no workflow after ten minutes is not dropped. The dispatch throws
`PhaseTwoPermanentFailure`, so the outbox blocks the entry right away. That is the same state an entry
gets after `block-after-attempts`. The extension logs at ERROR what it waited for and what to check:
whether the aggregate has a workflow in that BPMS, and whether the BPMS writes what its engine does.
Ten minutes without the workflow mean that something is wrong, and a dropped change could not be
reported again. A blocked entry stays in the outbox where it can be seen. The outbox does not dispatch it again by itself. Decided on 2026-10-05.

Since 2026-10-07 the ERROR line also says how to get the entry out again. It names the place
where the entry keeps its reason: the column `LAST_FAILURE`, on MongoDB the field `lastFailure`
(decision 118 of the platform). It ends with the address of the platform's wiki page
[Blocked outbox entries](https://github.com/vanillabp/adapter-platform-integration/wiki/Blocked-outbox-entries),
which shows per store how to find a blocked entry and open it again or delete it. The address
is taken from `PhaseTwoOutboxProperties.BLOCKED_ENTRIES_GUIDE`, the constant the platform's own
ERROR lines use (decision 115 of the platform), so both lines always name the same page. The
outbox writes its own ERROR about the entry next to ours, with the row and the stack trace. Both
lines stay. A command or a user interface for the repair is still missing. Decided on 2026-10-07.

The adapter of a change comes from VanillaBP's note of the start, or is the one adapter the
application configured. Only an application with several adapters and no note still asks the
election in the application's transaction, and the election may ask a BPMS there.

#### What it costs

A report built at the dispatch carries the timestamp of the change and the state of the dispatch.
Where an event with a later timestamp reached the cockpit in between, the cockpit keeps the later
one (decision 18), which is right. Otherwise the report shows details a little newer than its
timestamp.

The registration of a workflow module is no longer the only entry without a report which is built
at its dispatch. An entry which lost its report to a write which never committed is still sent
with its identifiers alone, and the argument tells the two apart.

A node of an older version, while the deploy rolls, does not know the argument. It sends such an
entry with its identifiers alone. Where the entry names its workflow, the cockpit gets an update
without details and keeps what it showed. Where it does not, the REST transport sends a path
without a workflow, the server refuses it with a client error, and the entry is blocked. The Kafka
transport fails before it sends, and the entry comes back, to a newer node in the end. Both were read
from the code and not tried. Before this entry such a change was not reported at all.

### 44. A key with a dot in the business data is stored once a replacement is configured

Decided on 2026-10-04 for story 1442. Before, a report whose business data had a key with a dot in
it was always given up.

Related entries: 41 (a report MongoDB refuses every time is given up). This entry narrows 41. Its
list of failures which come back every time names a key with a dot, and says that the cockpit
configures no replacement for the dot. That stays true by default, and it stops being true where the
new property is set. Whoever moves this entry into the log adds a note to the heading of 41, in the
form the log uses already, like "- a key with a dot changed by decision <n>".

A dot in a key is not forbidden. The business data, `details`, is a map the workflow module fills,
and a key like `order.id` is a normal thing to put there. MongoDB reads a dot in a path as a step
into a nested document, so Spring Data refuses such a key unless it is told what to write instead.

Now the cockpit offers that as a property, `business-cockpit.mongodb.map-key-dot-replacement`. It is
unset by default, so nothing changes for an installation which does not set it. Where it is set,
`MongoDbConfiguration` hands it to the converter of the cockpit's `MongoTemplate`, and Spring Data
writes the replacement instead of each dot. It turns the replacement back into a dot when it reads
the record. Nested maps are handled the same way.

#### What it costs

Two things, and every text which offers the property names both:

- Search and sorting see what is stored. A column or a filter has to name the key in its stored
  form, like `details.order~id` for the replacement `~`. The path with the dot finds nothing.
- The way back is not exact. A key which holds the replacement already comes back with a dot in its
  place.

The full-text search is not affected, because the workflow module fills `detailsFulltextSearch`
itself. The changesets do not read `details`.

#### Where the property is named

A report with such a key is still given up where the property is not set, with `422` over REST and
passed over on Kafka, as 41 says. The reason in the body and in the error of the log now names the
key, the property, an example value and both costs. It stays one line, so the sender's log shows it.

The description of `details` in both specifications of the BPMS API says the same, and so does
`apis/bpms-api/README.md`. That changes nothing in the generated code but its Javadoc and its
`@Schema` annotations.

#### What a value has to be

A value which cannot work stops the start, because somebody set it to have such reports stored, and
a cockpit which ignored it would give them up without a word at the start. `StartupConfigurationCheck`
checks it before the first bean is built, and a failure analyzer shows the reason, the property and
a value to copy. These values are refused:

- an empty value, or one of nothing but spaces. Every dot would be dropped or become a space.
- a value with a dot. The stored key would have a dot again.
- a value with a `$`. MongoDB reads a field name which starts with `$` as an operator.
- a value with the character NUL, which MongoDB does not take in a field name.

#### Why not keep the dot

Spring Data can also write the key as it is (`preserveMapKeys`), and MongoDB takes that since
version 5. But every path in the cockpit is a chain of dots: the search, the word suggestions, the
sorting with its index, the user interface, and the columns a workflow module declares. None of them
would reach such a key. Only `$getField` in an expression does, and the cockpit builds that nowhere.
`AKeyWithADotInTheDetailsTest` measures both settings.

### 45. An end which arrives alone gives the record a start, and says which start where it can - a late creation of any record changed by decision 50, and who sees a user task changed by decision 54

Decided on 2026-10-04 for story 1429. Before, a user task or a case which the cockpit knew from its
end alone had no `createdAt`, and the user interface got a record without the start its schema
requires.

Related entries: 18 (the timestamp of the event decides which report the cockpit stores), 36 (a
change of a case the cockpit never saw created creates the case). This entry narrows 36 and changes
one paragraph of 18. 36 says that only a case without `createdAt` waits for its creation, and 18
says that a record held from an end alone is recognizable by its empty `createdAt`. Neither is true
any longer: such a record has a start now, and a property of its own says that it waits. Whoever
moves this entry into the log adds a note to the headings of 18 and 36, in the form the log uses
already, like "- a record known from its end alone changed by decision <n>".

The schema of the user interface, `apis/official-gui-api/openapi/v1.yaml`, requires `createdAt` on
`UserTask` and on `Workflow`. Measured before the change against the running application: a
completion which reached the cockpit alone left a task and a case whose answer of the GUI API had
no `createdAt` at all, because the server leaves empty fields out. The generated client turns that
into an invalid date. The schema stays as it is, and the server fills the field instead.

#### The end may say when the record began

The four ends of the BPMS API, `UserTaskCompletedEvent`, `UserTaskCancelledEvent`,
`WorkflowCompletedEvent` and `WorkflowCancelledEvent`, have an optional `createdAt` now, in
version 1 and version 1.1. The Kafka messages have `created_at`: the four messages of version 1,
and the two created-or-updated messages version 1.1 sends ends with, where it is read only from an
end. The field is additive. A sender which does not fill it is not affected, and the generated
client and server only gain a property.

Where an end creates the record (decision 18), the server sets `createdAt` from that field. Where the
field is empty, it takes the timestamp of the end. The end is the earliest moment the cockpit knows
of, and a record without a start would break the user interface. The last paragraph of decision 18
said that guessing would make the same case look different depending on which report came first.
That is still the cost, and it is now limited to an end which does not say when the record began.

#### What still waits for its creation

The empty `createdAt` used to be the mark of a record which waits for its creation. That mark is
`knownFromItsEndAlone` now, a property of `UserTask` and `Workflow` which only an end that creates the
record sets. The creation still fills in what the end could not report (decision 18), and its own
start replaces the one of the end. Where the end reported the start, the two are the same, so a
creation which arrives later changes nothing about the start. A change which creates a record
(decision 36) does not set the property, so a creation after it still stores nothing.

A changeset gives the records stored the old way their end as their start and sets the property, so
they still take their creation. Where the end is missing too, it takes the latest event the cockpit
knows of, and then the moment the cockpit stored the record.

#### Who fills the field

`extensions-commons` takes the start from the answer of the BPMS half: `UserTaskDetailsPrefill` and
`WorkflowDetailsPrefill` have an optional `createdAt`, and only the report of an end sends it. Both
keep their constructors without the field, so a half which does not fill it needs no change. A half
fills it where the event it reports carries the start, and leaves it empty where it could only ask
the engine, because a read at that moment is what decision 26 ruled out.

#### What it costs

A record known from its end alone, whose end does not say when it began, shows its end as its start
until the creation arrives. If the creation never arrives, that stays so. Before, such a record
broke the user interface.

### 46. A changed user task the BPMS half cannot report right away is built when its entry is dispatched - the election moved to the dispatch by decision 47

Decided on 2026-10-05, while story 1443 was built.

This entry does for `BusinessCockpitService.aggregateChanged(aggregate, userTaskIds)` what decision
43 does for `aggregateChanged(aggregate)`. It narrows decision 26 for this second way too. Whoever
moves this entry into the log adds it to the headline of decision 26, in the form the log uses
already, like "and by decision 46, which does the same for a changed user task".

#### What was wrong

`aggregateChanged(aggregate, userTaskIds)` runs in the application's transaction, and the report
of each task was built there. On Camunda 8 the BPMS half can only name the tasks of a case, and
read their assignee, candidates and dates, out of the cluster's searchable storage. An exporter
writes that storage behind the engine. So a change of a task the storage did not hold yet was
dropped with a warning. Every task created while the exporter stood still got no change report at
all. A storage which was down failed the application's transaction.

#### What is decided

A BPMS half says itself whether it can build the report of a changed user task in the
application's transaction, through `BusinessCockpitBpmsBridge#reportsAChangedUserTaskRightAway`.
The default answers `true`, so a half which does not override it works as before. A half which
answers `false` is asked nothing in the application's transaction.

The extension then takes the tasks from what VanillaBP wrote down when it delivered them
(`WorkflowElection#openUserTasksOf`). No BPMS is asked for that. Each task gets an entry without a
report. The entry carries what the delivery log knows: the task, the workflow of its case, the
BPMN process the task belongs to, the task definition, the BPMN element and the version.

- Where the application named tasks, only those tasks get an entry. A named task the log does not
  know gets an entry with its id alone.
- A task of a called process names the case above it as its workflow, which the log takes from
  the note of the start. Without that note the case is not known, and the task gets an entry with
  its id alone as well. A task of the aggregate's own BPMN process runs in the case itself, so it
  needs no note.
- Where the application named no task and the log knows of no open one, one entry without a task
  is written. That covers tasks delivered before VanillaBP wrote them down, and a log which cannot
  be read.

When the entry is dispatched, the half is asked `prefilledUserTaskDetails`, and the application's
details provider runs in a transaction of the dispatch. An entry with the id alone asks
`userTaskOfAggregate` first, and an entry without a task asks `userTasksOfAggregate` for every
open task. An empty answer about a task means "not written yet". The entry goes back to the outbox
with the window, the growing distance and the block of decision 43: ten minutes, then
`PhaseTwoPermanentFailure` and a line at ERROR which names the task and what to check. Like the line of decision 43, it names the field with the reason and links to the platform's page on blocked outbox entries. An empty
answer of the search for every open task is taken as it is, because an aggregate without an open
task is nothing unusual. Nothing is sent then.

The entry is one of the same operation, `PUBLISH_USER_TASK_EVENT`, with the argument
`resolvedWhenDispatched`, for the reason decision 43 gives. An entry which names its task keeps
the key of decision 4, so a younger report of the same task takes its place. An entry without a
task is keyed by its aggregate.

The half of the tasks is found the way decision 43 finds the half of the workflows, with one more
source before the election: the adapter which delivered the open tasks.

#### What it costs

A report built at the dispatch carries the timestamp of the change and the state of the dispatch,
as in decision 43. A task completed between the change and the dispatch is read with its end, and
the report goes out as an update with the older timestamp. The cockpit keeps the end, because an
older report changes nothing and nothing undoes an end (decision 18).

A task which has no entry in the delivery log, and is not named, is not reported where the log
knows other open tasks of the aggregate. Only an empty log makes the dispatch search for every
open task.

A node of an older version, while the deploy rolls, does not know the argument. It sends such an
entry with its identifiers alone. Where the entry names its task, the cockpit gets an update
without details and keeps what it showed. Where it names no task, the server refuses the report
and the entry is blocked, as decision 43 describes for workflows. This was read from the code and
not tried.

### 47. A report whose adapter nothing names is elected when its entry is dispatched

Decided on 2026-10-06, while story 1447 was built.

This entry changes the last paragraph of decision 43 and the last paragraph of "What is decided"
in decision 46. Both said that an application with several adapters and no note of the start still
asks the election in the application's transaction. Whoever moves this entry into the log adds to
the headlines of 43 and 46, in the form the log uses already, "- the election moved to the dispatch
by decision NN".

#### What was wrong

`BusinessCockpitService.aggregateChanged` runs in the application's transaction. It finds the
adapter of the aggregate from what VanillaBP wrote down: the note of the start, the one adapter of
an application with only one, the adapter which delivered the open tasks. Where none of these
names it, it asked `WorkflowElection#adapterIdOfWorkflow`. Without the note, the election asks the
BPMS, and on a BPMS which answers from a storage behind its engine it waits for up to ten seconds.
That wait held the application's transaction and its database connection.

It happens only to an application with several adapters, which means during a migration: in the
moment after a first delivery which crashed, on Camunda 8 after a start by a message until the
worker reports the id, and while an exporter stands still. No answer was wrong, but a report made
somebody else's transaction wait.

#### What is decided

A report never waits in the application's transaction. A read may wait, so
`BusinessCockpitService.getUserTask` still asks the election where nothing names the adapter.

Where nothing VanillaBP wrote down names the adapter, both report ways write the entry of decision
43 or 46 without an adapter. The workflow entry carries no workflow and no version then, because
the note which could name them belongs to no known adapter. A user-task entry names the task the
application named, or no task where it named none. No BPMS half is asked in the application's
transaction.

The dispatch asks the election first, outside the application's transaction, where the election may
ask a BPMS and wait for it. With the adapter it names, the dispatch goes on as decisions 43 and 46
say. That holds for a half which builds its reports right away as well, like Camunda 7: on this way
its details provider runs in a transaction of the dispatch.

Where the election names no adapter, it throws. On a BPMS with a storage behind its engine that is
what a workflow started a moment ago looks like, so the entry gets the window of decision 43: it is
given back to the outbox with the growing distance, and after ten minutes it is blocked with a line
at ERROR which says what the election answered.

The platform carries an entry without an adapter as it is. `PhaseTwoCall#adapterId` may be
`null`, the outbox stores write `null`, and the dispatch of an extension's own operation hands the
call to the extension and elects nothing.

#### What it costs

An entry without an adapter has "null" in its key where the adapter stands. It never takes the
place of an entry which names its adapter, and the other way round. Two reports may then reach the
cockpit for one change, and the timestamp keeps the older one back (decision 18).

On this way the details provider of a Camunda 7 or Process-Engine-API application runs in the
dispatch, as it does on Camunda 8 under decisions 43 and 46. It reads the aggregate as it was
committed. This is paid only where nothing names the adapter.

A node of an older version, while the deploy rolls, finds no half for the adapter "null". The
dispatch fails, and the outbox repeats it with its own backoff until a newer node takes it or the
entry is blocked. This was read from the code and not tried.

### 48. An update stream asks the visibility of the lists, and remembers what its browser shows

Decided on 2026-10-06, while story 1251 was built.

An event in the update stream is a wake-up call. It names the kind of entity, its id and what
happened to it. It does not say who it concerns. Each open stream decides that for itself, and it
decides with the visibility the lists use: `UserTaskVisibility` and `WorkflowVisibility`. There is
no second rule beside them. `GuiEvent.targetGroups` and `GuiEvent.matchesTargetGroups` are gone.

There is one stream per browser tab. Each subscription gets its own `SseEmitter`.

#### Who gets a change

A stream gets a change in two cases:

- one of its views lets the change through. The views are the widest view of its person
  (`everythingTheUserMayWorkOn`, `workflowsAddressedTo`) and every view a list of that person
  answered with. The second part matters for a list which shows more than the widest view, a
  support team's list of every workflow for example.
- its browser shows the entity. This is how a browser learns that it has to drop something its
  person may no longer see. The id is already in the browser, so the wake-up call tells nothing
  new.

The decision is made once per filtering tick, for all changes collected since the last tick.
Without a change, nothing is asked. With changes, the database is asked once per kind of entity,
however many streams are open. That one query reads the changed entities with only the fields the
visibility looks at, `UserTaskVisibility.TaskFacts` and `WorkflowVisibility.WorkflowFacts`. Then
every stream decides in memory, with `UserTaskVisibility.letsThrough` and
`WorkflowVisibility.letsThrough`. `UpdateStreamAudience` is the interface behind which this sits.

So the rule of a visibility exists twice: as the query of the lists, and in Java. That is the price
of a load which does not grow with the number of open tabs. Asking the database once per stream
was built first and measured: 450 streams cost 450 queries and about 100 ms per tick on one machine,
and a network between the cockpit and MongoDB would make it about half a second.
`TheVisibilityInMemoryAgreesWithTheQueryTest` keeps the two versions together. It compares them for
every combination of the fields they read and for a fixed sample of views built from the record
components. It also fails when the query reads a field the facts records do not carry. Whoever
changes one version of the rule changes the other one in the same commit.

The filtering tick runs every 250 milliseconds by default,
`business-cockpit.gui-sse.filtering-interval`. With one query per tick that costs below four
queries a second even under heavy load, and a change reaches the browser as fast as it did before
the filter existed.

#### What a stream remembers

A stream remembers the ids the lists of its person were answered with, by `getUserTasks`,
`getUserTasksUpdate` and their workflow counterparts. A request of a list does not say which tab it
comes from, so every stream of the person remembers the answer. The ids the browser sends along in
an update request are not remembered: the browser could name any id, and would then hear about
changes of tasks it may not see.

The memory is limited, `business-cockpit.gui-sse.max-known-ids-per-stream`, 2,000 by default, for
user tasks and workflows together. Above it the stream keeps the ids of the answer which overflowed
it, forgets the rest, and sends a reload event for every kind which lost an id. A reload event is a
wake-up call of the kind with the type `RELOAD` and an empty id. The user interface already loads
its whole list on every wake-up call.

A new stream starts with a reload event for every kind. Its memory is empty, but the page may have
loaded a list before the stream opened, and after a reconnect the browser still shows the lists of
the old stream.

#### When a stream ends

A stream ends when the sign-in it was opened with expires. The browser connects again and gets a
stream with the rights of its new sign-in. That is how a change of rights reaches the stream, a
substitute for example, without a second mechanism for substitutes. A sign-in which names no end
keeps its stream for as long as the tab is open.

### 49. 'npm update' lifts our own packages only - superseded by decision 62

#### What was wrong

The build step 'npm install' of the root POM, of `development/simulator` and of
`development/dev-shell-angular` ran `npm update --scope @vanillabp/**`. The plan was to fetch the
newest builds of our own packages. But `npm update` has no filter. Only `init`, `publish`,
`search` and `login` read `--scope`. So the step lifted every npm dependency of the module to the
newest version its range allows, and wrote that into the checked-in `package-lock.json`. A build
of `apis/official-gui-api/client`, which needs none of our packages, changed 103 lines of
`@rollup/*` entries there. The release commits what its build changes, so every release also
shipped whatever npm had published that day.

#### What is decided

The step names the packages it may lift: the ones this repository publishes itself. The list is
the property `npm.own-packages` in the root POM. All three POMs call
`npm update ${npm.own-packages}`. npm skips a name the module does not use, still fills
`node_modules` from `package-lock.json`, and leaves the file alone. So one list serves every
module. Whoever adds a package to the repository adds its name to the list.

Updates of every other npm dependency come from Renovate, as they do for Maven, Docker and the
GitHub actions. A release no longer lifts them.

Today Renovate does not look at npm in this repository. `renovate.json` switches the npm manager
off until there is a reference interface to keep current. Until then the versions of other npm
packages stay as `package-lock.json` has them. This is wanted: the code which uses them will be
rewritten or dropped.

### 50. A creation which arrives late corrects the start, if its start is earlier

Related entries: 36 (a change of a case the cockpit never saw created creates the case) and 45 (an
end which arrives alone gives the record a start). This entry changes the section "What comes after
it" of 36, and the last sentence of the section "What still waits for its creation" of 45.

#### What was wrong

A change of a case the cockpit does not hold creates the case (decision 36). The case gets the time
of the change as its `createdAt`. A creation which arrived later stored nothing, because only a
record known from its end alone waits for its creation. A creation which was only late, and not
missing, could not correct the start. The case kept the time of its first change as its start for
good. User tasks had the same rule and the same gap.

#### What the cockpit does now

A creation of a case or a user task the cockpit already holds compares its timestamp with the
stored `createdAt`:

- If the creation is earlier, its timestamp becomes the new `createdAt`. Nothing else is taken from
  the creation. The change which created the record is younger, so the rest of the record stays as
  the change stored it. `latestEventAt` stays as well.
- If the creation is at the same time or later, nothing is stored, as before. The log says so, as
  for any report which changes nothing.

There is no new field and no new mark. The rule only compares two timestamps, so it holds for every
record the cockpit already holds, however it got its start. The rule is in
`OrderOfReports.isEarlierThanTheStoredStart` and is used by `WorkflowlistService.reportCreatedWorkflow`
and `UserTaskService.reportCreatedUserTask`. Both ways in, REST and Kafka, go through these two
methods.

#### What stays as it is

A record known from its end alone (decision 45) still takes everything the creation reports and the
start of the creation, as 45 says. Where the end reported the start, the two are the same. Where it
did not, the end is later than the creation. So such a record gets the same start under both rules.

`createdAt` is still never empty (decision 45). The user interface still shows the time of the first
change until the creation arrives.

#### What it costs

A creation now writes to a record it used to leave alone, when its start is earlier. A record whose
start changes shows up again in the update stream of a browser that shows it. If the creation never
arrives, the case keeps the time of the change as its start, as before.

### 51. A login is renewed while it is used, up to a maximum counted from the login

The cockpit's own token lived twelve hours and was never renewed. Since the update stream ends with
the token (decision 48), the whole user interface logged out after twelve hours, even in the middle
of work.

#### What is decided

- A request whose token has less than half of `business-cockpit.jwt.cookie.expires-duration` left
  gets a new cookie. The new token lives `expires-duration` again. `JwtRenewalFilter` does this. It
  runs right after `PassiveJwtSecurityFilter` and reads the authentication that filter restored.
- A login never lasts longer than `business-cockpit.jwt.cookie.max-login-duration`, seven days by
  default, counted from the login. The first token is cut to it as well. When the limit leaves no
  more time than the old token has, nothing is renewed.
- The time of the login travels in the claim `auth_time`, the name OpenID Connect uses for it. A
  renewal keeps it. A token without it counts from its `iat`. Such a token was issued before this
  change, or by an application of its own, and was never renewed, so `iat` is its login time.
- A renewal copies all claims of the old token. Only `iat`, `exp` and `jti` are new. So the user and
  the groups stay as they were at the login. A group somebody gets or loses reaches the cockpit at
  the next login, and `max-login-duration` is now the longest that can take, no longer
  `expires-duration`.
- The update stream does not renew (`WebSecurityConfiguration.updatesRequestMatcher`). It connects
  again by itself, so a tab nobody looks at would otherwise stay logged in for the whole maximum.
  Every other request with a valid token renews, including `current-user` and the proxy to the
  workflow modules.
- Both durations are checked when the application starts. A value which is no ISO-8601 duration
  greater than zero stops the start (`JwtLifetimeIsNotUsableException`). A maximum shorter than
  `expires-duration` boots with a warning, because it switches renewal off.

#### What it costs

- An open stream still ends when the token it was opened with expires. With renewal that happens
  once per token lifetime while somebody works, and the browser connects again with the new
  cookie.
- Several requests in the second half of a token each get a new cookie. That is harmless: the
  browser keeps the last one.
- A login by standard OAuth would bring its own renewal, by refresh tokens. If the cockpit moves to
  it, this decision goes away together with today's login.

### 53. The SPI only promises what reaches the cockpit

A method of the SPI for Java has to change what the cockpit shows or does. A method which gets lost
on the way to the cockpit does not belong in the SPI.

#### What was removed

`WorkflowDetails#getDetailsCharacteristics()` and the interface `DetailCharacteristics` it returned.
For each key of `details`, it said whether the value was sortable and whether it was filterable.

The one implementation, `WorkflowEvent` in `extensions-commons/core`, always returned an empty map.
The BPMS API of the cockpit has no field for it, so no value could ever reach the server. No adapter
and no API description used the name. Somebody who set it would see no effect and still believe
the cockpit followed it.

#### Removed, not deprecated

Version 1.0 is the first version on VanillaBP 2. A method which never did anything needs no time
to phase out. After 1.0 the same change would break the SPI.

#### If it comes back

Sorting and filtering by a detail needs a field in the BPMS API and a viewer which reads it. Once
both exist, the SPI can get a method for it again.

### 54. The report which creates a user task says who sees it

Related entries: 19 (an end overwrites what it reports), 36 (a change of a record the cockpit never
saw creates it), 45 (an end which arrives alone gives the record a start) and 50 (a creation which
arrives late corrects the start). This entry changes the sentence of 36 which lists what a second
change writes, for user tasks, and the sentence of 45 "The creation still fills in what the end
could not report", for who sees a task.

#### What was wrong

A change of a user task replaced its candidate groups and its excluded candidate users. It replaced
the assignee too, where it named one. So a workflow module could give an open task to other people
by reporting it again. That undid a takeover in the cockpit without anybody noticing. It also made
it hard to tell later why a person worked on a task.

#### What the cockpit does now

Four fields say who sees a user task: `assignee`, `candidateUsers`, `candidateGroups` and
`excludedCandidateUsers`. The report which creates the task in the cockpit sets them. After that no
report changes them: no change, no end, and no creation which arrives late. Only the cockpit itself
still changes the assignee and the candidate users, when somebody claims the task or assigns it to
somebody.

`admittedUsers` is not part of this. Every report may set it, because it only adds readers. An end
which admits nobody leaves the stored list alone, as decision 19 says for every list of an end.

The report which creates the task is usually the creation. A change or an end can arrive first and
create the task (decisions 36 and 45). Then that report says who sees the task, and the creation
which arrives later changes nothing about it. So a change and an end carry the four fields as well,
and the server decides: if it does not hold the task yet, it uses them, and if it does, it ignores
them.

One case is different: a dangling task. A task which names no assignee, no candidate user and no
candidate group is dangling, and the cockpit shows it to everybody. `UserTask.isDangling()` is the
existing definition, and the cockpit stores the flag from the fields every time it saves the task.
A dangling task takes who sees it from the first later report which names somebody: a change, an
end, or a creation which arrives late. After that it is no longer dangling, and the rule above
holds.

Version 1 of the API is deprecated. In version 1.1 every report carries all values, so a task which
a report of version 1.1 created is dangling only if it really names nobody. The exception covers
what is left: ends of version 1, which carry none of the four fields, and ends of a BPMS which could
no longer describe the task when the end was reported.

The rule holds on every way in: REST version 1, REST version 1.1 and Kafka. It lives in one class,
`io.vanillabp.cockpit.bpms.WhoSeesAUserTask`, next to `OrderOfReports`. `UserTaskService` calls it
for every change and end of a task it holds, and for every creation of a task it holds. The
mappers map the four fields like any other field. The old special case, where an update kept the
assignee if it named none, is gone, because the rule covers it.

A report which names other people than the cockpit holds is no mistake: the workflow module cannot
know whether the cockpit already holds the task. So the cockpit writes a DEBUG line about it and no
warning.

Cases are not part of this. `accessibleToUsers` and `accessibleToGroups` of a workflow stay
changeable, because the group of people who may read a case can grow for a business reason.

#### How a workflow module hands work to other people

It ends the task and enters it again, for instance with a boundary event which leads back into the
same user task. The new task is a new record in the cockpit, and its creation says who sees it.

#### What was rejected

- A provider type per point in the life of a task. It would be clean, but it breaks the SPI and
  puts the topic in front of every developer.
- Setters which do nothing once the task exists. The extension cannot know for sure whether the
  cockpit holds the task, because the reports arrive in no fixed order.

#### What it costs

A module which used to reassign tasks by reporting them again has to model that in BPMN. A creation
which arrives late after an end that named people cannot correct them, even where the end was
reported from an older state.

### 55. A list request without paging values gets defaults

#### What was wrong

The OpenAPI document of the GUI API calls `pageNumber`, `pageSize` and `sortAscending` optional.
The controllers of the task list and the case list passed them on as `int` and `boolean`. So a
request which left one of them out ended in a `NullPointerException` and HTTP 500, for
`POST /usertask`, `POST /workflow` and both updates with `PUT`. The answer even carried the text of
the exception. A client which kept to the document got a server error for nothing.

#### What the cockpit does now

- `pageNumber` defaults to 0, the first page.
- `sortAscending` defaults to `true`, in the list requests and in the update requests.
- `pageSize` has no default. A missing page size is answered with 400 and the body
  `The request is not valid: 'pageSize' is missing.` How many rows a page holds is up to the user
  interface. A value the server picked would only hide that the client forgot it.
- A page size below 1, an update size below 1 and a page number below 0 are refused with 400 by
  the schema itself, which now carries `minimum` for them.

The defaults are written into the OpenAPI document, and the class `io.vanillabp.cockpit.util.ListPaging`
applies them. The generated model sets the same defaults when a field is left out, but a client can
still send `null`, so the controllers do not rely on that.

`POST /workflow/{workflowId}/usertasks` keeps its own default of 100 for `pageSize`. It shares the
request type with the task list, which is why `pageSize` is not `required` in the schema.

#### How the 400 is built

A controller which finds a mistake throws `BcInvalidRequestException` from `commons`.
`RestfulExceptionHandler` answers it in the same words as a request which breaks its schema. So a
client reads one kind of answer, whichever check found the mistake.

#### Tests

`AListWithoutPagingValuesTest` in `business-cockpit` sends the requests through Spring MVC, with
the real bean validation and the real exception handler.
`RestfulExceptionHandlerTest#aMistakeAControllerFoundIsAnsweredLikeABrokenSchema` holds the wording.

### 56. The unused field `query` leaves the GUI API

#### What was wrong

`UserTasksRequest` and `UserTasksUpdateRequest` of the GUI API had a field `query`. No code read
it. The task list searches by `searchQueries` only, and a `SearchQuery` without a `path` is the
full-text search. A client which set `query` got every row and no error. `WorkflowsRequest` and
`WorkflowsUpdateRequest` never had the field.

#### What the cockpit does now

The field is gone from the OpenAPI document, and so from the generated Java model and the
generated TypeScript client. No caller in this repository set it: not the user interface in `ui`,
not the development shells and simulators in `development`, not the tests.

Implementing it instead was the other choice. It was rejected because `searchQueries` already does
the job, and because removing it before version 1.0 breaks nobody who used it, since it never did
anything.

#### What it does not change

The server still ignores fields it does not know, as Spring Boot's JSON reader does by default. So
an old client which still sends `query` gets the same answer as before: every row and no error. It
only stops being promised by the API document. Refusing unknown fields would be a change for every
endpoint and is not part of this decision.

### 57. A sort path cannot create indexes without limit

#### What was wrong

The lists of user tasks and of workflows are sorted by the paths in the field `sort` of a request.
The server created a MongoDB index for every combination of paths it had not seen before. It did
not check the paths, and it had no limit. So every logged-in user could create indexes. Measured on
2026-10-07 against the cockpit of this repository: a hundred requests with a hundred unknown paths
all answered 200, and the collection `usertask` went from 0 to 61 sort indexes. That is 64 indexes
with the cockpit's own, which is all MongoDB takes. After that no new index could be created by
anybody, and every index makes each write a little slower.

Since every application can build its own user interface, the one which ships with the cockpit and
sends column paths only does not protect the server any more.

#### What the cockpit does now

A path in `sort` names one of two things:

- a field of the list: a field of `UserTask` or `Workflow` in the GUI API which the server stores
  under the same name, like `dueDate`, `title.de` or `assignee.sort`;
- a key of the business data below `details.`, like `details.customer.name`.

Each part of a path holds letters, digits and `_` only, and below `details.` the `-` as well. A
workflow module may report a key like `order-id`, and its column stays sortable (the maintainer,
2026-10-07). Where `business-cockpit.mongodb.map-key-dot-replacement` is set, the replacement may
appear below `details.` too, because a column names such a key in its stored form. Any other path
is answered with 400, for the list and for its update, and no index is created.

At most `business-cockpit.mongodb.sort-indexes-per-collection` sort indexes exist per collection,
30 by default. A sort index is an index whose name starts with `_sort_`. The cockpit counts them in
the database, so several instances share the limit. Once it is reached, a new combination is
sorted without an index of its own, and the server warns once per combination. The warning names
the key and says how to free a place. The value is checked at startup: a whole number from 0 to 60.
60 leaves room for the cockpit's own indexes below MongoDB's 64.

Both lists use one class, `io.vanillabp.cockpit.util.SortIndexes`. It replaces two copies of the
same code, which kept their state in static fields.

#### What was rejected

- An allowed list of paths per workflow module, taken from the columns a module declares. The
  columns live in the user interface of the module, and the server never sees them.
- Refusing a new combination once the limit is reached. The list would break for a user who only
  clicked a column, while sorting without an index still gives the right answer.

#### What it costs

A key of the business data with a space or another character outside the rule, like `order id`, can
be shown in a column but no longer sorted by. Before, it was sorted and got an index. The module can
report it as `order_id`, or mark the column as not sortable.

#### Tests

`SortIndexesTest` in `business-cockpit` holds the rules against a real MongoDB.
`StartupConfigurationCheckTest` holds the startup check of the limit. `SortPathsTest` in `container`
holds both lists through HTTP, including a hundred new paths which leave 30 sort indexes per
collection.

#### Filters follow the same rule

The path of a filter (`SearchQuery.path`) and the parameter `path` of the suggestions for a search
field went into the database query unchecked as well. They create no index, but a client could
filter on a field the GUI API never shows and guess its values from what the list answers. Since
2026-10-07 (story 1463) both follow the rule above, for both lists, and anything else is answered
with 400, naming `searchQueries.path` or `path`. A filter without a path is the full-text search
and stays allowed. The rule lives in one class, `io.vanillabp.cockpit.util.ListPaths`, which
`SortIndexes` uses as well. `ListPathsTest` and `FilterPathsTest` in `container` hold it.

### 58. `OpenTasksWithFollowUp` is another name for `OpenTasks`

#### What was found

A list of user tasks has six modes. `UserTaskService.buildUserTasksCriteria` treats four of them as
open tasks: no `endedAt`, or one after the time the list was first read. Two of them then add a
condition on `followUpDate`:

- `OpenTasksWithoutFollowUp` keeps tasks without a follow-up date or with one which is due.
- `OpenTaskOnlyFollowUp` keeps tasks with a follow-up date in the future.

`OpenTasks` and `OpenTasksWithFollowUp` add nothing. So both show every open task, with or without
a follow-up date. The name `OpenTasksWithFollowUp` sounds like a filter, but no code ever had one,
and no comment said what it was meant to be.

#### What the cockpit does now

Nothing changes in what a list shows. `OpenTasksWithFollowUp` stays in the GUI API as another name
for `OpenTasks`. The OpenAPI document describes all six modes, and so do the comments of the enum
`UserTaskService.RetrieveItemsMode`.

Changing `OpenTasks` to hide tasks with a follow-up date in the future was the other choice. It was
rejected because `OpenTasks` is the default of `PUT /usertask` and of the suggestions of a search
field, and because `OpenTasksWithoutFollowUp` already shows exactly that list.

The dev-shell simulator in `development/dev-shell-simulator` answered `OpenTasksWithFollowUp` with
every task, ended ones included. It now answers it like `OpenTasks`, the same as the cockpit.

### 59. The extension knows which variables its details providers read - replaced by decision 60

A `@UserTaskDetailsProvider` method may take a process variable with `@TaskParam`. Camunda 7 hands a
BPMS half every variable of a task, so the value is there. A Camunda 8 job carries only the
variables its worker asked for, so the Camunda 8 half has to know the names before the first task
arrives. Stephan decided on 2026-10-09 that `@TaskParam` stays and gets its value on Camunda 8 as
well.

The extension now notes the names. `DetailsProviderTaskParams` reads the `@TaskParam` parameters of
each provider in the annotation check of the `@UserTaskDetailsProvider` contract. VanillaBP runs that
check for every method it scans and binds, so the extension reads the same methods and walks no class
a second time. The names are kept by lookup key: the element id, the task definition, the method's
name where the annotation names neither, and `*` for a provider of every task.

A BPMS half asks with `BusinessCockpitEventPublisher.variablesTheDetailsProvidersRead(taskDefinition,
bpmnTaskId)`. It is a default method which answers an empty list, so a half which does not ask is
not affected. The answer is empty too where the application reports no user tasks.

The names are not kept per workflow module or per BPMN process. The annotation check shows a method
and its annotation, but not the process the method was registered for. So the answer for a task is
the union over every provider of the application serving a key of the same spelling. A half may
therefore fetch a variable which a provider of another process reads. It never misses one.

The platform would be the better place for the question. It knows each method together with its
module and process, and it answers the same question for `@WorkflowTask` methods with
`WorkflowTaskWiring.taskParameterNames`. `ExtensionHandlers` has no such method yet. Once it has
one, `DetailsProviderTaskParams` can ask it and drop its own list, and the answer gets as narrow as
the one for workflow tasks.

Since 2026-10-09 the platform has that method. Decision 60 says what the extension does now.

### 60. VanillaBP says which variables the details providers of a task read

Decision 59 kept its own list of the names, and the list knew nothing of workflow modules or BPMN
processes. VanillaBP now answers the question itself, with
`ExtensionHandlers#taskParameterNames(annotationType, workflowModuleId, bpmnProcessId, lookupKeys)`.
It resolves the keys the same way it binds a provider for that place, per workflow module and BPMN
process. It returns only the `@TaskParam` names it binds, sorted and without duplicates.

So the extension keeps no list any more. `DetailsProviderTaskParams` is gone, and so is the
annotation check which filled it. `BusinessCockpitHandlers.userTaskContract()` takes no parameter
again.

`BusinessCockpitEventPublisher.variablesTheDetailsProvidersRead` now takes four values:
`workflowModuleId`, `bpmnProcessId`, `taskDefinition` and `bpmnTaskId`. The extension builds the
keys with `BusinessCockpitHandlers.lookupKeysOf`, element id first, and passes the question on with
`UserTaskDetailsProvider` as the annotation. Where the application reports no user tasks, the answer
is still empty and VanillaBP is not asked.

The old method with two values is removed, not kept beside the new one. Only the Camunda 8 half
called it, and it moves to the new method in the same change. A half which never asked is not
affected: the method is still a default method which answers an empty list.

The answer can still name a variable too many, but now for a different reason. Where several
providers of one key split the versions of a process between them, VanillaBP also counts the
provider for every task (`*`), although it may never run in this process. A provider of another
process no longer counts. A worker which serves a task in several BPMN processes asks once for each
process and fetches all the names it gets.

`VariablesTheDetailsProvidersReadTest` holds what the extension passes on.

### 61. Notifications form one group per business case

A user sets notifications for all workflows and, as an exception, per workflow. Until now a workflow
here was the workflow module and the BPMN process of the user task. A task of a process which the
case started by a call activity therefore formed a group of its own, named after the called
process. To the user that is one process, only split into several models to keep them readable. So
the group is now the process of the case. A called process with a workflow aggregate of its own is a
case of its own and keeps its own group. This holds for every BPMS.

The server cannot tell the two kinds of called process apart. It follows the case the workflow
module names: the `workflowId` of the task. So a called process with an aggregate of its own gets a
group of its own wherever its module files its tasks under a workflow of its own.

The cockpit learns the case of a task from the task's `workflowId`. That is the case the workflow
module files the task under, so the process of the case is the process of the workflow stored under
that id. Where the cockpit holds no such workflow, the task counts for its own process. That happens
while the report of the workflow has not arrived yet, and for a module which reports no workflows.
The rule is read when it is needed and not stored on the task:

- `CaseProcess.of` reads the workflows of all tasks of one poll cycle with one query. The
  notification poller passes the result to `NotificationScanner`, which looks the user's setting up
  by it.
- `UserTaskService#getVisibleWorkflows` lists the groups for the settings page. It joins the
  workflows in the same query (`$lookup`) and groups by the case's process. The title of a group is
  the `workflowTitle` of a task of the case's own process, and only where the user sees none, the
  title of a task of a called process.

So nothing stored has to change. A task stored before this change counts for its case as soon as
the case is stored as well.

A setting a user made for a called process before this change is no longer asked. Its key stays in
the user's configuration, and the settings page offers the case's process instead. Copying it
over was left out: one called process may belong to several cases, and its setting may contradict
the one the user made for the case.

`getVisibleWorkflows` now runs as a typed aggregation. Untyped, its criteria were not mapped:
`candidateGroups.id` and the other nested ids are stored as `_id`, so a user whose tasks reach them
through a group or as a candidate saw no workflow on the settings page.

`NotificationScannerTest` and `ATaskOfACalledProcessCountsForItsCaseTest` hold the rule, the second
one against a real MongoDB.

### 62. The Maven build builds no user interface

Decided on 2026-10-09.

No module of this repository runs npm any more. The `frontend-maven-plugin` is gone from every POM,
and so are the `skip.npm*` properties, `npm.registry`, `npm.own-packages` and the profiles
`java-install`, `local-install`, `unpublish-npm`, `prerelease-npm`, `release-npm` and
`skip-release-npm`. The workflows run no npm step either. This replaces decision 33, which switched
the npm side off, and decision 49, which said what the npm step 'npm install' may update.

#### Why

The user interfaces are moving to repositories of their own. Until they do, keeping their build
alive here costs work and buys nothing. The pull request build left the npm side out anyway, with
`-Pjava-install`, and so did the publish of main. Only the job `frontend` still checked the
packages, and it failed on the pull request which moved the snapshots to Maven Central. The
duplicate `frontend-maven-plugin` entries, which Maven 3.10 refuses, go away with this as well.

#### What changes

The npm sources stay where they are: `ui`, `development/dev-shell-react`, `-angular` and `-vue`,
`apis/official-gui-api/client`, `business-cockpit/src/main/webapp` and the two web applications of
`development/simulator`. Their `package.json` files and their sources are untouched. Decision 63
removed them a day later.

The modules which did nothing but run npm are no longer part of the reactor, and their POMs are
deleted: `ui`, `ui/bc-types`, `ui/bc-shared`, `ui/bc-ui`, `apis/official-gui-api/client` and the
three dev shells for React, Angular and Vue. Without npm they would build empty artifacts, and
`bc-types` and `bc-ui` would publish an empty jar.

`business-cockpit` no longer generates the TypeScript client of its web application. It still
generates the Spring server of the GUI API from the same specification. The Renovate rule which
held the OpenAPI generator below 7.0 for the TypeScript clients is gone as well.

So the jar of `business-cockpit` carries no user interface, and neither does the runnable jar of
`container` or of `development/simulator`. Nothing in `target/classes/static` comes from a build
any more. A path which no controller answers used to get the shell of the single-page application.
Now `SpaNoHandlerFoundExceptionHandler` answers it with 404 when the application has no shell. An
application which brings its own shell at `application.spa-default-file` still gets it.

A release publishes no npm package, to npmjs.com or anywhere else. The release workflow has no
input `npm-bypass-2fa-token` any more, and it leaves the versions in the `package.json` files as
they are. The npm packages published so far stay where they are. The publish of main no longer
deletes old npm snapshots on GitHub Packages.

#### What points at this

| place                                                                   | what it says |
|-------------------------------------------------------------------------|--------------|
| `pom.xml`, above the modules                                            | no module runs npm |
| `business-cockpit/pom.xml`, the OpenAPI generator                       | the TypeScript client is no longer generated |
| `SpaNoHandlerFoundExceptionHandler`                                     | 404 when the application has no shell |
| `.github/workflows/build.yaml`, `publish-snapshots.yaml`, `release.yml` | no npm step |
| `renovate.json`                                                         | no pin of the OpenAPI generator |
| `README.md`, `AGENTS.md`, `development/README.md`                       | building without npm |

### 63. The repository holds no user interface and no npm package

Decided on 2026-10-10.

Everything with a `package.json` is gone from this repository:

- the React application in `business-cockpit/src/main/webapp`
- the packages `ui/bc-ui`, `ui/bc-shared` and `ui/bc-types`
- the TypeScript client in `apis/official-gui-api/client`
- the dev shells `development/dev-shell-react`, `-angular` and `-vue`
- the two web applications of the simulator, `development/simulator/src/main/webapp-react` and
  `webapp-angular`

With them went what only served them: `openapi-generator-fixes`, whose templates were for the
TypeScript client alone, the local npm registry in `development/verdaccio` and its service in
`docker-compose.yaml`, the npm scripts in `.github/scripts`, and the Renovate rule which switched
npm updates off. Decision 62 had left the sources in place. This one removes them.

#### Why

Stephan, on 2026-10-10: no npm builds in this repository any more, and the dev shell user
interfaces leave as well. A dev shell has to fit the user interface it stands in for, so it belongs
in that interface's repository. No new repository is made now. The Git history holds the sources,
and Stephan picks later what he keeps.

#### What stays

`simulator` and `dev-shell-simulator` stay as Java modules. Both answer the server side and need no
user interface for that. The simulator reports user tasks and workflows to a cockpit and has its
test data form as a server page. The dev shell simulator answers what a dev shell asks, and the
local user directory of `container` loads its users from it. A task the simulator reports still names
`/remoteEntry.js` as its form, which nothing serves now.

`container` builds a jar without a user interface. `SpaNoHandlerFoundExceptionHandler` stays. An
application which puts a shell at `application.spa-default-file` still gets it for every unknown
path, and one without a shell answers 404. The container tests used a stand-in `index.html` to
show the shell. That stand-in is gone, and the tests now check the 404.

The npm packages published so far stay on npmjs.com and on GitHub Packages. No new version comes
from here.

#### What points at this

| place                                                                 | what it says |
|-----------------------------------------------------------------------|--------------|
| `pom.xml`, above the modules                                          | no user interface and no npm package here |
| `business-cockpit/pom.xml`, the OpenAPI generator                     | the TypeScript client is gone |
| `.github/workflows/publish-snapshots.yaml`, `release.yml`             | Java artifacts only |
| `UnknownPathsTest`, `ApplicationWithoutTheBaseClassTest`              | 404 for an unknown path |
| `README.md`, `AGENTS.md`, `UPGRADE.md`, the module READMEs, the skill | no user interface here |
