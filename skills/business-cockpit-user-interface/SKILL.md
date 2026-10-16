---
name: business-cockpit-user-interface
description: Generate a user interface for the VanillaBP Business Cockpit that fits an organisation's own tech stack, in any web framework, together with a dev shell, a types library for workflow modules, a jar the cockpit backend pulls as a Maven dependency and an nginx image for a separate deployment. The agent first interviews the user (framework, design library, theme, the word for a case, screens, primary filters, columns, live updates, language, extension points, backend framework, coordinates, memory limit), then generates the project, then verifies it against a versioned checklist that also drives the end-to-end tests. Covers the GUI API with paging, sorting, filtering over reported business data and the update stream, lists that hold only a window of rows, loading the parts of workflow modules at runtime as self-contained ES modules with import() and a small manifest per module, sharing no dependencies so that cockpit and modules mix frameworks freely, the web components with shadow DOM those parts register, the event protocol between a module's part and the cockpit, the behaviour a user expects, and what a cockpit user interface never does. Use whenever somebody wants their own Business Cockpit front end, wants to upgrade a generated one to a newer checklist, or needs a dev shell for the parts of a workflow module.
license: Apache-2.0
metadata:
  author: vanillabp
  homepage: https://www.vanillabp.io
---

# Generating a Business Cockpit user interface

The Business Cockpit is the central user interface of a company. Employees find the user tasks that
the digitised business processes give them, and they look up the state of a business case. The
cockpit backend has no user interface of its own. Every organisation brings its own framework, its
own version of it, its own component library and its own design. So no single user interface fits,
and this skill does not describe one.

This skill is a generator. You use it to generate a cockpit user interface that fits the tech stack
of one organisation. You work in three phases:

