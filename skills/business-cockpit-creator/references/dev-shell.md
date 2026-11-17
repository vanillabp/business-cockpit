# The dev shell

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Somebody who writes a form for a workflow module needs something it runs in. The real cockpit is a
poor fit. It loads the built files of a module at runtime, so hot module replacement does not work:
every change needs a production build of the module and a reload in the browser. On top of that, a
cockpit, a MongoDB and a BPMS have to run.

The dev shell is the way out. It simulates the Business Cockpit for the developer of a module. It is
a small web application, written in the framework of the module, that imports the module's
components directly from their source files, not through the built visual entry. So the dev server
swaps them while the developer types. It still draws each component inside a host element with a
shadow root, as the cockpit does. Behind it runs the dev shell simulator, a Java application that answers what the
cockpit would answer. The workflow module itself runs unchanged against the dev shell.

The generated project has one dev shell package per framework the modules use
(`Q-MODULE-FRAMEWORKS`), see [project.md](project.md#the-dev-shell-libraries).

1. [The rule](#the-rule)
   - [What surrounds a part in the cockpit](#what-surrounds-a-part-in-the-cockpit)
   - [How to find the chain](#how-to-find-the-chain)
   - [A browser test for it](#a-browser-test-for-it)
2. [What a dev shell must do](#what-a-dev-shell-must-do)
3. [The entry point](#the-entry-point)
4. [What it shows](#what-it-shows)
5. [Where the data comes from](#where-the-data-comes-from)
6. [The dev shell simulator](#the-dev-shell-simulator)
7. [The proxy of the dev server](#the-proxy-of-the-dev-server)
8. [Where it differs from the cockpit on purpose](#where-it-differs-from-the-cockpit-on-purpose)
9. [Developing inside the cockpit instead](#developing-inside-the-cockpit-instead)

## The rule

For the components of the workflow module there must be no difference between the dev shell and
the cockpit. A form that works and looks right in the dev shell works and looks exactly the same in
the cockpit. This covers three things:

- The data. The components get the same objects, `BcUserTask` and `BcWorkflow` from the module
  kit, with the same functions and the same language.
- The styles. The same CSS reaches the component: the custom properties of the theme, the inherited
  properties such as the font and the colour, and the module's own style inside its shadow root.
  No stylesheet of the dev shell reaches it.
- The element hierarchy. The component sits in the same chain of elements as in the cockpit: the
  same host element with an open shadow root, inside the same layout boxes of the page.

Data alone is not enough. A form can get the right data and still look different, because a box
around it is narrower, scrolls somewhere else or passes another font into the shadow root.

### What surrounds a part in the cockpit

The cockpit draws a part through its module host. From the page down to the part, the chain is:

1. The root of the document. The theme library sets the custom properties of the theme on it
   (`Q-THEME-TOKENS`), see [project.md](project.md#the-theme-library). The base styles of the same
   library set the font, the colour and the margins of `<html>` and `<body>`. Fonts are declared
   with `@font-face` here, in the document, not in a shadow root.
2. The layout boxes of the page. On the page of a user task or a case, these are the application
   header and the box of each area: `body` in the main area, which scrolls, `information` at the top
   of the column on the right, `actions` at its bottom, or one box for `all`, see
   [module-loading.md](module-loading.md#the-page-of-a-user-task-or-a-case). The other extension
   points have their own boxes: the header for a button, the content area under the menu for a page
   of a module, the modal layer for a dialog. The size, `display` and `overflow` of these boxes
   decide how much room a part has and where it scrolls.
3. The host element. It is the custom element with the name the data entry returned, for example
   `taxi-ride-assess-risk-form`, built by `defineBcElement`, see
   [types.md](types.md#the-event-protocol). It is in the cockpit's own DOM, so the cockpit's styles
   reach it from outside, and a rule of the cockpit on it wins over the module's `:host` rule. A
   custom element is `display: inline` unless a style says otherwise.
4. Its open shadow root. Selectors of the cockpit stop here. Only inherited properties pass, such
   as `font-family`, `font-size`, `line-height`, `color` and `direction`, and all custom
   properties. A `rem` size inside the shadow root still follows the font size of `<html>`.
5. A `<style>` element with the module's style, in front of the framework's root, see
   [rough-edges.md](rough-edges.md#vue-in-a-shadow-root).
6. The component of the module.

The dev shell builds steps 1 to 5 the same way and puts the developer's component in at step 6. It
takes the theme and the base styles from `<scope>/bc-theme`, and builds the same layout boxes. A dev shell in another framework than the cockpit's cannot import the cockpit's
layout components, so it builds the boxes again with the same elements and the same CSS. For the host element it calls `defineBcElement` from the module kit, the same
helper the module's visual entry uses, with the same name. Its `mount` writes the module's style
into the shadow root and renders the component that was handed over, as the module's own `mount`
does. The component is still imported from its source file, so the dev server still swaps it while
the developer types.

The dev shell's own controls, the drop-down, the choice of the user and the choice of the language,
are not part of this chain. They sit next to it, for example in a bar above the page, and none of
their styles reach into it.

### How to find the chain

Do not guess the chain. Read it from the finished cockpit:

1. Start the cockpit with its backend and one workflow module, and open one user task of that
   module.
2. In the developer tools of the browser, select the host element of the module's form. Write down
   every ancestor from `<html>` to the host element: the element name, its classes, and its
   `display`, `position`, `overflow`, `width` and `height`.
3. Look at the computed styles of the host element and of the first element in its shadow root.
   Write down the inherited properties from step 4 above and every custom property of the theme.
   Also write down which rules of the cockpit match the host element itself.
4. Build exactly this in the dev shell: the same ancestors with the same layout properties, the
   same base styles and the same values in the shadow root.
5. Do the same for the page of a case and for every extension point from `Q-EXTENSION-POINTS`.

Repeat it whenever the layout or the base styles of the user interface change. The test below
notices when somebody forgets.

### A browser test for it

The end-to-end tests of the generated project prove the rule too (`DS-018`, `DS-019`). The test
opens a user task of the same workflow module and task definition once in the cockpit and once in
the dev shell, finds the host element by its name, and compares:

- the chain of ancestors, from the host element up to `<html>`: the element names, `display`,
  `position` and `overflow`, and the width of the box of the area, with the same size of
  the browser window in both,
- the computed styles of the host element and of the first element in its shadow root: the
  inherited properties and the custom properties of the theme.

Read both with `getComputedStyle` in the page. Walk up the chain with `parentElement`, and with
`getRootNode().host` where a shadow root ends. The test fails on the first difference and names the
element and the property. Do the same for the page of a case.

## What a dev shell must do

It stands in for the pages of the cockpit that load parts of a module: the page of a user task, the
page of a case, and the places of every other extension point agreed in the interview
(`Q-EXTENSION-POINTS`), for example the buttons in the header, the pages of a module with their
menu entries, and the modal dialogs they open.

For this it must:

1. Import the module's data entries and components directly, not through the manifest and
   `import()`.
2. Show real data. The user task and the case come from the GUI API of the simulator, not from a
   file with sample data. Only then does the shape of the data match the cockpit's.
3. Wrap the data into `BcUserTask` and `BcWorkflow`, so the functions a part expects are there,
   `sendTo` and `receiveMessage` included, and hand every part `showNotification`. See
   [types.md](types.md#bcusertask-and-bcworkflow).
4. Switch between the parts, and between the user tasks and cases there are.
5. Offer the users of the simulator to choose from. The developer picks a user, switches to another
   one, and deselects the user again. Picking a user sets up a real security context: the simulator
   writes a JWT into the same cookie the cockpit uses, see
   [The dev shell simulator](#the-dev-shell-simulator). A form shows different things to different
   users, and this is how the developer sees it.
6. Offer every agreed extension point for testing, not only the page of a user task and the page of
   a case. A header button shows in a header like the cockpit's, a page of a module under its route
   `/<workflowModuleId>/<pageId>` with its menu entry, and a dialog opens modally.
7. Show a toast when a part calls `showNotification`, in the look of its `level`, and keep a
   `sticky` one until it is clicked away.
8. Let the developer choose the language from the languages of the cockpit (`Q-LANGUAGES`).
9. Look exactly like the cockpit, see [The rule](#the-rule). The theme from the interview
   (`Q-THEME`) comes from the theme library `<scope>/bc-theme`, the workspace the user interface
   takes it from too, see [project.md](project.md#the-theme-library). The application header with
   the task actions, the areas `body`, `information` and `actions` (`Q-TASK-PAGE-AREAS`,
   `Q-WORKFLOW-PAGE-AREAS`) and the scrolling are laid out as in the cockpit, with the same elements
   and the same styles.

A dev shell for Angular runs zoneless, as an Angular module does in the cockpit, see
[module-loading.md](module-loading.md#nothing-is-shared). A form that needs `zone.js` fails in the
dev shell, not first in the cockpit.

It needs no lists, no live updates and no login. Those are the cockpit's business and not the
workbench's. A view that copies a row of a list is only needed once modules can draw cells
themselves, see [module-loading.md](module-loading.md#cells-drawn-by-a-workflow-module).

## The entry point

The developer hands over their parts and an address, and the shell builds the application from them.
What is handed over is the real contract:

|                 What                  |                                                            Why the shell needs it                                                            |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| the address of the GUI API            | it builds its client from it, with the simulator `/official-api/gui/api/v1`                                                                  |
| the functions of the data entries     | `userTaskForm`, `workflowPage`, and for the other extension points for example `header` and `pages`; the shell asks them as the cockpit does |
| the components, by web component name | what the shell draws for each name the functions return; the module builds the names with the same `elementName` as its data entries         |
| the style of the components           | the stylesheet text the module's web components write into their shadow roots, so the shell writes the same, see [The rule](#the-rule)       |
| the workflow module id                | for the frame, and for the requests of a form through `/wm/`                                                                                 |

The form of the entry point follows the framework, and it does not have to be the same everywhere:

- React: `bootstrapDevShell(elementId, options)`, which renders into the element with that id.
- Angular: a function that returns an `ApplicationConfig`, plus a `bootstrapDevShell` that starts it.
- Vue: a component `DevShellApp` with properties, or a plugin.

Use what is natural in the framework. What is handed over is the same.

The functions are the module's own data entries, imported from their source files. So the shell
runs the same routing by process and task definition as the cockpit, see
[module-loading.md](module-loading.md#data-entries-carry-no-framework). Of the three steps of
[Ask, load, draw](module-loading.md#ask-load-draw), the shell does the first and the last:

1. Ask. It calls the function with the user task, the case or the current user, the same as the
   cockpit. `undefined` means "not mine", as in the cockpit. It checks every name of the answer
   against the module id it was handed, by the same rule as the cockpit, and shows the same note for
   a name it refuses, see
   [module-loading.md](module-loading.md#the-cockpit-checks-every-name). So the author of a module
   finds a wrong name in the dev shell, not first in the cockpit.
2. Load. The shell skips it. It does not load the visual entry the answer names.
3. Draw. For each web component name in the answer, the shell draws the framework component that
   was handed over under this name, inside a host element of that name with a shadow root, see
   [What surrounds a part in the cockpit](#what-surrounds-a-part-in-the-cockpit). A string is the all-in-one form, an object of areas the
   laid-out form. The shell picks the areas, or `all`, by the same rules as the cockpit, see
   [module-loading.md](module-loading.md#the-page-of-a-user-task-or-a-case), so it lays out the
   page the same way.

The components are imported directly, so the dev server swaps them while the developer types. A
name the function returns but for which no component was handed over shows a note with that name,
so the developer sees the mistake in the shell and not first in the cockpit.

## What it shows

Three surfaces are enough:

- A start page, with a way to each part and to each extension point, and the choice of the user.
- A page per part, with the id of the user task or the case in the address, for example
  `/tasks/<id>` and `/workflows/<id>`.
- A header with a drop-down list of the user tasks or cases there are, searchable by name and with
  paging. The drop-down loads its entries in blocks of 20 and sorts by `createdAt` descending. The
  header also holds the language choice and the choice of the user, with an entry for "no user".

The drop-down always asks with the primary filter `All`, for user tasks and for cases, and offers no
other. It is only there to pick an entry while developing. The data comes from the simulator, so
there are few entries, and closed ones are worth trying too. This differs from the cockpit on
purpose: there the default is `OpenTasksWithoutFollowUp` for user tasks and `InProgress` for cases, see
[behaviour.md](behaviour.md#the-task-lists).

## Where the data comes from

From the GUI API, through a client generated the same way as the cockpit's, see
[gui-api.md](gui-api.md#generating-the-client). The shell only gets the base address. There is no file
with sample data.

That is why a dev shell shows nothing without the simulator. It is also why it is worth anything: a
form that runs here runs against the same shape of data as in the cockpit.

The functions on `BcUserTask` and `BcWorkflow` call the API directly. There is no event protocol in
the dev shell, because the components are not wrapped. What each function does:

|                 Function                 |                                                                   What the shell does                                                                    |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `open`, `navigateToWorkflow`             | go to the shell's own page of this user task or this case                                                                                                |
| `assign`, `unassign`, `claim`, `unclaim` | the simulator does not answer them; show a toast that says so, and resolve                                                                               |
| `getUserTasks`                           | call `POST /workflow/{id}/usertasks` and wrap the tasks again                                                                                            |
| `sendTo`, `receiveMessage`               | carry the message to the subscribed component of the other area, as the cockpit does; keep messages until the area subscribes, and stop on `unsubscribe` |
| `showNotification`                       | show the toast, as the cockpit does                                                                                                                      |
| `openDialog`                             | show the component handed over for its `element` in a modal layer, as the cockpit does                                                                   |

Doing nothing is allowed, as long as the developer notices. A function that silently does nothing is
the worst choice, because the developer then looks for the bug in their form.

`getUserTasks` must be real. A case page without its user tasks is no case page.

## The dev shell simulator

A Spring Boot application in `development/dev-shell-simulator` of the `business-cockpit`
repository, built as a runnable jar and attached to a release. It runs on port 8079 and keeps
everything in an in-memory H2 database. No MongoDB, no BPMS and no cockpit are needed.

It has two sides.

Towards the dev shell, it speaks the official GUI API under `/official-api/gui/api/v1`. Each
system a simulator stands in for gets a prefix of its own, and below it is the path of the original.
The GUI API of the cockpit has the prefix `/official-api`, and below it `/gui/api/v1` as in the
cockpit. So the paths of two simulated systems cannot collide. A workflow module whose generated
client of the GUI API should point to the simulator sets only the address with the prefix,
`spring.http.serviceclient.official-gui-api.base-url=http://localhost:8079/official-api`, and the
client adds `/gui/api/v1` itself, as with the cockpit.

Of the GUI API, the simulator answers a user task, the task list, a case, the case list and the
user tasks of a case. Paging and sorting happen in memory, the page size is 20 by default, and the
default order is `createdAt` descending. It also answers the user who is logged in,
`GET /app/current-user`, as the cockpit does, with all groups including the group hierarchies of the
registered workflow modules, and the notification settings under `/notifications`. It offers no
medium, because it sends nothing, and it keeps the settings in memory only.

Towards the workflow module, it speaks the BPMS API under `/bpms/api/v1_1`. A workflow module
registers there, and its reports about user tasks and cases come in there, the same as with the
cockpit. Version 1 under `/bpms/api/v1` gets a 404. The cockpit itself still answers it, see
[backend.md](backend.md#the-bpms-api). A module whose adapter still reports version 1 needs a
current adapter for the dev shell.

The module is pointed at it with one key: `vanillabp.cockpit.rest.base-url` points to
`http://localhost:8079` instead of to the cockpit. The address is the root; the transport adds
`/bpms/api/v1_1` itself, and that is exactly the path the simulator answers. No credentials are
needed, because the simulator leaves its data paths open. Where else the key may be set is in the
wiki under
[Connecting a workflow module](https://github.com/vanillabp/business-cockpit/wiki/Connecting-a-workflow-module).

There is also a small interface of its own for the shell, under `/dev-shell/user`:

|              Call               |  For whom   |              What it does              |
|---------------------------------|-------------|----------------------------------------|
| `GET /dev-shell/user/all/id`    | shell       | the ids of all users                   |
| `GET /dev-shell/user/all`       | shell       | all users with details                 |
| `GET /dev-shell/user/`          | shell       | the id of the user chosen now, as text |
| `POST /dev-shell/user/{userId}` | shell       | switch the user; `---` logs out        |
| `GET /dev-shell/user/details`   | application | the user chosen now, from the cookie   |
| `GET /dev-shell/user/{userId}`  | application | one user by id                         |

These calls and `GET /simulator-api/usertask/{userTaskId}/report`, the last report of a workflow
module about a user task (see below), are described in an OpenAPI file of their own,
`apis/dev-shell-simulator-api/openapi/dev-shell-simulator-api.yaml`. It is also in the jar
`io.vanillabp.businesscockpit:dev-shell-simulator-api-client`, and a shell generates its client from
it the same way as from the file of the official GUI API. Switching to an unknown user answers 404.

The pretend users are in `development/dev-shell-simulator/users.yaml`, under the key
`dev-shell-simulator.users`. Each user has `id`, `email`, `first-name`, `last-name`, `groups` as a
comma-separated list, and `attributes` as a free map. `john`, `jane` and `joe` come with it, with the
groups `ADMIN` and `RISK_ASSESSMENT`.

These users are three things at once: the entries of the user choice in the shell, the user
directory of the simulator, and their groups become the rights in the token. The simulator does not
resolve groups in the token on purpose. A group hierarchy is the business application's job. Only
`GET /official-api/gui/api/v1/app/current-user` answers with the resolved groups, as the cockpit does.

Nobody logs in. The simulator writes the same JWT into the same cookie the cockpit uses, and switching
the user is a `POST`. The secret for signing is in plain text in its configuration, because the point
is not security but a real token in the right shape. The data paths `/official-api/**`,
`/bpms/api/**` and `/simulator-api/**` are open to everyone.

A report about a user task carries more than the GUI API shows: `excludedCandidateUsers`,
`admittedUsers` and `notificationDelivery`. The simulator keeps them, and
`GET /simulator-api/usertask/{userTaskId}/report` answers the last report which created, updated or
ended the task, exactly as the module sent it. No cockpit answers this path, so a dev shell uses it
only to show the developer what the module reports, and never in a form.

A project which extends the simulator with an application of its own (`extends DevShellSimulator`)
keeps its defaults: port, H2, schema update and the JWT secret come from
`dev-shell-simulator-defaults.yaml` with the lowest priority. Its own `application.yaml` names only
what differs, and the users.

The simulator is an application on purpose, not a set of test data. What it answers must be what the
cockpit answers, or a form runs here and not there.

What it does not answer is the rest of the GUI API: claim, assign, mark as read, follow-up, the update
form of the lists, the user search, the suggestions for the search field and the update stream. A
shell that calls one of these calls into nothing.

## The proxy of the dev server

The shell runs on the dev server of the framework, the simulator on port 8079. So that the cookie
travels along and the browser sees no other origin, the dev server forwards:

|        Path        |                                 Target                                  |
|--------------------|-------------------------------------------------------------------------|
| `/official-api/**` | `http://localhost:8079`                                                 |
| `/dev-shell/**`    | `http://localhost:8079`                                                 |
| `/wm/<id>/**`      | the workflow module, if its forms call their own backend through `/wm/` |

Plus the fallback to the start page for unknown paths, because the shell is a single-page
application. The generated dev shell brings this proxy configuration for its dev server.

## Where it differs from the cockpit on purpose

A dev shell is not meant to be the cockpit. These differences are wanted, and a developer must know
them, so they do not believe their form passed a check the shell never makes:

- There are no live updates. The simulator has no update stream.
- There is no login. The user is chosen, not logged in. The cookie is real, but the data paths
  check nothing.
- The visibility rules of the cockpit are not copied. What the shell shows, it shows to everybody.
- The paths have a prefix. The GUI API is under `/official-api/gui/api/v1` instead of
  `/gui/api/v1`, because each simulated system gets a prefix of its own. Below it the path is the
  same as in the original, so only the address a client gets changes.
- The actions that change a user task are not simulated, see
  [Where the data comes from](#where-the-data-comes-from).

## Developing inside the cockpit instead

Some customers want to develop directly in the cockpit. That has an advantage and two drawbacks.

The advantage: the form runs in the real cockpit, so no difference between the dev shell and the
cockpit can hide. By [The rule](#the-rule) such a difference is a bug of the dev shell, and the
browser test finds it, but only for what it compares.

The drawbacks: the whole cockpit has to be set up, with MongoDB, while the dev shell uses an
in-memory H2 database. And parts the cockpit loads with `import()` are not updated while the
developer types; every change needs a production build and a reload in the browser.
