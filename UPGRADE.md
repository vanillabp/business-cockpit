# Upgrading

What an application built on the Business Cockpit has to change when it moves to a new version of
the cockpit. Each section names one version and lists only what breaks or changes. Everything the
section does not name keeps working.

The wiki page [Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases) tells the
whole story of a version. This file is the short list of what somebody has to touch.

## 0.8.x to 0.9.0

### The MongoDB migration is a library of its own

The cockpit no longer carries the MongoDB changeset mechanism. It uses
`com.phactum.mongodb:mongodb-changesets`, which does the same thing and is published on its own.
The `commons` module brings the dependency along, so an application which depends on the cockpit
gets it without naming it.

Nothing on disk changes. The collection is still called `ChangesetInformation`, a step is still
identified by the class name of its changeset bean plus the name of its method, and the cockpit's
own changeset beans stayed where they were. A database migrated by an earlier version runs no step
a second time.

### Read your `spring.autoconfigure.exclude`

The auto-configuration has a new fully qualified name.

| before | now |
|--------|-----|
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetAutoConfiguration` | `com.phactum.mongodb.changesets.ChangesetAutoConfiguration` |

An application which switched the migration off by the old name switches nothing off any more, and
it starts anyway. Spring Boot refuses an exclude entry only while the class it names is on the
class path. The old class is gone, so the entry is read and ignored, and the migration runs in an
application which had it switched off. Replace the name.

### A changeset bean of your own changes two imports

The annotations are called `DbChangeset` and `DbChangesetConfiguration`. They were called
`Changeset` and `ChangesetConfiguration` until 0.9.0, and they sit in another package now.

| before | now |
|--------|-----|
| `io.vanillabp.cockpit.commons.mongo.changesets.Changeset` | `com.phactum.mongodb.changesets.DbChangeset` |
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration` | `com.phactum.mongodb.changesets.DbChangesetConfiguration` |

Nothing else about such a bean changes. It is still a Spring bean, its steps are still public
methods which take a `MongoTemplate` or nothing, and they still answer with the command which
undoes what they did.

### A bean which waits for the migration asks for another type

Taking a parameter of the auto-configuration used to be how a bean said "build me after the
migration". It has to be `com.phactum.mongodb.changesets.ChangesetApplier` now. The
auto-configuration only does the wiring, and a configuration class is built before the beans it
declares, so a parameter of that type compiles, starts and guarantees nothing.

### `ChangesetInformation.timestamp` is an `Instant`

It was an `OffsetDateTime`. The document is the same either way, a BSON date, so there is no data
migration. Only code which reads the field in Java is affected.

### No configuration key changes

The library reads `mongodb.changesets.mode`, and the cockpit hands it the value of its own
`mongodb.mode`. So `mongodb.mode` stays the key to set, and `mongodb.changesets.mode` must not be
set: the cockpit binds the prefix `mongodb` strictly, and a key below it which the cockpit does not
know ends the start.

### `WorkflowDetailsProviders` is gone

The annotation `io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProviders` is no longer
published. It was the container of a repeatable annotation, but `@WorkflowDetailsProvider` is not
repeatable, so Java never put a value into it and nothing ever read one. Delete the import if your
code carries it. Nothing else changes, because the type could not be reached from a running
application.

A BPMN process has one workflow details provider, and that stays as it is. The method serves the
whole process, so there is nothing to repeat. `@UserTaskDetailsProvider` is a different case: it
stays repeatable and keeps its container `UserTaskDetailsProviders`.
