---
name: business-cockpit-creator
description: Generate a VanillaBP Business Cockpit that fits an organisation's own tech stack, as two repositories. The frontend is one npm workspaces repository with the user interface in any web framework, the types, the cockpit's own module kit and module host, the theme, dev shells for workflow module parts and an nginx image. The backend is a Maven project with the cockpit application on the business-cockpit library, the list controllers, the login (OIDC with Keycloak by default), a types jar and a docker-compose.yaml with MongoDB, Kafka, Mailpit and Keycloak. The agent interviews the user, generates both repositories and verifies them against a versioned checklist. Covers the GUI API, lists over large data, loading workflow module parts at runtime as ES modules and web components, and the behaviour users expect. Use whenever somebody wants their own Business Cockpit, its user interface or backend, wants to upgrade a generated one to a newer checklist, or needs a dev shell for the parts of a workflow module.
license: Apache-2.0
metadata:
  author: vanillabp
  homepage: https://www.vanillabp.io
---

# Generating a Business Cockpit

The Business Cockpit is the central user interface of a company. Employees find the user tasks that
the digitised business processes give them, and they look up the state of a business case. The
VanillaBP cockpit backend has no user interface of its own. Every organisation brings its own
framework, its own version of it, its own component library and its own design. So no single user
interface fits, and this skill does not describe one.

This skill is a generator. You use it to generate the cockpit of one organisation: a user interface
that fits its tech stack, and on request the backend application that serves it. You work in three
phases:

