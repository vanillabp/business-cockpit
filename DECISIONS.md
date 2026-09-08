# Decision log

Decisions this repository's code points at. A number is handed out once and never reused or
renumbered, so a citation stays resolvable; a decision which gets overturned keeps its entry,
marked as superseded and naming the entry which replaced it.

A citation in code reads `see decision 3 in the repository's DECISIONS.md`, and it names an entry of
THIS repository only. A decision which the platform shares has its own entry in
`adapter-platform-integration`, written from that side, and one a BPMS half of the extension shares
has its entry in that adapter's repository; a pointer into another repository is the fragile kind
this log exists to avoid.

### 1. The generated BPMS API client names its own dependencies

`apis/bpms-api/client` used to reach everything it compiles against through the cockpit's `commons`
artifact, which carries Spring Boot, Spring Data MongoDB and Spring Security with it. Nothing
generated there touches any of that: the OpenAPI generator's Feign library with `interfaceOnly`
emits Feign, Jackson and `jakarta.annotation` and nothing else.

The client now names those nine artifacts itself. A client which drags a web framework into every
consumer cannot be used from a module which has to stay free of both platforms, and that module is
the point of `extensions-commons`. The two existing consumers declare `commons` themselves already,
so nothing they use disappeared.

### 2. The neutral half of the extension compiles against no platform integration

`extensions-commons/core` sees the two VanillaBP SPI artifacts, the platform-neutral
`migration-adapter`, and the cockpit's own API artifacts. It does not see
`vanillabp-spring-boot-integration` or `vanillabp-quarkus-integration`, and it does not see Spring
or CDI.

What that costs is visible: each platform module carries a `TransactionRunner` of its own, doing
what the platform's own runner does. What it buys is that a feature cannot quietly exist on one
platform only. VanillaBP asks the same of every extension, and its own sample extension is built
that way, so this is the shape a reviewer of either repository expects.

### 3. An outbox entry carries identifiers, and the event is built when it is dispatched

VanillaBP's outbox holds at most 2048 characters of arguments per entry, by contract, so version
1's approach of serializing the whole event into the entry is not available. An entry therefore
carries the kind of event, the adapter id, the identifiers of the task and the workflow, the task
definition, the BPMN element id, the event's own id and its timestamp.

Everything else is read while the entry is dispatched: the BPMS half is asked what it knows now,
the application's details provider is invoked, the titles are rendered, and the result is sent.

Two things follow, and both are wanted. Several pending reports about one task collapse into one,
because the surviving entry reports everything the discarded ones would have reported. And a change
a details provider makes to the workflow aggregate is committed while the entry is dispatched, not
with the transaction the BPMS event arrived in - a provider which writes is writing later than it
looks.

### 4. The idempotency key of a report names the operation, the BPMS, the entity and the kind

`<operation>|<adapterId>|<taskId or workflowId>|<eventKind>` for a task or a workflow, and
`<operation>|<workflowModuleId>` for the registration of a workflow module. Every key starts with
the operation, the way the core's own keys do.

Two pending updates of one task therefore share a key and only one survives. That is the point
rather than a side effect: by decision 3 the surviving one reports the current state. The
BPMS' own event id is deliberately not part of the key, and no clock reading is - version 1 built
an event id from `System.nanoTime()`, which made every repetition a new event.

The keys are a persisted contract. An entry written before an upgrade is deduplicated after it, so
an operation is never renamed and a derivation rule is never changed.

### 5. The extension dispatches its own operations, and routes them by adapter id

The three operations are `businesscockpit:PUBLISH_USER_TASK_EVENT`,
`businesscockpit:PUBLISH_WORKFLOW_EVENT` and `businesscockpit:REGISTER_WORKFLOW_MODULE`, all of them
`Election.OWN_DISPATCH`: nothing is elected by the core, the extension dispatches them itself.

Which BPMS half serves a dispatch is decided by the adapter id the entry carries, which the
election answered when the event was observed. An adapter id no BPMS half is registered for ends
the dispatch with a message naming the ids which are registered and the three artifacts which
provide one.

### 6. A workflow module registers itself through the same outbox, once the application is up

