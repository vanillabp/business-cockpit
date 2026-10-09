![](../readme/vanillabp-headline.png)

# Development

The local development environment of this repository, and the tools which support developing a
workflow module: the dev shell simulator and the simulator. The dev shells for React, Angular and
Vue are npm packages. They sit here until they move to repositories of their own, and Maven does
not build them.

Building a workflow module, developing its user task forms and deriving an application from the
Business Cockpit are described in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki), under
[Connecting a workflow module](https://github.com/vanillabp/business-cockpit/wiki/Connecting-a-workflow-module),
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites)
and
[Developing UI components locally](https://github.com/vanillabp/business-cockpit/wiki/Developing-UI-components-locally).
This file is about working on the cockpit itself.

**Contents:**

1. [MongoDB](#mongodb)
1. [Local NPM registry](#local-npm-registry)
1. [Notification e-mails (Mailpit)](#notification-e-mails-mailpit)
1. [Kafka](#kafka)
1. [Build and Run the Business Cockpit](#build-and-run-the-business-cockpit)
1. [Simulation Service](#simulation-service)

The Business Cockpit is a Java Spring Boot application. Its React user interface is no longer built
by Maven, see decision 62 in [DECISIONS.md](../DECISIONS.md). One thing has to be running before it
starts: a [MongoDB cluster](#mongodb). The provided compose configuration also brings two
services which are not needed to run the cockpit but to develop and test particular features,
[Mailpit](#notification-e-mails-mailpit) for notification e-mails and a
[single-node Kafka](#kafka) for reporting BPMS events over Kafka rather than over REST. The latter is
only needed with a real business service, since the simulation service brings its own broker.

```sh
cd development
docker-compose up -d
```


## MongoDB

The cockpit reads MongoDB's change stream, which MongoDB offers on a replica set only, so the local
one is a single-node replica set as well. `docker-compose up -d` starts it.

A database tool connects with these:

| | |
|---|---|
| Host | `business-cockpit-mongo`, port 27017 |
| Replica set | `rs-business-cockpit` |
| User and password | `business-cockpit` / `business-cockpit`, authenticating against `business-cockpit` |
| Database | `business-cockpit` |

The replica set names its member `business-cockpit-mongo`, and a tool follows that name, so it has
to resolve. Add it to `/etc/hosts`, or to `C:\Windows\System32\drivers\etc\hosts` on Windows:

```
127.0.0.1       business-cockpit-mongo
```

The container itself does not need that entry: the profile `local` connects to `localhost` with
`?directConnection=true`, which skips the member discovery the name comes from.

## Local NPM registry

The Maven build does not need this registry any more. It is for somebody who works on the npm
packages of this repository by hand, until they move to repositories of their own. The packages
depend on each other, so they are published to a local registry first and read from there. The
registry is [Verdaccio](https://www.verdaccio.org/), which is part of the provided
`docker-compose.yaml`.

To use this registry one has to create a file `.npmrc` in your home folder:

```
@vanillabp:registry=http://localhost:4873
//localhost:4873/:_authToken="fake"
```

*Hint:* Verdaccio is preconfigured to accept unauthorized attempts of publishing NPM packages. Therefore, as you can see, the content of the authentication token `fake` is not taken into account - it just has to be filled.

To connect to the registry UI use these parameters:

* *URL:* [http://localhost:4873/](http://localhost:4873/)
* *username:* admin
* *password:* admin

## Notification e-mails (Mailpit)

Notification e-mails are not sent to a real mail server during development but to
[Mailpit](https://mailpit.axllent.org/), which is part of the provided `docker-compose.yaml`. It
accepts every message and keeps it in memory:

* *SMTP:* localhost:1025
* *Web UI:* [http://localhost:8025/](http://localhost:8025/)
* *REST API:* [http://localhost:8025/api/v1](http://localhost:8025/api/v1)

The e-mail medium of the business cockpit is switched off by default. To activate it and point it
at Mailpit add the Spring profile `mailpit`:

```sh
java -Dspring.profiles.active=local,mailpit -jar target/business-cockpit-*-runnable.jar
```

The profile also shortens the notification interval to 10 seconds, so testing does not mean waiting
a minute for every attempt.

Notifications are switched off per user (the default is "none"). So, to receive an e-mail:

1. Log in to the business cockpit. The user record, including the e-mail address reported by the
   user directory, is stored on login - a user who never logged in is never notified.
1. Open the notification settings of that user and switch on e-mail, either globally or for the
   workflow you are about to test.
1. Report a user task, e.g. by the [simulation service](#simulation-service).

Check the result using the Mailpit REST API:

```sh
# subject and recipients of everything received so far
curl -s http://localhost:8025/api/v1/messages | jq '.messages[] | {To, Subject}'
# the newest message including its rendered text body
curl -s http://localhost:8025/api/v1/message/latest | jq '{To, Subject, Text}'
# only the messages addressed to one user
curl -s 'http://localhost:8025/api/v1/search?query=to%3Ajohn%40doe.com' | jq '.messages_count'
# start over
curl -s -X DELETE http://localhost:8025/api/v1/messages
```

## Kafka

Instead of reporting BPMS events by the REST API a business service may report them by Kafka. For
that a single-node broker (KRaft mode, so no ZooKeeper involved) is part of the provided
`docker-compose.yaml`, listening at `localhost:9092`. Topics are created on demand, so there is
nothing to set up.

It is needed for business services which report by Kafka. The
[simulation service](#simulation-service) does not need it: it starts an embedded broker at the same
address, unless [switched off](#kafka).

The business cockpit consumes those events using the Spring profile `kafka`, which sets the
bootstrap servers as well as the topic names `workflows`, `user-tasks` and `modules`:

```sh
java -DworkerId=local -Dspring.profiles.active=local,kafka,mailpit -jar target/business-cockpit-*-runnable.jar
```

*Hint:* `workerId` is mandatory for consuming Kafka events
(see https://github.com/vanillabp/spring-boot-support#worker-id).

The reporting side has to use the same broker. The [simulation service](#simulation-service) brings
its own embedded broker, which is started by default and listens at the very same address - so for
reporting by Kafka nothing has to be started at all:

```sh
cd development/simulator
java --add-opens=java.base/java.lang=ALL-UNNAMED \
    -Dspring.profiles.active=kafka-sync \
    -jar target/simulator-*-runnable.jar
```

*Hint:* Besides `localhost:9092` the embedded broker binds port 9093 (reachable by other machines,
see `server.address`) and 9094 (its own KRaft controller, of no use to clients).

Whoever wants to use the "real" broker of the docker-compose instead switches the embedded one off.
Otherwise both would compete for port 9092 and the simulation service won't start:

```sh
java --add-opens=java.base/java.lang=ALL-UNNAMED \
    -Dspring.profiles.active=kafka-sync \
    -Dkafka.embedded=false \
    -jar target/simulator-*-runnable.jar
```

Naming a broker explicitly by `-Dspring.kafka.bootstrap-servers=<host>:<port>` switches the embedded
one off as well - useful for a broker which is not at `localhost:9092`.

## Build and Run the Business Cockpit

The business cockpit is developed with Java 21 and Spring Boot 4 (Spring MVC on virtual threads).
Build it with Maven, from the root of the repository:

```sh
mvn install
```

After the build succeeded the service can be started:

```sh
cd container
java -Dspring.profiles.active=local -jar target/container-*-runnable.jar
```

The runnable jar is built from `container`, the business cockpit as an application. The module next
to it, `business-cockpit`, is the same functionality as a library, and it is what a custom cockpit
application depends on. It produces no runnable jar. Backend changes usually land in
`business-cockpit`.

Neither jar carries a user interface. The service answers its APIs at
[http://localhost:8080/](http://localhost:8080/), and a path no API knows gets 404. The login for a
local run is user `test` with password `test`.

To build again after a change, build the modules you touched and what they depend on:

```sh
mvn -pl business-cockpit,container -am install
```

## Simulation Service

The simulator is a standalone Spring Boot application which stands in for a business service: it
reports user tasks and workflows to the cockpit the way a workflow module does, so cockpit features
can be developed without a BPMS and without a real application.

```sh
cd development/simulator
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar target/simulator-*-runnable.jar
```

Its [test data form](http://localhost:8079/testdata/usertask/form) generates user tasks, ten per
press of `Generate`, and they appear in the cockpit as they are generated. The tasks it makes belong
to no process instance, which is what makes them cheap to make.

Its user interface, `development/simulator/src/main/webapp-react`, is not built by Maven any more,
so the runnable jar does not serve it. The sources are still the worked example a workflow
module's user interface is copied from. What to do with it is in the wiki, under
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites).