1. [Interview](#phase-1-interview). You ask, and you assume nothing.
2. [Generate](#phase-2-generate). You build both repositories from the answers and from the
   references.
3. [Verify](#phase-3-verify). You check the result against the checklist, item by item.

Example user interfaces in React, Angular and Vue exist, each generated with this skill. They are
starting points, not finished applications.

The skill lives in the repository `business-cockpit`, because it describes that repository's own
interfaces. If it lived somewhere else, it would get out of date apart from the code. Where it and
the code disagree, the code is right and the text is the bug.

Install it with `gh skill install vanillabp/business-cockpit business-cockpit-creator`. The skill
`business-cockpit` in `vanillabp/skills` only says that the cockpit exists and where to read more;
this skill is the one that builds a cockpit.

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

Two repositories, each with one build tool. Running the skill builds both: the frontend repository
always, the backend repository when the user wants it (`Q-BACKEND`).
[references/project.md](references/project.md) describes them.

The UI repository is one npm repository with npm workspaces. Every npm package below is a
workspace of it, and one `package-lock.json` at the root pins the dependencies of all of them, see
[references/project.md](references/project.md#the-npm-workspaces). It produces:

- The types library `<scope>/bc-types`. It holds the data of the GUI API and every constant of the
  contract with the workflow modules, such as the entries, the areas and the `ui-uri-type` values
  this user interface understands. It has no dependencies.
- The module kit `<scope>/bc-module-kit`, the package a workflow module installs. It is this
  cockpit's own copy of the VanillaBP kit `@vanillabp/bc-module-kit`, made to fit the interview.
  It is built on the types library and exports its types again, and it brings the runtime code a
  module needs: `defineBcElement`, the event protocol, the manifest and `bc-check-entries`. It covers
  the VanillaBP kit as its minimum, see
  [references/types.md](references/types.md#the-vanillabp-kit-is-the-minimum).
- The module host `<scope>/bc-module-host`, this cockpit's own copy of `@vanillabp/bc-module-host`.
  It loads the parts of the workflow modules. The user interface uses it, and so do the tests of a
  workflow module.
- The theme library `<scope>/bc-theme`. It holds the colours, fonts and logo of the cockpit, and
  the user interface and every dev shell take their look from it.
- The cockpit user interface, a single-page application. It shows the lists and loads the parts of
  the workflow modules into its pages at runtime.
- A dev shell for each framework the workflow modules use, as an npm package. It is the workbench on
  which somebody writes a user task form without starting a cockpit.
- The UI jar, with the built files at the place where the backend framework serves them. A Node
  script builds it and uploads it with HTTP `PUT` into the company's Maven repository.
- A `Dockerfile` that serves the user interface with nginx, for an organisation that deploys it
  apart from the backend (`Q-HOSTING` is `behind` or `in-front`).

The backend repository is a Maven project with a parent `pom.xml` and two modules, see
[references/backend.md](references/backend.md):

- `application`, the cockpit application of the organisation, the counterpart of the `container`
  module of the cockpit. It depends on the `business-cockpit` library and brings what the library
  leaves to the application: the controllers of the lists, the login, the user directory and the
  configuration. By default it pulls the UI jar as an ordinary dependency and serves the user
  interface itself.
- `types`, the types jar for the Java side of the workflow modules. It holds one constant for each
  `ui-uri-type` value, generated from the types library, so a workflow module reports the same
  values the user interface understands.
- A `docker-compose.yaml` that starts what the application needs on a developer's machine:
  MongoDB, Kafka, Mailpit, and Keycloak for an OIDC login.

When the organisation keeps a backend of its own, the skill does not create one. It tells the user
which dependency to add, and the types jar comes from a small Maven project of its own instead.

The user interface can also run in an nginx of its own. Then the backend still answers every request
of the browser and passes on to that nginx only what the cockpit does not know itself. The three ways
are described in [Where the user interface runs](#where-the-user-interface-runs), and `Q-HOSTING`
decides between them.

The types jar reads the npm package of the types library, and the user interface needs the OpenAPI
files of the cockpit jars first. Inside the cockpit build that would be a circle. So the order of a
release is: the cockpit release, the user interface, then the backend repository, see
[references/project.md](references/project.md#where-the-types-jar-is-built).

## Phase 1: interview

Ask the questions in [references/interview.md](references/interview.md). Ask all of them, and
write the answers down. Where a question has a default, propose it, but let the user decide. Never
fill in an answer because it sounds likely. An organisation that uses the cockpit in one department
wants different things from one that uses it company-wide.

The answers go into the UI repository as `INTERVIEW.md`. The next agent who changes the user
interface or the backend reads it first.

## Phase 2: generate

Generate the UI repository as [references/project.md](references/project.md) describes, and the
backend repository as [references/backend.md](references/backend.md) describes. Build each screen
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

### A module built against the kit loads in your cockpit

The VanillaBP kit `@vanillabp/bc-module-kit` is the minimum every cockpit keeps, see
[references/types.md](references/types.md#the-vanillabp-kit-is-the-minimum). Its repository
`vanillabp/business-cockpit-module-kit` has three example modules, the same module written in
React, in Vue and in Angular, and a browser test for them. Run that test against your cockpit
(`EXT-040`). It passes for a cockpit with areas and for one without, because the example modules
offer both.

1. Take the kit's repository at the version your module kit covers. Run `npm ci`,
   `npm run build:examples` and `npm run build:conformance`. This writes the folder
   `dist/conformance`, which works on its own: the built modules below `wm/react-module/`,
   `wm/vue-module/` and `wm/angular-module/`, a small web server `serve.mjs`, the test
   `cockpit.test.js` and a `package.json` with its one dependency, `playwright`.
2. In that folder run `npm install` and `npx playwright install chromium`, then start
   `node serve.mjs`. It prints the address of each module, for example
   `http://localhost:4173/wm/react-module`.
3. Register the three modules in your cockpit, the way a workflow module registers itself: one
   `POST /bpms/api/v1_1/workflow-module/{id}` per module, with the id `react-module`, `vue-module`
   or `angular-module` and the printed address as `uri`. Give it `accessibleToGroups` the test user
   has. Your cockpit then forwards `/wm/<id>/` to `serve.mjs`.
4. Create one user task per module with `POST /bpms/api/v1_1/usertask/created`: the module's id as
   `workflowModuleId`, `bpmnProcessId` `TaxiRide`, `taskDefinition` `confirmRide`, the ES module
   value of `Q-UI-URI-TYPE` (by default `ES_MODULES`) as `uiUriType` and `/bc-manifest.json` as
   `uiUriPath`, plus the other fields `UserTaskCreatedEvent` requires, such as `id`,
   `workflowId` and `timestamp`. Make the test user a candidate. Write both steps into a setup
   script of the end-to-end tests, with the credentials your cockpit asks for on the BPMS API, so
   they run again for every test run.
5. Run the test with the address of your cockpit and the path of the page of each user task:

   ```bash
   BC_COCKPIT_URL=http://localhost:8080 \
   BC_USER_TASK_PAGES="react-module=/tasks/4711 vue-module=/tasks/4712 angular-module=/tasks/4713" \
   BC_STORAGE_STATE=e2e/.auth/user.json \
   node --test cockpit.test.js
   ```

   `BC_STORAGE_STATE` names a file with a logged-in session, as Playwright's `storageState` writes
   it. Leave it out if your cockpit needs no login for the test.

The test opens each page and works with the form the way a user does. It finds the form in an open
shadow root, fills in a comment, claims the task and opens a dialog. A failure means that your
cockpit breaks the minimum. A blueprint built against the kit would then fail in your cockpit too.
Fix the cockpit, not the test.

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
   The two repositories: the npm workspaces of the UI repository, the user interface, the UI jar,
   the upload into the Maven repository, the nginx image, the backend repository, the types jar, the
   types library, the module kit, the module host, the theme library and the dev shells.
3. [references/backend.md](references/backend.md)<br>
   The backend repository the skill creates: the parent pom, the application, the controllers of
   the lists, the login, the BPMS API, the user directory, the configuration, the
   `docker-compose.yaml` and the test that starts the application.
4. [references/gui-api.md](references/gui-api.md)<br>
   What the server can do: generating the client, logging in, the endpoints, paging, sorting,
   filtering over reported business data, suggestions for a search field, the primary filter, the
   timestamp that keeps a list stable, and the update stream.
5. [references/module-loading.md](references/module-loading.md)<br>
   How a part of a workflow module gets into your page: self-contained ES modules loaded with
   `import()`, the manifest of a module, build recipes per framework, the extension points, data
   entries without a framework, the registry of modules and the central loader, the three steps
   ask, load and draw with their states, web components with shadow DOM, the areas of a page, and
   the event protocol.
6. [references/types.md](references/types.md)<br>
   What goes into the types library and the module kit the generated user interface publishes.
7. [references/behaviour.md](references/behaviour.md)<br>
   What the user expects: the screens, the header and the menu, the counts, the lists, the columns,
   notes, opening a task, the window of loaded rows, live updates, search, user settings, language,
   extension points and who sees what.
8. [references/dev-shell.md](references/dev-shell.md)<br>
   The workbench for a workflow module and the simulator behind it.
9. [references/rough-edges.md](references/rough-edges.md)<br>
   The places where the obvious thing is the wrong thing.
10. [references/reference-1.0.0.md](references/reference-1.0.0.md)<br>
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
UI jar is ready for it (`Q-BACKEND-FRAMEWORK`). A cockpit is either the runnable `container` jar of
the `business-cockpit` repository, or an application of the organisation's own that has the library
`io.vanillabp.businesscockpit:business-cockpit` as a dependency. The skill creates such an
application on request, see [references/backend.md](references/backend.md). The wiki describes it
for a reader under
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).
The library and the `container` jar bring no user interface.

The backend repository adds the UI jar as a dependency. A Spring Boot cockpit serves what lies below
`static` on its class path, a Quarkus application what lies below `META-INF/resources`. So the UI jar
puts its files there. The details are in [references/project.md](references/project.md#the-ui-jar).

### Where the user interface runs

There are three ways. Ask which one in the interview (`Q-HOSTING`); the default is `embedded`.

1. **`embedded`: inside the backend.** The backend takes the UI jar as a dependency and serves its
   files, as described above. One application, one deployment, and the backend's security covers
   everything. This is the default.
2. **`behind`: in an nginx of its own, behind the backend.** The user interface has its own release
   cycle and runs in the nginx image of the UI repository. The browser talks to the backend only. The
   backend has one more route in its Spring Cloud Gateway (the cockpit already uses
   `spring-cloud-starter-gateway-server-webmvc` for `/wm/`): `/**` to the nginx, with the lowest
   priority of all routes. So every path the cockpit knows is answered by the cockpit, and only what
   it does not know reaches the nginx. The backend's security covers the user interface as well, just
   as in `embedded`. This way has been built so far only with the session or the token in a cookie.
3. **`in-front`: in an nginx in front of the backend.** The nginx serves the user interface and
   forwards the GUI API, the workflow modules and the login to the backend, so the browser still sees
   one origin, see [references/project.md](references/project.md#in-front-of-the-backend-in-front). This way is only
   worth it when the user interface has to load before the login. Today almost every login goes
   through OIDC or OAuth2, a redirect to another site. Loading the user interface before that
   redirect only delays what the user sees. It is needed when the user interface sends the token in
   the `Authorization` header instead of a cookie. Then the user interface itself handles the token,
   so it has to load first.

Propose `embedded`. Propose `behind` when the user interface needs its own deployment. Propose
`in-front` only for a token in the `Authorization` header.

The library brings no login. How a user logs in is decided by the backend application, in its
security chains (`Q-LOGIN-KIND`). A backend the skill creates logs the user in at an OIDC provider,
by default the Keycloak of its `docker-compose.yaml`, or with a login form, see
[references/backend.md](references/backend.md#the-login). The runnable `container` jar is an example
that runs out of the box. Only it uses basic authentication, with a local user directory. See
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) and
[references/rough-edges.md](references/rough-edges.md#what-needs-a-newer-cockpit).

What your user interface needs from the backend application:

- Whether the login or the user interface comes first depends on `Q-HOSTING`. With `embedded` and
  `behind` the login comes first. A browser without a login gets the login of the backend before any
  file of the user interface, for example the redirect to an OIDC provider. Loading the user
  interface first would only delay that redirect. With `in-front` the single-page application loads
  first, because it may have to handle the token itself. Then its static files, its shell
  `index.html` and the two endpoints `GET /gui/api/v1/app/info` and
  `GET /gui/api/v1/app/current-user` stay open.
- In every variant the user interface logs in again when needed. When `current-user` answers without
  a user, or any other call answers 401, it sends the browser to the login of the backend. That
  happens when a session ends while the page is open.
- `application.spa-default-file` names the shell, by default `classpath:/static/index.html`. Every
  path that no controller and no file answers gets this file instead of an error. That is what
  makes a link into the middle of your application work.

## The order of building

Each step can be shown on its own. If you change the order, you build for a long time without
seeing anything.

1. The project skeleton: the npm workspaces with the types library, the module kit, the module host
   and the theme, the generated GUI API client, and the UI jar with an empty shell, uploaded into
   the Maven repository.
2. The backend repository, when the skill creates it: the parent pom, the application with the
   controllers of the lists, the login and the user directory, the `docker-compose.yaml` and the
   test that starts the application. Start it with the UI jar and see the shell load after the
   login. With a backend of the organisation's own, add the UI jar to it instead.
3. The frame and the login. `GET /app/info` and `GET /app/current-user` are enough for an
   application that knows who is there. Then the header with its toggle and the menu.
4. A list without the columns of the modules, with the primary filter and its counts, and with the
   window of loaded rows. This settles paging. Write the memory test and the scrolling test now, not
   at the end, and run them with a lot of data: three or four simulated workflow modules that
   together report 30,000 user tasks or cases. Run them on the primary filter for closed tasks as
   well, because that is where the most rows pile up. Watch two things, because they pull in opposite
   directions. The JavaScript heap stays below the limit. The list scrolls without stuttering, and a
   placeholder instead of the data shows only when the user scrolls very fast. A larger window of
   loaded rows scrolls more smoothly and costs more memory. Write the measurement of the list as
   well: scrolling down and up must not make the heap grow, and leaving the list must free it.
   Simulate a total of one million entries to see the cap of the list at work. See
   [references/behaviour.md](references/behaviour.md#paging-and-the-window-of-loaded-rows).
5. Sorting and searching. After this, the paths and their traps are clear.
6. The module kit with `bc-check-entries`, and the module host with the registry of modules and the
   central loader. Then
   the columns of a list, then the page of a user task with its areas, then the page of a case,
   then the other extension points.
7. The update stream. It comes late, because it needs most of the rest.
8. The dev shells, one per framework the workflow modules use.
9. The types jar: the module `types` of the backend repository, or the types module of its own with
   its `README.md`. Then the nginx image when `Q-HOSTING` is `behind` or `in-front`.
10. The end-to-end tests, one per checklist item.

## How you know it is done

Run the generated user interface against a running cockpit with workflow modules. As workflow
modules, use the Business Cockpit blueprints in the `vanillabp-blueprints` organisation. They do not
wait for a generated cockpit. They are built against the VanillaBP kit `@vanillabp/bc-module-kit`, a
small npm package in plain JavaScript from its own repository
(`vanillabp/business-cockpit-module-kit`). It holds the minimum of the contract: the four data
entries and their export names, the area `all`, the manifest, the event protocol,
`defineBcElement` and `bc-check-entries`. The blueprints test their modules with the second package
of that repository, `@vanillabp/bc-module-host`, a minimal host which loads a module part the way
[references/module-loading.md](references/module-loading.md) describes. Both packages are not on
npmjs.com yet; until they are, they come from a local Verdaccio registry.

The module kit you generate does not take the VanillaBP kit in. Together with the types library
below it, it is the one contract of this cockpit. It covers the VanillaBP kit as its minimum, with
the same names, and adds what only this user interface knows, such as its `ui-uri-type` values,
`Header` or the areas of a page (`PKG-043`). So a blueprint which passes against the VanillaBP kit
loads in every generated cockpit. A blueprint that offers a `Header` or
areas your cockpit does not have still loads, only without that part. One of the blueprints shows
user tasks, one shows a workflow details page, and one shows an extension of the header. If the
organisation's cockpit has extension points that no blueprint offers, extend a blueprint, so that
every extension point is tested by a real module.

Then:

1. Every item of `CHECKLIST.md` has a test that names its id, and every test passes.
2. The task list and the case list show rows with the columns the modules bring.
3. The page of a user task from a module loads, lays out the areas of `Q-TASK-PAGE-AREAS` or the one
   web component of `all`, and shows the data of the task.
4. A change to a task that somebody else makes shows up without a reload.
5. Claiming and unclaiming work, and the row shows it afterwards.
6. Paging, sorting and searching return what they should, also over reported business data.
7. Scrolling through 30,000 user tasks or cases of three or four modules keeps the JavaScript heap
   below the limit from the interview, also on the primary filter for closed tasks. The list scrolls
   without stuttering, and placeholders show only when the user scrolls very fast. Scrolling down
   and up again does not make the heap grow, and leaving the list frees it.
8. A user task whose `uiUriType` is `EXTERNAL` opens its address in a tab of its own and leaves
   the list intact.
9. The dev shell shows the page of a blueprint module the same way the cockpit shows it.
10. A page shows parts of modules in two frameworks other than the cockpit's, side by side, and the
    data entries of a module load without its framework.
11. The backend repository builds and serves the user interface the way `Q-HOSTING` chose: with the
    UI jar as a dependency (`embedded`), through its gateway route to the nginx image (`behind`), or
    behind the nginx image (`in-front`).
12. When the skill created the backend: `docker compose up` and the profile `local` start a cockpit
    a user logs in to the way `Q-LOGIN-KIND` chose, and every list of `Q-LIST-CONTROLLERS` answers.
13. A module built against the kit loads in your cockpit: the conformance test with the kit's
    example modules in React, Vue and Angular passes, see
    [A module built against the kit loads in your cockpit](#a-module-built-against-the-kit-loads-in-your-cockpit).
