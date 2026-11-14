# Upgrading

What an application built on the Business Cockpit has to change when it moves to a new version of
the cockpit. Each section names one version and lists only what breaks or changes. Everything the
section does not name keeps working.

The wiki page [Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases) tells the
whole story of a version. This file is the short list of what somebody has to touch.

## 0.8.x to 0.9.0

### `commons` is gone: `support` and the library module take its classes

The artifact `io.vanillabp.businesscockpit:commons` no longer exists. There is no artifact under the
old name which points at the new ones. What a workflow module can use as well went into the new
artifact `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`, called `support` below. What only the cockpit uses went into the library
module `io.vanillabp.businesscockpit:business-cockpit`.

A cockpit application depends on `business-cockpit`, which brings `support` along. It changes its
imports and nothing in its POM. A workflow module or another service which depended on `commons`
depends on `support` instead. `support` brings no MongoDB and no Kafka.

`support` brings the cockpit's SPI, `io.vanillabp.businesscockpit:spi-for-java`, and through it the
SPI of VanillaBP. A workflow module which names the SPI itself may keep it or remove it. With
`support` as its only dependency, a workflow module compiles. To run, it still needs the cockpit's
adapter for its BPMS, `business-cockpit-*-adapter`. A workflow module which is a library of its own
takes the adapter in test scope, and the application which runs it brings the adapter.

One line per package. Replace the beginning of each import:

|                                      before                                      |                                   now                                    |      artifact      |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------------|--------------------|
| `io.vanillabp.cockpit.commons.security.jwt`                                      | `io.vanillabp.cockpit.support.security.jwt`                              | `support`          |
| `io.vanillabp.cockpit.commons.security.usercontext.WorkflowModuleGroupHierarchy` | `io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy`  | `spi-for-java`     |
| `io.vanillabp.cockpit.commons.security.usercontext`                              | `io.vanillabp.cockpit.support.security.usercontext`                      | `support`          |
| `io.vanillabp.cockpit.commons.rest`                                              | `io.vanillabp.cockpit.support.rest`                                      | `support`          |
| `io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException`                | `io.vanillabp.cockpit.support.security.BcUnauthorizedException`          | `support`          |
| `io.vanillabp.cockpit.users.UserDetailsProvider`                                 | `io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService` | `support`          |
| `io.vanillabp.cockpit.users.UserDetailsImpl`                                     | `io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl`      | `support`          |
| `io.vanillabp.cockpit.commons.exceptions`                                        | `io.vanillabp.cockpit.exceptions`                                        | `business-cockpit` |
| `io.vanillabp.cockpit.commons.kafka`                                             | `io.vanillabp.cockpit.kafka`                                             | `business-cockpit` |
| `io.vanillabp.cockpit.commons.mapstruct`                                         | `io.vanillabp.cockpit.mapstruct`                                         | `business-cockpit` |
| `io.vanillabp.cockpit.commons.mongo`                                             | `io.vanillabp.cockpit.mongo`                                             | `business-cockpit` |
| `io.vanillabp.cockpit.commons.utils.DateTimeUtil`                                | `io.vanillabp.cockpit.mongo.converters.DateTimeUtil`                     | `business-cockpit` |
| `io.vanillabp.cockpit.commons.utils.LoggerFactory`                               | gone, see below                                                          |                    |

