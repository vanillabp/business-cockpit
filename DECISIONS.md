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

### 8. The extension owns one outbox store, chosen at startup

The extension writes its entries into the application's single `PhaseTwoOutbox`. An application
which runs several of them is told so while it starts, with the stores named, rather than having
one picked for it.

Per-aggregate stores are what `PhaseTwoOutboxAware` is for, and they would be resolvable for the
reports made through `BusinessCockpitService`, where the aggregate class is known. They are not
resolvable for an event a BPMS observed: what arrives there is a workflow module, a BPMN process
and a serialized id, and no class. One store for the whole extension is the shape which is the same
in both directions.

### 9. One event class per side, with the kind as a field

Version 1 had a class per kind of event, twelve of them, because its publishing dispatched on the
class. The extension has `UserTaskEvent` and `WorkflowEvent`, each carrying the kind, and the
transports switch on it.

The mappers are the place where the four kinds still look alike, because the cockpit's API declares
one schema per kind and the generator produced four classes for them. Folding those four mappings
into one would mean converting between generated classes, and converting a timestamp is exactly the
kind of thing which goes wrong without anybody noticing.