The registration is an outbox entry like every other report, so a cockpit server which is down
while the application starts does not stop the application from booting: the entry is written, the
boot continues, and the outbox keeps trying.

It is written when the application is up rather than while its workflow modules are deployed. On
Quarkus the outbox store creates its table in a startup observer of its own, which runs after the
deployment pipeline; an entry written any earlier would find no table. Both platforms do it at the
same point, so that the two halves stay comparable.

### 7. The `version` attribute of `@UserTaskDetailsProvider` is reserved and refused

Version-aware matching means picking a different method per deployed version of a process, and the
events this extension reacts to carry no process version: a task listener says which task fired,
not which version of the model it came from.

Version 1 documented the attribute and never read it, and applications wrote it believing it
worked. A value is therefore refused while the application starts, with a message naming the
attribute and the two attributes to match by instead. Nothing about the annotation changes for an
application which left it alone.

### 8. The extension owns one outbox store, chosen at startup - superseded by decision 10

The extension writes its entries into the application's single `PhaseTwoOutbox`. An application
which runs several of them is told so while it starts, with the stores named, rather than having
one picked for it.

Per-aggregate stores are what `PhaseTwoOutboxAware` is for, and they would be resolvable for the
reports made through `BusinessCockpitService`, where the aggregate class is known. They are not
resolvable for an event a BPMS observed: what arrives there is a workflow module, a BPMN process
and a serialized id, and no class. One store for the whole extension is the shape which is the same
in both directions.

Superseded by decision 10: one store for the whole extension was one store too few. An application
whose stores all come from `PhaseTwoOutboxAware` beans has no plain store bean at all and was told
to add a data source it already had; an application with two persistences did not boot although
the platform attributes a store to every aggregate of it; and where one of two stores was marked
as primary, entries were written into a store the aggregate's transaction never reaches, which is
the one thing an outbox must not do.

### 9. One event class per side, with the kind as a field

Version 1 had a class per kind of event, twelve of them, because its publishing dispatched on the
class. The extension has `UserTaskEvent` and `WorkflowEvent`, each carrying the kind, and the
transports switch on it.

The mappers are the place where the four kinds still look alike, because the cockpit's API declares
one schema per kind and the generator produced four classes for them. Folding those four mappings
into one would mean converting between generated classes, and converting a timestamp is exactly the
kind of thing which goes wrong without anybody noticing.

### 10. The outbox store is the one the workflow aggregate's transaction reaches - the Quarkus half superseded by decision 12

An entry is written into the store VanillaBP attributes to the workflow aggregate the report is
about, which is what the platform does for its own operations. Wherever the extension knows the
aggregate's class - every report made through `BusinessCockpitService` - it asks for that store and
writes there, so an application with a store per persistence, or a store of its own for one
aggregate, is served the way it expects.

An event a BPMS observed knows no class. It names a workflow module, a BPMN process and a
serialized id, and nothing in either SPI turns that into the class the aggregate has. Such an entry
therefore goes into the application's single store, and an application running several is told that
the report cannot be attributed rather than having a store picked for it. The registration of a
workflow module belongs to no aggregate at all and is written in a transaction of its own, so any
store carries it correctly; the first by class name is taken, so a restart writes it into the same
one.

On Spring Boot the attribution is VanillaBP's own `PhaseTwoOutboxResolver` bean, taken as the
interface the platform-neutral core knows. On Quarkus no such bean exists, so the extension does
what it can reach: the store an application named for the aggregate, else the single store. What
the platform does beyond that - attributing one of its two default stores to the persistence
managing an aggregate - needs classes which live in the Quarkus integration, which an extension
does not compile against (decision 2). A Quarkus application running two stores therefore names
the store of an aggregate itself, with a `PhaseTwoOutboxAware` bean, and the message asks for that
bean where it is missing.

Superseded by decision 12: the Quarkus half of this was a second attribution next to the platform's
own, and it got the same application wrong that the platform gets right. The Quarkus integration now
offers its resolver as a bean too, so both platforms are served by the one answer, and the moment it
is asked moved from the first report to the boot.

### 11. A failed report says whether repeating it can help

The outbox repeats what a dispatch threw, which is right for a cockpit server that was restarting
and wrong for a report the server will refuse every time: an entry retried forever occupies the
store and hides the defect. The transports therefore classify what they get back.

