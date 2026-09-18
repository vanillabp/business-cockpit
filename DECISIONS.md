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

### 18. The timestamp of the event decides which report the cockpit stores - the state a report carries superseded by decision 26

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

### 26. The report is built at the event and travels with its entry

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
names the case. The payload of an entry is removed when the entry was dispatched, and the outbox
housekeeping removes what a crash left behind, so an entry can only meet this after waiting longer
than `vanillabp.outbox.retention`. It is the same answer this extension gives an end whose BPMS said
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
