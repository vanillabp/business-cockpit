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

A write concern says when MongoDB counts a write as stored. The cockpit needs one which survives a
failover of the primary, it is configured in `mongodb.write-concern`, and the cockpit says on every
start what it is really writing with. What each value costs an installation, and what the warning
on a start means, is in the wiki under
[The write concern](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern).
This section is about why the value has to stand there and nowhere else.

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

### Why `0` ends the start

Every document of the cockpit which carries `@Version` is saved by comparing the version, and
Spring Data does that by reading how many documents the write changed. An unacknowledged write
answers that question with an exception. The start ends even though Spring Data would quietly raise
the value to one acknowledgement, because an installation which asked for `0` would otherwise run
on a promise it never made.

### Azure Cosmos DB for MongoDB

The cockpit knows this server through `mongodb.mode: AZURE_COSMOS_MONGO_4_2`, and the startup check
reads that mode. Cosmos DB is not a replica set, so how durable a write is comes from the
consistency level of the account and a message about failovers would describe a server such an
installation does not have. What holds there instead is in the wiki section named above.