Over REST a status the server blames the client for - anything from 400 upwards below 500, except
the two which mean "later" - ends the entry permanently. 503 and 429 mean "not now" and give the
entry back with the time the server named in `Retry-After`, so the dispatching thread is free while
the entry waits. Everything else is repeated with the store's own backoff.

Over Kafka the client's own classification is used. What it marks retriable is a broker which is
busy, gone for a moment or leading another partition now. Everything else is the record itself: a
topic which does not exist, a message larger than the broker takes, a client which may not write
there.

An entry naming an adapter id no BPMS half serves stays repeatable, because the jar carrying that
half may be missing from one deployment and back in the next.

### 12. The store of an entry is VanillaBP's answer, asked while the application boots - the shared-store rule superseded by decision 13

The extension never attributes a store itself. It asks the platform's `PhaseTwoOutboxResolver` -
a bean on Spring Boot and, since the Quarkus integration offers it as one, on Quarkus as well - so
an entry of the extension is written where an entry of the core is written: into the store the
transaction of the workflow aggregate reaches.

The extension's own resolution was a copy which could not be complete. Which of the platform's two
default stores serves which persistence, and whether a default is switched on and usable at all, is
knowledge of the platform integration, and an extension does not compile against one (decision 2).
The copy therefore refused an application with two stores where the platform serves it, and returned
the store of the other technology where the platform refuses - an entry written next to the
aggregate instead of into its transaction, which is the one thing this outbox must not do.

Every workflow aggregate of the application is resolved once while it boots, the way VanillaBP
validates the outbox of its own process services. That settles two things. A store which cannot be
attributed ends the boot with the platform's own message rather than surfacing inside the
transaction of the first report. And the store all aggregates share is what an entry naming no
aggregate class is written into - an event a BPMS observed names a workflow module, a BPMN process
and a serialized id, and nothing in either SPI turns that into a class. An application whose
aggregates live in different stores has no such store and is told while it boots; letting it start
and fail at the first user task would be the same defect one report later.

Superseded by decision 13: "nothing turns that into a class" was wrong. The workflow service
serving the BPMN process names the aggregate it is written for, so an application whose aggregates
live in two stores is served rather than refused.

### 13. The BPMN process an event names is what its outbox store is looked up by

An event a BPMS observed carries a workflow module, a BPMN process and a serialized id, and the
store it belongs in is the one holding the workflow aggregate. The class is not carried, and it does
not have to be: `@WorkflowService` declares both halves - the aggregate the service is written for
and the BPMN processes it serves, the primary one and whatever it names as secondary. Reading the
annotations of the application while it boots turns every one of its BPMN processes into the
aggregate class, and that class into VanillaBP's answer about the store.

Two workflow services may declare the same process, one per generation of the model, and they are
written for the same aggregate then. Two aggregates on one process would make the store of a report
depend on which class was scanned first, so that ends the boot.

The registration of a workflow module belongs to no aggregate at all and is written in a
transaction of its own, so any store carries it correctly. Which one has to be the same after a
restart, because two entries of the same registration in two stores would be sent twice: it is the
store of the module's first aggregate by class name, and the module's aggregates are known because
VanillaBP says which module deployed which BPMN process while it wires them.

What is left of decision 12 is its point: the extension never attributes a store itself, it asks the
platform's resolver. What changes is that an application with two persistences no longer has to
choose between the Business Cockpit and its own architecture. A report whose BPMN process belongs to
no workflow service is still refused, with the declared processes named, because nobody can say
where it goes. An application holding a single store is spared even that: with one store there is
nothing to decide.

### 14. The configuration keeps the version 1 keys

An application upgrades to version 2 by changing a dependency. The Business Cockpit having become
an extension of the VanillaBP platform is an implementation of this repository, and there is no
reason an application should have to rewrite its configuration to learn about it. So the keys are
the ones version 1 read, in the places it read them: `vanillabp.cockpit` for what holds for the
whole application, `vanillabp.workflow-modules.<id>.cockpit` for one workflow module, and
`vanillabp.workflow-modules.<id>.workflows.<process>.cockpit` and its `user-tasks.<task>.cockpit`
for the two levels below it. Lists are lists again and the group hierarchy is a map of lists again,
because that is how an application already wrote them.

