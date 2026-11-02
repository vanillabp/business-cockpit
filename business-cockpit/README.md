# The library

The Business Cockpit as a library: the services and the persistence behind the two lists, the GUI
API, the security extension points, the part which takes in what workflow modules report, the
notification feature and the proxy to workflow modules. The jar carries no user interface, see
decision 63 in the repository's [DECISIONS.md](../DECISIONS.md).

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

## The cockpit's own part of the GUI API

Most of the GUI API is in [`apis/official-gui-api`](../apis/official-gui-api/README.md), because the
dev-shell simulator plays it as well. The rest is in `src/main/resources/api/gui/v1.yaml`: the
information about the application, the current user and the update stream. The simulator has none
of these. Its code is generated into the same package as the code of the official file. So
`schemaMappings` in the POM points `Person` and `Group` at the classes of `official-gui-api-server`.
Otherwise the generator writes them a second time, and two classes of one name end up on the class
path.

The file describes `GET /updates`, but nothing is generated for it. A generated interface cannot
return a stream of events in a servlet application. So `LoginApiController` writes the stream by
hand, and the operation carries `x-internal: true`. Without it the generator writes an
`UpdatesApi`, which maps the same request a second time, and the cockpit does not start. A user
interface reads the stream with the browser's `EventSource`, not with a generated client. The
schemas of the events are generated as usual. `UpdateStreamIsInTheOpenApiFileTest` fails when the
file no longer names the stream.

## The write concern the cockpit needs

A write concern says when MongoDB counts a write as stored. The cockpit needs one which survives a
failover of the primary. It is configured in `mongodb.write-concern`, and on every start the cockpit
logs which write concern it really uses. What each value costs an installation, and what the
warning on a start means, is in the wiki under
[The write concern](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern).
This section is about why the value has to stand there and nowhere else.

Nobody had written this down before. The MongoDB changeset mechanism set `JOURNALED` on the
application's template while it migrated, and never set it back. So the cockpit ran with that value
by accident for years. Today the migration is a library of its own, and it leaves the template as it
found it. So the cockpit now writes with the write concern it asks for.

### Where the value has to be written

On the cockpit's `MongoTemplate`, which is what `mongodb.write-concern` sets. Not in the connection
string, and not on the `MongoClient`.

The template checks the result of every write, because that is how Spring Data notices that
somebody else changed the same document first. A template which does that never reads the write
concern of its connection. `MongoTemplate.prepareWriteConcern` asks the template. If the template
has no write concern set, it writes with `ACKNOWLEDGED`, which waits for one node only. So the driver
understands a `?w=majority` in `spring.mongodb.uri`, but every write of the cockpit ignores it, and
nothing would tell you. That is why the startup check names such a value when it finds one.

The same rule drops the driver's own `WriteConcern.JOURNALED`. That value says `j: true` and leaves
`w` open. `prepareWriteConcern` replaces every write concern with an open `w` by a plain
acknowledged write, and the journal flag is lost with it. This is why `mongodb.write-concern-journal` is only read
together with `mongodb.write-concern`.

### Why `0` ends the start

Every document of the cockpit which carries `@Version` is saved by comparing the version. Spring
Data does that by reading how many documents the write changed, and an unacknowledged write answers
that question with an exception. Spring Data would quietly raise the value to one acknowledgement.
The start ends anyway, because otherwise an installation which asked for `0` would silently get
something else.

### Azure Cosmos DB for MongoDB

The cockpit knows this server through `mongodb.mode: AZURE_COSMOS_MONGO_4_2`, and the startup check
reads that mode. Cosmos DB is not a replica set. How durable a write is depends on the consistency
level of the account, and a message about failovers would talk about a server such an installation
does not have. What holds there instead is in the wiki section named above.

## A key with a dot in the business data

The business data of a user task or a workflow, `details`, is a map the workflow module fills.
MongoDB reads a dot in a path as a step into a nested document, so Spring Data refuses a key like
`order.id` unless it is told what to write instead. By default the cockpit tells it nothing. Such a
report is given up, see decision 38 in the [decision log](../DECISIONS.md). Its `422` and its error
in the log name the key and the property which changes that.

The property is `business-cockpit.mongodb.map-key-dot-replacement`. `MongoDbConfiguration` hands it to
the converter of the cockpit's `MongoTemplate`, which writes and reads every record. Spring Data then
stores each dot as the replacement and turns it back into a dot on the way out. That has two costs:

- Search and sorting see what is stored. A column or a filter has to name the key in its stored
  form, like `details.order~id` for the replacement `~`. The path with the dot finds nothing.
- The way back is not exact. A key which holds the replacement already comes back with a dot in its
  place.

The start ends where the value cannot work: an empty value, a dot, a `$` or the character NUL.
`StartupConfigurationCheck` checks it before the first bean is built.

Spring Data can also keep the dot as it is (`preserveMapKeys`). MongoDB accepts that since version
5. But every path in the cockpit is a chain of dots: in the search, the word suggestions, the
sorting with its index, and the user interface. None of them would find such a key.
`AKeyWithADotInTheDetailsTest` measures both settings.

## A suspended task or case

A BPMS can suspend a process instance. Its tasks cannot be worked on until it is activated again.
The cockpit learns about it from the reports `suspended` and `activated` of the BPMS API, see
[its README](../apis/bpms-api/README.md). `SuspensionOfARecord` holds the rules, the same for a
user task and a workflow:

- A suspension sets `suspendedAt` to the moment of the report and `suspendedBy` to the person it
  names. A record which is suspended already keeps the moment and the person of its first
  suspension. An activation removes `suspendedAt` and sets `suspendedBy` to the person who
  activated.
- The reports overtake each other, so the cockpit orders them by their timestamps. The record keeps
  `suspensionChangedAt`, the moment of the last report which changed the suspension. An older
  report changes nothing.
- Only these two reports change the suspension. A task is created and completed by a running
  process instance, so a creation, a change, a change of the aggregate and an end leave
  `suspendedAt` and `suspendedBy` as they are. A record which was cancelled while it was suspended
  keeps `suspendedAt` after its end.
- A report about a record the cockpit does not know is answered with 200 and logged. The creation
  brings the record, and the next report about the suspension marks it.
- Neither report changes `latestEventAt`, so they never make a change look old.

A suspended record stays in every list. Removing it would look like an error to a user who saw it
before. The GUI API carries `suspendedAt` and `suspendedBy`, and a list can be sorted and filtered by it like by any
other field. There is no mode of its own for suspended entries. The cockpit's own actions, like
claiming or a note, stay allowed, because they change cockpit data and do not ask the BPMS. A
missing field means "not suspended", so the records stored before need no changeset.