1. [Interview](#phase-1-interview). You ask, and you assume nothing.
2. [Generate](#phase-2-generate). You build the project from the answers and from the references.
3. [Verify](#phase-3-verify). You check the result against the checklist, item by item.

Example user interfaces in React, Angular and Vue exist, each generated with this skill. They are
starting points, not finished applications.

The skill lives in the repository `business-cockpit`, because it describes that repository's own
interfaces. If it lived somewhere else, it would get out of date apart from the code. Where it and
the code disagree, the code is right and the text is the bug.

**Contents:**

1. [What you generate](#what-you-generate)
1. [Phase 1: interview](#phase-1-interview)
1. [Phase 2: generate](#phase-2-generate)
1. [Phase 3: verify](#phase-3-verify)
1. [The parts of this description](#the-parts-of-this-description)
1. [What a cockpit user interface never does](#what-a-cockpit-user-interface-never-does)
1. [How your user interface gets into the cockpit](#how-your-user-interface-gets-into-the-cockpit)
1. [The order of building](#the-order-of-building)
1. [How you know it is done](#how-you-know-it-is-done)

## What you generate

By default two repositories and one small Maven project, each with one build tool.
[references/project.md](references/project.md) describes them.

The UI repository is a pure npm project. It produces:

- The cockpit user interface, a single-page application. It shows the lists and loads the parts of
  the workflow modules into its pages at runtime.
- The UI jar, with the built files at the place where the backend framework serves them. A Node
  script builds it and uploads it with HTTP `PUT` into the company's Maven repository.
- A types library for workflow modules, as an npm package. It says what a part of a module may
  expect from the cockpit, and it carries the small helper which turns a function call into an event
  on the DOM element. It also lists the `ui-uri-type` values this user interface understands.
- A dev shell for each framework the workflow modules use, as an npm package. It is the workbench on
  which somebody writes a user task form without starting a cockpit.
- A `Dockerfile` that serves the user interface with nginx, for an organisation that deploys it
  apart from the backend.

The backend repository is a pure Maven project, the cockpit application of the organisation. It
pulls the UI jar as an ordinary dependency. The skill does not write the application, but it tells
the user which dependency to add.

The types module is a Maven project of its own, apart from the backend application and from the UI
build. It builds the types jar with the constants of the `ui-uri-type` values, for the Java side of
the workflow modules. It reads the npm package of the types library, and the user interface needs
the OpenAPI files of the cockpit jars first. Inside the cockpit build that would be a circle, so its
`README.md` explains the order: cockpit release, user interface, types module, backend.

## Phase 1: interview

Ask the questions in [references/interview.md](references/interview.md). Ask all of them, and
write the answers down. Where a question has a default, propose it, but let the user decide. Never
fill in an answer because it sounds likely. An organisation that uses the cockpit in one department
wants different things from one that uses it company-wide.

The answers go into the generated project as `INTERVIEW.md`. The next agent who changes the user
interface reads it first.

## Phase 2: generate

Generate the project as [references/project.md](references/project.md) describes. Build each screen
as [references/behaviour.md](references/behaviour.md) describes. Talk to the server as
[references/gui-api.md](references/gui-api.md) describes, and load the parts of the workflow
modules as [references/module-loading.md](references/module-loading.md) describes. Read
[references/rough-edges.md](references/rough-edges.md) before the first screen, not afterwards.

Then write `CHECKLIST.md` into the project:

1. Copy [references/reference-1.0.0.md](references/reference-1.0.0.md) into it, with the version
   of the reference at the top.
2. Drop the items that depend on an interview answer the user gave the other way. Mark them as
   "not chosen" with the question id, instead of deleting them, so a diff stays readable.
3. Add one item for every wish of the customer that the reference does not cover. Give these items
   their own id prefix, `CUST-`, and number them from `CUST-001`.

Every end-to-end test names the id of the item it proves, for example in its title:
`TL-004 the primary filter shows its count`. An item without a test is not done.

Ids are never reused and never renumbered. An item that no longer holds is marked as removed, and
its id stays taken.

## Phase 3: verify

Go through `CHECKLIST.md`, item by item. For each item, run its test or check it by hand, and write
down the result. Then do what [How you know it is done](#how-you-know-it-is-done) says.

### Upgrading a generated user interface

A later version of this skill brings a new reference, for example `reference-1.1.0.md`. An agent
who upgrades a generated user interface does this:

1. Read the reference version at the top of the project's `CHECKLIST.md`.
2. Diff `reference-<old>.md` against `reference-<new>.md`. The ids show what is new, what changed
   and what was removed.
3. Apply only that difference to the project and to its `CHECKLIST.md`. The `CUST-` items stay as
   they are.
4. Ask the interview questions that are new in this version.

## The parts of this description

Read them in this order. Each one builds on the one before.

1. [references/interview.md](references/interview.md)<br>
   The questions you ask before you generate anything, and their defaults.
2. [references/project.md](references/project.md)<br>
   The two repositories: the user interface, the UI jar, the upload into the Maven repository, the
   nginx image, the backend, the types module, the types library and the dev shells.
3. [references/gui-api.md](references/gui-api.md)<br>
   What the server can do: generating the client, logging in, the endpoints, paging, sorting,
   filtering over reported business data, suggestions for a search field, the primary filter, the
   timestamp that keeps a list stable, and the update stream.
4. [references/module-loading.md](references/module-loading.md)<br>
   How a part of a workflow module gets into your page: self-contained ES modules loaded with
   `import()`, the manifest of a module, build recipes per framework, the extension points, data
   entries without a framework, the registry of modules and the central loader, web components with
   shadow DOM, the areas of a page, the event protocol, and the states of loading.
5. [references/types.md](references/types.md)<br>
   What goes into the types library the generated user interface publishes.
6. [references/behaviour.md](references/behaviour.md)<br>
   What the user expects: the screens, the header and the menu, the counts, the lists, the columns,
   notes, opening a task, the window of loaded rows, live updates, search, user settings, language,
   extension points and who sees what.
7. [references/dev-shell.md](references/dev-shell.md)<br>
   The workbench for a workflow module and the simulator behind it.
8. [references/rough-edges.md](references/rough-edges.md)<br>
   The places where the obvious thing is the wrong thing.
9. [references/reference-1.0.0.md](references/reference-1.0.0.md)<br>
   The checklist of version 1.0.0. The base for `CHECKLIST.md` and for the end-to-end tests.

The wiki of the repository describes the same things for a reader who does not build a user
interface:
[Architecture](https://github.com/vanillabp/business-cockpit/wiki/Architecture),
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security),
[Configuration](https://github.com/vanillabp/business-cockpit/wiki/Configuration),
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit)
and
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites).

## What a cockpit user interface never does

These limits are the design, not thrift. If you cross them, you build a second system next to the
workflow modules.

It never contains a user interface part of a business process. Forms, case pages, list columns
and extensions of a workflow module are always loaded at runtime. This keeps the life cycle of the
cockpit apart from the life cycles of the business processes. Otherwise every change to the form of
one process would need a new release of the cockpit. From about ten processes on, that is a problem
for the organisation, and every release interrupts the users.

It has no business logic. What happens to a user task in business terms is decided by the workflow
module. The cockpit knows no case, it only knows what was reported.

It never completes a user task and never starts a case. A form calls the API of its own workflow
module for that. The cockpit learns that the task is done the same way it learns everything else,
as a report.

It never calls a workflow module itself. Only the module's own parts talk to their module, and they
do it through the cockpit's proxy under `/wm/<workflowModuleId>/`. The frame around a form never
does.

It has no login of its own. There is no login form, no user management and no rights check in the
browser. Login is the job of the backend. Who sees what is decided by the server, and what the
server does not deliver does not exist for your user interface.

All data of the cockpit comes through the GUI API. Two OpenAPI files describe it: the
`official-gui-api` and the cockpit's own file in the `business-cockpit` jar, see
[gui-api.md](references/gui-api.md). The client is generated from them, so it never goes into the
customer's Git repository.

## How your user interface gets into the cockpit

Today a cockpit is a Spring Boot application. A Quarkus variant of the cockpit comes later, and the
generator is ready for it (`Q-BACKEND-FRAMEWORK`). A cockpit is either the runnable `container` jar
of the `business-cockpit` repository, or an application of the organisation's own that has the
library `io.vanillabp.businesscockpit:business-cockpit` as a dependency. How such an application is
built is in the wiki under
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).
The library and the `container` jar bring no user interface.

The backend repository adds the UI jar as a dependency. A Spring Boot cockpit serves what lies below
`static` on its class path, a Quarkus application what lies below `META-INF/resources`. So the UI jar
puts its files there. The details are in [references/project.md](references/project.md#the-ui-jar).

An organisation that does not want the user interface inside the backend runs the nginx image of
the UI repository instead. It forwards the GUI API, the workflow modules and the login to the
backend, so the browser still sees one origin, see
[references/project.md](references/project.md#the-nginx-image).

The library sets no rules for login. How a user logs in is decided by the backend application. In
Spring Boot, for example, an unauthenticated request can be sent to the `/login` endpoint. Depending
on the configuration, that shows a login form or forwards to an OAuth2 login. The runnable
`container` jar is an example that runs out of the box. Only it uses basic authentication, with a
local user directory. See [Security](https://github.com/vanillabp/business-cockpit/wiki/Security).
Today the library still has a default chain of its own, with basic authentication and users in
memory. Prompt `1510` moves it into `container`, see
[references/rough-edges.md](references/rough-edges.md#what-needs-a-newer-cockpit).

What your user interface needs from the backend application:

- The single-page application loads without a login. Its static files, its shell `index.html` and
  the two endpoints `GET /gui/api/v1/app/info` and `GET /gui/api/v1/app/current-user` stay open.
- After that it logs in when needed. When `current-user` answers without a user, or any other call
  answers 401, the user interface sends the browser to the login of the backend.
- `application.spa-default-file` names the shell, by default `classpath:/static/index.html`. Every
  path that no controller and no file answers gets this file instead of an error. That is what
  makes a link into the middle of your application work.

## The order of building

Each step can be shown on its own. If you change the order, you build for a long time without
seeing anything.

1. The project skeleton: the npm workspace, the generated GUI API client, and the UI jar with an
   empty shell, uploaded into the Maven repository. Add it to the backend and see the shell load.
2. The frame and the login. `GET /app/info` and `GET /app/current-user` are enough for an
   application that knows who is there. Then the header with its toggle and the menu.
3. A list without the columns of the modules, with the primary filter and its counts, and with the
   window of loaded rows. This settles paging. Write the memory test now, not at the end.
4. Sorting and searching. After this, the paths and their traps are clear.
5. The types library with `bc-check-entries`, the registry of modules and the central loader. Then
   the columns of a list, then the page of a user task with its areas, then the page of a case,
   then the other extension points.
6. The update stream. It comes late, because it needs most of the rest.
7. The dev shells, one per framework the workflow modules use.
8. The types module with its `README.md`, and the nginx image.
9. The end-to-end tests, one per checklist item.

## How you know it is done

Run the generated user interface against a running cockpit with workflow modules. As workflow
modules, use the Business Cockpit blueprints in the `vanillabp-blueprints` organisation. One of
them shows user tasks, one shows a workflow details page, and one shows an extension of the header.
If the organisation's cockpit has extension points that no blueprint offers, extend a blueprint, so
that every extension point is tested by a real module.

Then:

1. Every item of `CHECKLIST.md` has a test that names its id, and every test passes.
2. The task list and the case list show rows with the columns the modules bring.
3. The page of a user task from a module loads, lays out its areas and shows the data of the task.
4. A change to a task that somebody else makes shows up without a reload.
5. Claiming and unclaiming work, and the row shows it afterwards.
6. Paging, sorting and searching return what they should, also over reported business data.
7. Scrolling through 20,000 cases keeps the JavaScript heap below the limit from the interview.
8. A user task whose `uiUriType` is `EXTERNAL` opens its address in a tab of its own and leaves
   the list intact.
9. The dev shell shows the page of a blueprint module the same way the cockpit shows it.
10. A page shows parts of modules in two frameworks other than the cockpit's, side by side, and the
    data entries of a module load without its framework.
11. The backend repository builds with the UI jar as a dependency, and the nginx image serves the
    same user interface against the same backend.
