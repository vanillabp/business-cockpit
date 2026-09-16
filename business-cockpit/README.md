# The library

The Business Cockpit as a library: the services and the persistence behind the two lists, the GUI
API, the security extension points, the part which takes in what workflow modules report, the
notification feature, the proxy to workflow modules, and the React application under
`src/main/webapp`.

It has no main class and builds no runnable jar. What makes it start is [container](../container),
and that split is explained in [its README](../container/README.md#why-the-library-and-the-application-are-two-modules).
A custom cockpit depends on this module, which the wiki describes under
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).

Anything a custom cockpit would also need belongs here rather than in `container`. Built as part of
the reactor build described in the [root README](../README.md#building-it), and published as
`io.vanillabp.businesscockpit:business-cockpit`.

## How its beans reach an application

Through the auto-configurations in `io.vanillabp.cockpit.autoconfigure`, which Spring Boot reads from
this jar's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. There
used to be a base class carrying a component scan, and an application got the beans by extending it.

So a stereotype annotation alone does not make a bean here any more. A class added to
`io.vanillabp.cockpit` is named in the `@Import` of the auto-configuration of its area, and
`EveryCockpitBeanIsRegisteredTest` fails when it is not. The package documentation of
`io.vanillabp.cockpit.autoconfigure` says why it is written out rather than scanned, and
`container/README.md` says why the delivered application cannot be the one to prove it works.

## The write concern the cockpit needs

A write concern says when MongoDB counts a write as stored. The cockpit needs one which survives
a failover of the primary, it is configured in `mongodb.write-concern`,
and the cockpit says on every start what it is really writing with.

Nobody had written this down until now. The MongoDB changeset mechanism set `JOURNALED` on the
application's template while it migrated and never took it back, so the cockpit ran with that value
by accident for years. The migration is a library of its own today, and it gives the template back
the way it found it. What the cockpit writes with is therefore what the cockpit asks for.

### Where the value has to be written

On the cockpit's `MongoTemplate`, which is what `mongodb.write-concern` sets. Not in the connection
string, and not on the `MongoClient`.

The template checks the result of every write, because that is how Spring Data notices that
somebody else changed the same document first. A template which does that never reads the write
concern of its connection. `MongoTemplate.prepareWriteConcern` asks the template, and where the
template has nothing to say it writes `ACKNOWLEDGED`, which is one node. So a `?w=majority` in
`spring.mongodb.uri` is understood by the driver, thrown away by every write of the cockpit, and
nothing would say so. The startup check names it where it finds one.

The same rule swallows the driver's own `WriteConcern.JOURNALED`. That value says `j: true` and
leaves the `w` open, and `prepareWriteConcern` replaces anything with an open `w` by a plain
acknowledged write, journal flag included. This is why `mongodb.write-concern-journal` is only read
together with `mongodb.write-concern`.

### What each setting does

| `mongodb.write-concern` | carries | what happens |
|---|---|---|
| `majority` | yes | A write which a majority of the replica set has stays there when the primary steps down. This is what the delivered cockpit runs with. |
| `majority`, plus `mongodb.write-concern-journal: true` | yes | The same, and each acknowledging node has the write in its journal before it answers. Only needed where the replica set runs with `writeConcernMajorityJournalDefault: false`, and the delivered cockpit sets it because that setting belongs to the database rather than to the cockpit. |
| nothing, or `1` | no, warning | A write which only the primary has is gone when that primary steps down before another node got it. The cockpit has answered the BPMS adapter by then that the user task is stored, so the adapter never reports it again and the task never appears. It stays a warning, because an installation which runs one node has no failover to lose anything to. |
| `1` plus a journal | no, warning | The same. A journal protects the one node against a crash, not the replica set against a failover. |
| `2` and up | cannot be decided here, warning | Whether a number is a majority depends on how many nodes the replica set has, and the cockpit cannot see that. Below a majority it is the line above. On a three node set, `2` is a majority, and writing `majority` says that without depending on the size. |
| the name of a tag set | cannot be decided here, warning | Which nodes a tag stands for is written in the configuration of the replica set. Below a majority it is the same loss. |
| `0` | no, the start ends | Nobody acknowledges the write, so there is no result to read. Every document of the cockpit which carries `@Version` is saved by comparing the version, and Spring Data does that by reading how many documents the write changed. An unacknowledged write answers that question with an exception. The start ends even though Spring Data would quietly raise the value to one acknowledgement, because an installation which asked for `0` would otherwise run on a promise it never made. |

### What does not depend on the write concern

The user interface. The cockpit reads its own changes through a MongoDB change stream, and a change
stream only reports what a majority of the replica set has. So a write which a failover takes back
never reaches a browser, whatever the write concern was. What a weak write concern costs is the
other direction: the report is lost, and nobody notices, because the BPMS adapter was told it
arrived.

### Azure Cosmos DB for MongoDB

The cockpit knows this server through `mongodb.mode: AZURE_COSMOS_MONGO_4_2`. Cosmos DB speaks the
MongoDB protocol but is not a replica set, and how durable a write is comes from the consistency
level of the account rather than from the write concern the driver sends. So the table above
describes a server such an installation does not have.

Two things hold there all the same. `0` ends the start, because the cockpit needs the result of its
writes and that is decided in the driver and in Spring Data, not in the server. And `majority` is
the value to configure, because it is the one the cockpit does not warn about.

Which write concerns Cosmos DB honours, and what `j: true` means to it, could not be decided from
the code of this repository. An installation running on Cosmos DB reads the consistency level of
its account instead.