Neither platform reads anything below `vanillabp.cockpit` by itself, so the extension binds that
tree: a `@ConfigurationProperties("vanillabp")` overlay on Spring Boot and a second
`@ConfigMapping(prefix = "vanillabp")` on Quarkus, which is the pattern the platform documents for
an adapter contributing keys of its own. Both hand one neutral object to the core, which parses and
validates it, so a port which is no number and a timeout which is no span of time are answered by
the same message on either platform. On Quarkus the mapping is also what lets the application boot: a key
below `vanillabp` which no mapping declares ends the startup there, which is why every key the
cockpit reads is declared, including the one key its Process-Engine-API half reads.

That strictness is what a misspelled key runs into, and what a key of version 1 which is gone runs
into as well. On Quarkus the build ends naming the key and either the one it was nearest to or what
became of it; on Spring Boot such a key is ignored, which is the platform integration's own decision
about the two frameworks and not this repository's.

These keys do not come back, one reason each:

- `rest.log`: the client logs through SLF4J, so the logging configuration switches it on. Set the
  logger `io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi` to `DEBUG`.
- `rest.additional-get-parameters.<name>`: it appended query parameters to GET requests, and every
  report is a POST.
- `rest.retry.enabled`, `.max-attempts`, `.period`, `.max-period`: a failed report waits in the
  outbox and is repeated from there, which `vanillabp.outbox.*` configures.
- `kafka.group-id-suffix`: it made a consumer group unique, and the extension only produces.
- `jwt.hmacSHA256-base64` and `jwt.cookie.*`: version 1 bound them and read them nowhere, and the
  cockpit server configures its own tokens.
- `group-hierarchy-bean-name`: the hierarchy is configuration, and which groups may see a workflow
  module is answered by the `WorkflowModuleDetailsProvider` bean.
- `workerId`: it named the instance a report came from, and the extension takes the host name for
  that and asks for nothing.
- `spring.application.name`: only the Camunda 8 exporter path read it, and that path is gone.
- `spring.kafka.consumer.*` and `camunda.zeebe.kafka-exporter.topic-name`: version 1 also consumed
  a topic a Camunda 8 exporter wrote, and version 2 learns the same from the listeners it puts into
  the models.
- The Camunda 8 keys below `vanillabp.workflow-modules.<id>.adapters.camunda8`: a worker of the
  cockpit is a worker on the same cluster and is opened the way the Camunda 8 adapter opens its
  own, so the adapter's keys are read rather than copied. `workflow-visibility-timeout` is gone
  with the waiting it configured: a report of something the cluster has not exported yet goes back
  into the outbox, and `vanillabp.outbox.block-after-attempts` says how long it keeps coming back.
- `io.vanillabp.businesscockpit.tasklistener.prefixes`,
  `io.vanillabp.businesscockpit.executionlistener.prefixes` and `io.vanillabp.deployment.priority`:
  entries of an in-memory map of version 1's deployment, never keys of a configuration file. A job
  type of this extension is recognized by its own prefix.

One behaviour of version 1 comes back with its keys, and it is worth naming: the token client of
the client-credentials flow configures its own connection. It inherits nothing from `rest.*`, so an
authorization server behind another proxy, with another certificate or a slower answer stays
reachable, and what the flow does not say for itself is the default rather than what the cockpit
server's client uses.

Two keys move into the cockpit's own tree rather than being read out of Spring's:
`kafka.bootstrap-servers` and `kafka.properties.*` are what version 1 took from
`spring.kafka.bootstrap-servers` and `spring.kafka.producer.*`, and the extension builds its own
producer now, on both platforms. One key is new with the Process-Engine-API integration:
`process-engine-api.remembered-user-tasks` sizes what that half remembers between a delivery and
its dispatch.

What was mandatory per workflow module stays mandatory per workflow module, and there is no global
default for it. Version 1 had none either, and a `ui-uri-path` written once for every module would
promise something the cockpit cannot keep: the modules answer at different addresses. Every setting
is read and validated while the application starts, rather than when the first event arrives, so
the log of the first boot is what a developer works from.