`WorkflowModuleGroupHierarchy` went to the cockpit's SPI, which `support` brings, because the
adapter reads it. The section
[`group-hierarchy-bean-name` becomes a bean](#group-hierarchy-bean-name-becomes-a-bean) says
what else changed about it.

`BcUnauthorizedException` left its siblings, because `UserContext` in `support` throws it and
`support` cannot see the library module.

`UserDetailsImpl` moved to `support` as well. It is the plain implementation of `UserDetails`,
which the cockpit and a workflow module both need. Before, a workflow module had to write its own
or depend on the library module.

The directory of the users got a new name on its way to `support`, so it is no longer confused with
the `UserDetailsProvider` which says who is calling. Its methods stayed. A controller which extends
`AbstractUserTaskListGuiApiController` finds it in the field `userDirectoryService` instead of
`userDetailsProvider`.

The two auto-configurations of the MongoDB package have new names as well. An application which
names one of them in `spring.autoconfigure.exclude` replaces the name, for the same reason as the
[changeset auto-configuration](#read-your-springautoconfigureexclude):

|                                    before                                    |                                 now                                  |
|------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.BusinessCockpitMongoDbAutoConfiguration` | `io.vanillabp.cockpit.mongo.BusinessCockpitMongoDbAutoConfiguration` |
| `io.vanillabp.cockpit.commons.mongo.ChangesetPropertiesAutoConfiguration`    | `io.vanillabp.cockpit.mongo.ChangesetPropertiesAutoConfiguration`    |

They now come with `business-cockpit` only. A workflow module which had `commons` on its class path
got both of them, and with them the MongoDB settings of the cockpit. With `support` it gets neither.

### The snapshot artifact `support` is now `business-cockpit-spring-boot-support`

This concerns only a build which used a 0.9.0 snapshot. The module `support` got a level below it,
because a Quarkus version of it will follow. Its classes moved into the sub-module
`support/spring-boot`, which is published as
`io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. The artifact
`io.vanillabp.businesscockpit:support` is gone, and no artifact under the old name points at the new
one. `business-cockpit-bom` manages the new artifact. Replace the `artifactId` in the POM. The
packages stay `io.vanillabp.cockpit.support...`, so no import changes.

The module now also has `BcUnknownTaskException` and `BcTaskNotOpenException` in
`io.vanillabp.cockpit.support.usertask`, and `BcUserTaskExceptionHandler`, which answers them with
`404 Not Found` and `409 Conflict`. A workflow module which wrote its own exceptions for a task id
it does not know, or for a task which is no longer open, can use these. Nothing registers the
handler on its own: import it with `@Import(BcUserTaskExceptionHandler.class)`.

### The cockpit no longer injects a `Logger`

`commons` had a bean which handed an SLF4J `Logger` to every field or parameter of that type, and
the cockpit's auto-configuration imported it. The bean is gone. The cockpit's own classes use
Lombok's `@Slf4j` now. A class of the application which has `@Autowired Logger logger` fails at
the start with a missing bean of type `org.slf4j.Logger`. Write
`private static final Logger logger = LoggerFactory.getLogger(MyClass.class);` or use `@Slf4j`.

### The REST client runs on OpenFeign 13.18 and OkHttp 5

The REST client in `support` moved from OpenFeign 13.14 to 13.18. That version of `feign-okhttp`
is built against OkHttp 5, whose classes come in the artifact `com.squareup.okhttp3:okhttp-jvm`.
The package is still `okhttp3`. An application which names OkHttp 4 itself, `okhttp` or
`logging-interceptor` in a version `4.x`, ends up with both versions on its class path. Remove the
version, or import `com.squareup.okhttp3:okhttp-bom` in version `5.5.1`.

`feign-form` is part of OpenFeign now. Its coordinate changed from `io.github.openfeign.form:feign-form`
to `io.github.openfeign:feign-form`, and the package `feign.form` stayed.

The REST client fetches its OAuth token without Apache Oltu, and `support` no longer brings
`org.apache.oltu.oauth2:org.apache.oltu.oauth2.client`. An application which uses Oltu itself names
it in its own POM. The classes `OAuthClient` and `OAuthOkHttpClient` in the package
`io.vanillabp.cockpit.support.rest.adapter.oauth` are gone, because their methods took Oltu types.
`OauthBearerTokenHandler` stays. With `authentication.oauth.basic: true`, the token request sends
`grant_type` in the form body now, and no longer in the query string, as RFC 6749 asks. A missing
`authentication.oauth.client-id` or `client-secret` is reported when the REST client is built,
which is usually at the start of the application, and no longer at the first request.

### The MongoDB migration is a library of its own

The cockpit no longer carries the MongoDB changeset mechanism. It uses
`com.phactum.mongodb:mongodb-changesets`, which does the same thing and is published on its own.
The `commons` module brings the dependency along, so an application which depends on the cockpit
gets it without naming it.

Nothing on disk changes. The collection is still called `ChangesetInformation`, a step is still
identified by the class name of its changeset bean plus the name of its method, and the cockpit's
own changeset beans stayed where they were. A database migrated by an earlier version runs no step
a second time.

### The write concern is yours now

The changeset mechanism used to set `WriteConcern.JOURNALED` on the application's `MongoTemplate`
while it migrated and never took it back. Every application built on the cockpit ran with that
value without asking for it. Now the library leaves the template as it found it, so the write
concern is the one the application configures.

The delivered `container` configures it. A custom cockpit adds:

```yaml
mongodb:
  write-concern: majority
  write-concern-journal: true
```

Without it the cockpit writes with one acknowledging node and warns about that on every start. A
write which only the primary has is lost when that primary steps down, and the user task the
cockpit has already confirmed to the BPMS adapter never appears.

Write the value here and not into `spring.mongodb.uri`. The cockpit's `MongoTemplate` checks the
result of every write, and Spring Data replaces the write concern of the connection with a plain
acknowledged write while it does that. `mongodb.write-concern: 0` ends the start.
[The write concern](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern)
in the wiki has the list of what each value costs.

### A table of the version 1 Camunda 8 adapter stays behind

The version 1 adapters left this repository with 0.9.0. One of them leaves something in the
database: the Camunda 8 adapter created a table `CAMUNDA8_BC_PROCESS_INSTANCES` through its
Liquibase changelog, and nothing ever wrote a row into it. A changelog which drops the table was
written back then and never switched on, so the table is still there and no version of the cockpit
removes it.

An application which ran the version 1 Camunda 8 adapter can drop the table by hand. Nothing reads
it, nothing writes it, and dropping it costs no data.

### Read your `spring.autoconfigure.exclude`

The auto-configuration has a new fully qualified name.

|                                   before                                   |                             now                             |
|----------------------------------------------------------------------------|-------------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetAutoConfiguration` | `com.phactum.mongodb.changesets.ChangesetAutoConfiguration` |

An application which switched the migration off by the old name switches nothing off any more, and
it starts anyway. Spring Boot refuses an exclude entry only while the class it names is on the
class path. The old class is gone, so the entry is read and ignored, and the migration runs in an
application which had it switched off. Replace the name.

### A changeset bean of your own changes two imports

The annotations are called `DbChangeset` and `DbChangesetConfiguration`. They were called
`Changeset` and `ChangesetConfiguration` until 0.9.0, and they sit in another package now.

|                                 before                                 |                            now                            |
|------------------------------------------------------------------------|-----------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.changesets.Changeset`              | `com.phactum.mongodb.changesets.DbChangeset`              |
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration` | `com.phactum.mongodb.changesets.DbChangesetConfiguration` |

Nothing else about such a bean changes. It is still a Spring bean, its steps are still public
methods which take a `MongoTemplate` or nothing, and they still answer with the command which
undoes what they did.

### A bean which waits for the migration asks for another type

A bean used to take a parameter of the auto-configuration to say "build me after the migration".
Now the parameter has to be `com.phactum.mongodb.changesets.ChangesetApplier`. The
auto-configuration only does the wiring, and a configuration class is built before the beans it
declares. So a parameter of the auto-configuration's type compiles and starts, but it guarantees
nothing.

### `ChangesetInformation.timestamp` is an `Instant`

It was an `OffsetDateTime`. The document is the same either way, a BSON date, so there is no data
migration. Only code which reads the field in Java is affected.

### The workflow provider API is gone

The artifacts `io.vanillabp.businesscockpit:workflow-provider-api-client` and
`io.vanillabp.businesscockpit:workflow-provider-api-server` held no code, and the cockpit never
called the API. Both artifacts and the OpenAPI file are gone. Remove the artifacts from your POM if
you named them. A workflow module may still send `workflowProviderApiUriPath` when it registers;
the cockpit stores it and does nothing with it.

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

### Every report names an initiator, and one new key says who answers it

The initiator is the user who caused what is reported: who started a case, who took a task. In
version 1 the field was optional, and most applications left it empty. Now every report carries a
value, and a new key says where the value comes from:

```yaml
vanillabp:
  cockpit:
    initiator-source: by-application
```

The key has no default, so write it before you start the application. `by-application` means your
code sets the initiator in a method annotated with `@WorkflowDetailsProvider` or
`@UserTaskDetailsProvider`. `system` means this workflow module knows no action a user causes, and
every report of it then names `system`. A workflow module may say something of its own at
`vanillabp.workflow-modules.<moduleId>.cockpit.initiator-source`, and a single workflow at
`vanillabp.workflow-modules.<moduleId>.workflows.<process>.cockpit.initiator-source`.

With `by-application` a report which names nobody after your provider ran fails, and it takes the
engine's work with it, because the report is built in the transaction of the event. The constant
`io.vanillabp.spi.cockpit.Initiator.SYSTEM` is the way to say that no user caused this one event. It
is also how you drop a value Camunda 7 prefilled: calling `setInitiator(null)` looks exactly like a
provider which did nothing.

Your existing cases keep what they have. Nothing is filled in afterwards, because nobody can find
out later who started a case which ran without an initiator. That is also why the key has no
default: a value which cannot be filled in later is worth one line of configuration.

### A workflow module keeps its templates in its own directory

A workflow module now finds its title templates in `<workflow-module-id>/business-cockpit-templates`
of the classpath, with nothing configured. All of them go there, also `workflow-title.ftl`,
`workflow-fulltext-search.ftl` and a `task-fulltext-search.ftl` which serves every task. A module
which keeps them somewhere else names the directory in its own
`vanillabp.workflow-modules.<moduleId>.cockpit.template-loader-path`.

`vanillabp.cockpit.template-loader-path` keeps working for every module which names no directory of
its own. While it is set, the default directory is not looked at, so nothing changes for an
application which uses it. To move a module's templates into the default directory, remove the key
from the application, or give the other modules a `template-loader-path` of their own.

One thing may change without a key: a module with Freemarker on its classpath, without the key of
the application, and with a directory `<moduleId>/business-cockpit-templates` which held something
else. Its templates are rendered now. Before, the cockpit showed the BPMN names.

### `uiUriType` is a string, and the address is the user interface's business

`uiUriType` was an enum with two values in the published schemas. It is a plain string now, in
`bpms-api` v1 and v1_1 and in `official-gui-api` v1. `EXTERNAL` and `WEBPACK_MF_REACT` are still
valid strings, so a workflow module which reports one of them needs no change.

The field stays required. For a module whose user interface is federated anyway, leaving it out
looks tempting. But then the database fills up with entries that say nothing. On the day a second
value shows up, nobody could tell what an empty one was meant to say. One line of configuration
answers that question once.

The start of an application still checks `ui-uri-type`, and it only checks that a value is there.
Any string passes. The message where nothing is configured names no values. It sends you to the
cockpit user interface instead.

The names are a convention between a workflow module and the user interface which loads it.
VanillaBP passes the value on and never reads it, so it has no list of names and no class with
constants for them. `WEBPACK_MF_REACT` was the value of the React user interface which came with
the cockpit up to 0.8. It is still accepted, like any other string. Which names a user interface
understands is written down with that user interface. If your user interface comes with a small
jar of constants, your workflow modules can use that.

What changes for a user interface is `uiUri` in the GUI API. Until 0.9.0 the cockpit built the whole
address: `/wm/<workflowModuleId>` plus the path the workflow module reported. Only for the type
`EXTERNAL` it handed out the reported value unchanged. Now the reported path is handed out in every
case, and the user interface builds the address. A cockpit user interface follows one rule. A path
which starts with `http://` or `https://` is an address already and is opened as it is. Every other
path gets `/wm/<workflowModuleId>/` in front of it. `workflowModuleId` and `workflowModuleUri` are in
the same answer, so nothing has to be looked up for it.

So the type no longer decides whether a path is an address. A workflow module whose pages live in
another application reports the whole address in `ui-uri-path`, or per case through `setUiUriPath`
of `PrefilledUserTaskDetails` and `PrefilledWorkflowDetails`. It is opened without the route in
front, whatever `ui-uri-type` says.

If you built your own user interface against the old answer, there are two ways out.

The first is to do it in the browser. Your code looks at the text it got. Where it starts with
neither `/wm/` nor `http://` or `https://`, it adds the proxy route of the module. That keeps
working against both versions of the cockpit, which matters while the two are rolled out one after
the other.

The second is to change what is stored, so the answer looks like it used to. Report the whole
address as `ui-uri-path`, `/wm/taxi-ride/remoteEntry.js` rather than `/remoteEntry.js`, and update
the entries which are already there in one block. This way only fits a user interface of your own
which does not follow the rule above. One which follows it would put the route in front a second
time. Three things are worth knowing before you do that.

The field exists twice. A user task has one and a workflow has one, so two MongoDB collections carry
it, `UserTask` and `Workflow`.

The database holds the reported path, never the computed address. Only `GuiApiMapper` built the
address, at the moment it answered, so nothing in the database has to be taken apart.

Entries whose path starts with `http://` or `https://` must be left alone. Their path is the whole
address already, the cockpit never prefixed it, and prefixing it now would send the browser to a
route of the cockpit which does not exist. So an update in one block has to leave them out. Up to
0.8 the type `EXTERNAL` said the same thing. If an entry of that type has a path without `http://`
or `https://`, give it the whole address in the same block.

### A report no longer changes who sees a user task

The report which creates a user task in the cockpit sets its assignee, its candidate users, its
candidate groups and its excluded candidate users. Later reports, an update or an end, do not change
them any more. Before, an update replaced the candidate groups and the excluded users, and an update
which named an assignee replaced the assignee. Only taking a task over and assigning it in the
cockpit still change the assignee and the candidate users. `admittedUsers` is still read from every
report. A task which names nobody is shown to everybody, and it takes the names from the first later
report which has some.

A workflow module which handed a task to other people by reporting it again has to end the task and
enter it again, for instance with a boundary event. The new task gets the new values. Nothing else
needs to change: the details provider may keep setting the values for every report.

### The cockpit jar carries no user interface

`io.vanillabp.businesscockpit:business-cockpit` and the runnable jar of `container` no longer
contain the React user interface. The repository holds no user interface any more, and no npm
package of the cockpit is released with 0.9.0. If your application depends on `business-cockpit`,
it serves no user interface, and a path no API knows gets 404 instead of the single-page
application.

If your application brings a user interface of its own, name its `index.html` with
`application.spa-default-file`, for example `classpath:/static/index.html`, and unknown paths get it
as before. The library has no value for the key any more, see "Two values are the application's" below. Decisions 62 and 63 in [DECISIONS.md](./DECISIONS.md) have the details.

### `OpenTasks` is gone

The GUI API knew two names for the same list of user tasks: `OpenTasks` and
`OpenTasksWithFollowUp`. `OpenTasks` is gone. A request which still sends it gets 400. Send
`OpenTasksWithFollowUp` instead. The list is the same: every task which has not ended, with or
without a follow-up date. Its partner is `OpenTasksWithoutFollowUp`, which leaves out the tasks with
a follow-up date in the future.

A request without `mode` now gets the same list for the first page and for the update:

- `POST /usertask` without `mode` shows no ended tasks any more. Before, it showed all tasks.
- `POST /workflow` without `mode` shows no ended workflows any more. Before, it showed all workflows.
- `POST /workflow/{workflowId}/usertasks` reads the whole `mode` now, like `POST /usertask`. Before,
  it only looked for `OpenTasks` and answered every other mode with all tasks of the workflow,
  `ClosedTasksOnly` included. Without `mode` it shows the open tasks of the workflow.

If you need the ended ones, send `All`.

A subclass of `AbstractWorkflowListGuiApiController` which overrides `getUserTasksOfWorkflow` gets a
mode instead of the `boolean` which said whether to leave out the ended tasks. The same goes for a
caller of `UserTaskService.getUserTasksOfWorkflow`.

The dev-shell simulator still shows all tasks and all workflows without `mode`. It only fills the
list a developer picks a task from, and it holds a few tasks only.

### An ended user task refuses claim and assign

Claiming a user task, giving it back, assigning it and unassigning it now answer 409 for a task which
has ended. Before, they answered 200 and changed the task. The list forms do the same: if one of the
tasks the user may see has ended, the request changes none of them and answers 409. A user interface
which relied on 200 has to switch these actions off for a task whose `endedAt` is set.

Marking as read and as unread still work for an ended task, and so does `GET /usertask/{id}` with
`markAsRead=true`. A read marker only changes how the list shows the row.

The list forms never answered 404, although the OpenAPI file said so. The file no longer says it.

### Two count endpoints

`POST /usertask/count` and `POST /workflow/count` are new. Each takes a list of filters, the same
filters as its list, and answers one count per filter, with no rows and without sorting. Nothing
else changes. A user interface which counted with a page of size 1 can switch when it likes. Two new
changesets add indexes on the fields which decide who sees a user task or a workflow. On a large
database the first start after the upgrade takes a little longer while MongoDB builds them.

### Signing in is the application's

The library `business-cockpit` brings no login any more. The two security chains `guiHttpSecurity`
and `bpmsApiHttpSecurity`, the users with the password `test` and the bean `userDetailsProvider`
moved into the module `container`. The runnable jar of `container` behaves as before.

If your application depends on `business-cockpit` and relied on what the library brought, write
these three beans yourself. The classes `GuiSecurityConfiguration` and
`BpmsApiSecurityConfiguration` in `container` are the template, and the wiki page
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) says what every chain has to
keep.

- Without `userDetailsProvider` the application does not start. The failure report shows the bean
  to write.
- Without any chain of your own, Spring Boot adds its default chain. It asks for a login on every
  path, with a password it generates and prints. Your users and the BPMS adapters cannot log in.
- With some chains of your own but not these two, a request which none of your chains takes is not
  checked at all.

On every start the log names each of the two chains your application does not have. The keys
`bpms-api.realm-name`, `bpms-api.username` and `bpms-api.password` stay. The library no longer
builds a chain from them, the chain of `container` does.

The constants `BPMS_API_ROLE` and `BPMS_API_AUTHORITY` moved from
`io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration` to
`io.vanillabp.cockpit.bpms.BpmsApiAuthority`. The BPMS API still answers only a caller with the
authority `ROLE_BPMS-API`.

The users of the dev-shell simulator log in with `test` now, as they do at the cockpit. Before, it
was `Secure_123`.

### Two values are the application's

The library has no value for `application.spa-default-file` and `business-cockpit.default-locale`
any more. `container` sets both in its `application.yaml`.

- `application.spa-default-file` was `classpath:/static/index.html`. Without it, every path no API
  knows gets 404, and the log says so on every start. If your application has a user interface,
  write the key.
- `business-cockpit.default-locale` was German. `container` sets English now, so the delivered
  application writes a mail in English to a user who has no language of their own. Without the
  key, such a mail uses the template without a language in its name, and the log warns while
  e-mail notifications are switched on. If your application relied on German, write
  `business-cockpit.default-locale: de`.

### The update stream is in the OpenAPI file

`GET /gui/api/v1/updates` is now described in the cockpit's own OpenAPI file,
`business-cockpit/src/main/resources/api/gui/v1.yaml`, with its events and their data. The stream
itself does not change.

Two classes moved to another jar, in the same package `io.vanillabp.cockpit.gui.api.v1`.
`UserTaskEvent` and `WorkflowEvent` are no longer in `official-gui-api-server`. They are in
`business-cockpit` now, and their `type` is an enum instead of a string. A client generated from the
official file alone no longer has them. Generate it from both files. `Person` and `Group` used to
be in both jars. Now they are only in `official-gui-api-server`. `UserAllOf` is gone, nothing used
it. The generator of the cockpit's file is now 7.24.0, the same as for the official file.

### A task and a case say why they ended

`UserTask` and `Workflow` of the GUI API carry `endReason`: `COMPLETED` or `CANCELLED`, and nothing
while they are open. The cockpit sets it from the report of the end. Two changesets give the tasks
and cases which ended before a reason, on the first start: `CANCELLED` where `comment` has text,
`COMPLETED` otherwise. That is the rule the old releases left in the data, and it is not exact. A
cancellation without a reason becomes `COMPLETED`, and a completion with a comment becomes
`CANCELLED`. If you need it otherwise, correct `endReason` in the collections `usertask` and
`workflow` by hand after the start. A list can also be filtered and sorted by `endReason`.

### A note on a task and on a case

`UserTask` and `Workflow` of the GUI API carry `note`, a text the users keep about the entry. It is
set with `PATCH /usertask/{userTaskId}/note` and `PATCH /workflow/{workflowId}/note`, and for
several entries at once with `PATCH /usertask/note` and `PATCH /workflow/note`. Nothing else
changes. The full-text filter of the lists now finds an entry by its note as well.

### The settings of a user

`GET`, `PUT` and `DELETE /gui/api/v1/app/current-user/settings/{key}` are new. They keep one setting
of the user signed in, in the new field `customSettings` of the collection `users`. Nothing breaks.
A chain of your own needs nothing new, as long as it keeps `currentUserRequestMatcher` as it is and
does not open `/gui/api/v1/app/current-user/**`.

### The list of workflows says `InProgress` and `Done`

`WorkflowRetrieveMode` has two new names: `InProgress` for the workflows which have not ended, and
`Done` for those which have. A suspended workflow has not ended, so it is in `InProgress`. A request
without `mode` gets `InProgress`, the same list as before.

`Active` and `Inactive` still work and give the same lists as `InProgress` and `Done`. They are
deprecated and go with the next major version at the earliest. Since there is "suspend" and
"activate", `Active` reads like "not suspended", and that is not what it shows. So move a user
interface to the new names when you touch it.

A Java caller has to change. `WorkflowlistService.RetrieveItemsMode` knows only `All`, `InProgress`
and `Done` now. A subclass of `AbstractWorkflowListGuiApiController` which overrides `getWorkflows`,
and a caller of `WorkflowlistService`, use `InProgress` instead of `Active` and `Done` instead of
`Inactive`. The generated `WorkflowRetrieveMode` of the official GUI API has all five values, so code
which reads it gets two more cases in a `switch`.

### `avatar` is gone from `Person` and `User`

`Person` and `User` of the GUI API no longer have the number `avatar`. Nothing said what it meant,
and the cockpit only passed on what your `PersonAndGroupApiMapper` returned. If your mapper set
`avatar`, remove that line, because the generated `Person` has no `avatar(...)` any more. If your
user interface shows a picture of a person, load it from an endpoint of your application, and show
the initials when there is none.

### The groups of a user come from the directory, not from the login token

The login token names the user and the time of the login, but no groups any more. On every request
the cockpit asks your bean of type `UserDirectoryService` for the user, by the subject of the token,
and takes the groups from `getAuthorities()` of the answer. So a group you take away in the
directory is gone on the next request. Before, it stayed until the login ended, which by default is
seven days.

`getUser` now runs on every request of a logged-in user. If your directory asks a remote system,
like an identity provider or LDAP, let it cache the users for a few minutes. If `getUser` throws, the
request fails with an error in the log. A user the directory does not know keeps the login, but has
no groups.

A token written by 0.8.x still names the groups in the claim `authorities`. It stays valid, so
nobody is logged out by the update. The claim is not read, and a renewal drops it.

The subject of a new token is `getName()` of the authentication of the login. For a login by user
name and password that is the user name, as before. For a login by OpenID Connect it is the claim
Spring Security names the user by, `sub` unless you configure `user-name-attribute`. Before, a
login by OpenID Connect got no token at all. The directory has to know the user by this id.

What to change:

- If you use the library's `jwtMapper` bean, nothing. It takes your `UserDirectoryService`.
- If you build a `JwtAuthenticationTokenMapper` yourself, in the cockpit or in a workflow module,
  pass a `UserDirectoryService`:

  ```java
  new JwtAuthenticationTokenMapper(properties, userDirectoryService, groupHierarchyService::resolveGroups)
  ```

  The constructors without a directory are gone. Pass `Function.identity()` if you have no group
  hierarchy. A workflow module which reads the cockpit's token needs a `UserDirectoryService` of
  its own for this.

- `new JwtUserDetailsProvider()` stays as it is. Name, short name and e-mail address now come from
  the directory and, where it has none, from the claims of the token.

- A `GrantedAuthoritiesMapper` which copies the groups of an OpenID Connect login into the
  authentication is no longer needed. Those groups do not reach the token.

### `group-hierarchy-bean-name` becomes a bean

The adapter no longer reads `vanillabp.workflow-modules.<id>.cockpit.group-hierarchy-bean-name`,
and the start ends if the key is set. Write a fixed hierarchy below
`vanillabp.workflow-modules.<id>.cockpit.group-hierarchy`.

If the hierarchy changes while the application runs, keep the bean instead, and remove the key:

- Import `io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy`.
- Add the method `getWorkflowModuleId()`. It returns the id of the module the bean answers for.
  The adapter finds the bean by its type and this id, and no longer by its name.
- After a change, publish a `WorkflowModuleGroupHierarchyChangedEvent` with the id of the module.
  On Spring Boot use the `ApplicationEventPublisher`. The adapter then registers the module at the
  cockpit again, with the new hierarchy.
- If your module resolves the groups of a request with this hierarchy, ask the bean on every
  request. The adapter keeps no copy, so there is no cache to clear.

A module with the bean and the key `group-hierarchy` does not start, and neither does a bean for a
module which reports nothing to the cockpit.

### The search takes the text literally

A search in the list of user tasks or of workflows finds the text the user typed, and the
suggestions of a search field do the same. Before, the cockpit sent the text to MongoDB as a regular
expression. Now characters like `.`, `(`, `+`, `*` and `\` find themselves. So `Mei.r` no longer
finds `Meier`, and `4.7` no longer finds `437`. A pattern like `(a+)+$` is searched for as text and
can no longer keep the database busy. Whether the search ignores case stays as it was.

If your user interface or a caller of `UserTaskService` or `WorkflowlistService` sent a pattern,
it gets no result for it any more. Send the plain text instead.
`SearchQuery.query()` is plain text now. If code of your own builds a regular expression from what
a user typed, call `SearchCriteriaHelper.literally(...)` the same way.

### The suggestions of a search field are the most frequent words

`OPTIONS /usertask` and `OPTIONS /workflow` answer with the 21 words found most often, the most
frequent first. Words found equally often come in alphabetical order. Before, the answer held any
21 words, and the rarest came first.

If your user interface sorted the suggestions itself or showed them from the end of the list, show
them in the order they come instead.

### A user directory for Keycloak and for LDAP comes with the cockpit

If your directory reads the users of a Keycloak realm or of an LDAP directory, you can drop it and
let the cockpit bring one. Set `business-cockpit.user-directory.type` to `keycloak` or `ldap`. The
next start names every value the directory still needs, with an example. The directory keeps each
user with its groups for 14 hours, `business-cockpit.user-directory.cache-duration`, and forgets
every user each night at 2 a.m., `business-cockpit.user-directory.cache-clear-cron`. So a group given
or taken away counts the next day at the latest. The actuator endpoint `userdirectorycache` empties
the cache at once, if the application has Spring Boot Actuator and exposes the endpoint.

- The LDAP directory needs `org.springframework.ldap:spring-ldap-core`. The cockpit does not bring
  it along, so add it. The Spring Boot BOM names its version.
- The Keycloak directory needs no library. It logs in with a client whose service account has the
  role `view-users` of `realm-management`. Newer releases of Keycloak also need the client scope
  `kc-admin-api` among the default client scopes of that client.
- Do not keep a bean of type `UserDirectoryService` of your own next to it. The start stops if you
  do, and it stops as well if there is no directory at all. Both messages say what to do.

The wiki page about security says more.

### The update of the list of workflows takes `businessIds`

`PUT /workflow` takes `businessIds` now, the same filter `POST /workflow` has. Before, the update
left the business ids out, so a list filtered by them got workflows which did not belong to it.
The field is optional. A user interface which filters by business ids sends them with the update
too, and one which does not use them changes nothing.

A subclass of `AbstractWorkflowListGuiApiController` which overrides `getWorkflowsUpdated` gets the
business ids as a new parameter after `initialTimestamp`. A caller of
`WorkflowlistService.getWorkflowsUpdated` passes them at the same place, or `null` for all
workflows.

### A list can be limited to workflow modules

Every request of a list takes the optional `workflowModuleIds` now: `POST` and `PUT` of
`/usertask` and `/workflow`, the two count endpoints and the suggestions of a search field. The
list then holds only the entries of these workflow modules. Without the field, or with an empty
array, a list holds the entries of every workflow module, as it always did. A user interface which does not use the field changes
nothing.

Each element of the update stream names the workflow module of its entity in `workflowModuleId`,
next to `event`. A user interface which checks the keys of an element strictly has to accept the
new one.

A Java caller has to change. A subclass of `AbstractUserTaskListGuiApiController` which overrides
`getUserTasks`, `countUserTasks`, `getUserTasksUpdated` or `kwic` gets the workflow module ids as a
new parameter. So does a subclass of `AbstractWorkflowListGuiApiController` which overrides
`getWorkflows`, `countWorkflows`, `getWorkflowsUpdated` or `kwic`. A caller of the methods of the
same names in `UserTaskService` and `WorkflowlistService` passes the ids, or `null` for every
workflow module. In all of these methods the new parameter comes right before `searchQueries`.

An implementation of `UpdateStreamAudience` returns `WhatTheStreamsMaySee` from
`whatEachStreamMaySee` now. It carries the visible ids per stream as before, and the workflow
module of each changed entity. The records `UserTaskVisibility.TaskFacts` and
`WorkflowVisibility.WorkflowFacts` have the new last component `workflowModuleId`.

Two new changesets index `workflowModuleId` together with `endedAt` in the collections `usertask`
and `workflow`.

### Where the rights of a user come from

`GET /gui/api/v1/app/current-user/group-hierarchy` is new. It answers the user signed in with the
groups the user directory gives them, and for each workflow module with a group hierarchy the
entries of the user's groups. So a user interface can show where each group and right comes from.
Nothing breaks. A chain of your own needs nothing new, as long as it does not open
`/gui/api/v1/app/current-user/**`.
