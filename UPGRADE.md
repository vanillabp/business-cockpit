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

### `uiUriType` is a string, and the address is the user interface's business

`uiUriType` was an enum with two values in four published schemas. It is a plain string now, in
`bpms-api` v1 and v1_1, in `official-gui-api` v1 and in `workflow-provider-api`. `EXTERNAL` and
`WEBPACK_MF_REACT` are still valid strings, so a workflow module which reports one of them needs no
change. `workflow-provider-api` called the schema `UiComponentsType` and showed `WEBPACK_REACT` as
its example. Both are gone, and all four schemas now name the same thing the same way.

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

If your application brings a user interface of its own, nothing changes. Put its `index.html` at
`classpath:/static/index.html`, or name it with `application.spa-default-file`, and unknown paths
get it as before. Decisions 62 and 63 in [DECISIONS.md](./DECISIONS.md) have the details.

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
