# The dev shell

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Somebody who writes a form for a workflow module needs something it runs in. The real cockpit is a
poor fit. It loads the built files of a module at runtime, so hot module replacement does not work:
every change needs a production build of the module and a reload in the browser. On top of that, a
cockpit, a MongoDB and a BPMS have to run.

The dev shell is the way out. It simulates the Business Cockpit for the developer of a module. It is
a small web application, written in the framework of the module, that imports the module's
components directly, not through the web component wrapper. So the dev server swaps them while the
developer types. Behind it runs the dev shell simulator, a Java application that answers what the
cockpit would answer. The workflow module itself runs unchanged against the dev shell.

The generated project has one dev shell package per framework the modules use
(`Q-MODULE-FRAMEWORKS`), see [project.md](project.md#the-dev-shell-libraries).

1. [The rule](#the-rule)
1. [What a dev shell must do](#what-a-dev-shell-must-do)
1. [The entry point](#the-entry-point)
1. [What it shows](#what-it-shows)
1. [Where the data comes from](#where-the-data-comes-from)
1. [The dev shell simulator](#the-dev-shell-simulator)
1. [The proxy of the dev server](#the-proxy-of-the-dev-server)
1. [Where it differs from the cockpit on purpose](#where-it-differs-from-the-cockpit-on-purpose)
1. [Developing inside the cockpit instead](#developing-inside-the-cockpit-instead)

## The rule

For the components of the workflow module there must be no difference between the dev shell and
the cockpit. They get the same objects, `BcUserTask` and `BcWorkflow` from the types library, with
the same functions, the same language and the same theme. A form that works in the dev shell works in
the cockpit.

## What a dev shell must do

It stands in for the pages of the cockpit that load parts of a module: the page of a user task, the
page of a case, and the places of every other extension point agreed in the interview
(`Q-EXTENSION-POINTS`), for example the buttons in the header, the pages of a module with their
menu entries, and the modal dialogs they open.

For this it must:

1. Import the module's components directly, not through the manifest and `import()`.
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
9. Look like the cockpit. The theme from the interview (`Q-THEME`) is built into the base pages when
   the dev shell is generated, so a user task or a case looks as close to pixel-identical to the
   cockpit as possible. The application header with the task actions, the areas `body`,
   `information` and `actions` (`Q-PAGE-AREAS`) and the scrolling are laid out as in the cockpit.

A dev shell for Angular runs zoneless, as an Angular module does in the cockpit, see
[module-loading.md](module-loading.md#nothing-is-shared). A form that needs `zone.js` fails in the
dev shell, not first in the cockpit.

It needs no lists, no live updates and no login. Those are the cockpit's business and not the
workbench's. A view that copies a row of a list is only needed once modules can draw cells
themselves, see [module-loading.md](module-loading.md#cells-drawn-by-a-workflow-module).

## The entry point

The developer hands over their parts and an address, and the shell builds the application from them.
What is handed over is the real contract:

| What                        | Why the shell needs it |
|-----------------------------|------------------------|
| the address of the GUI API  | it builds its client from it, with the simulator `/official-api/v1` |
| the user task form          | the page of a user task |
| the case page               | the page of a case |
| further parts by their name | for the organisation's other extension points |
| the workflow module id      | for the frame, and for the requests of a form through `/wm/` |

The form of the entry point follows the framework, and it does not have to be the same everywhere:

- React: `bootstrapDevShell(elementId, options)`, which renders into the element with that id.
- Angular: a function that returns an `ApplicationConfig`, plus a `bootstrapDevShell` that starts it.
- Vue: a component `DevShellApp` with properties, or a plugin.

Use what is natural in the framework. What is handed over is the same.

The user task form and the case page are handed over as a function of the same shape as the one the
module's entries export, `userTaskForm` and `workflowPage`, see
[module-loading.md](module-loading.md#the-page-of-a-user-task-or-a-case). The only
difference: it returns the module's component of the framework instead of the name of a web
component. One component is the all-in-one form, a map of areas to components is the laid-out
form. So the dev shell lays out the page the same way, and the dev server can swap the components.

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
purpose: there the default is `OpenTasksWithFollowUp` for user tasks and `Active` for cases, see
[behaviour.md](behaviour.md#the-task-lists).

## Where the data comes from

From the GUI API, through a client generated the same way as the cockpit's, see
[gui-api.md](gui-api.md#generating-the-client). The shell only gets the base address. There is no file
with sample data.

That is why a dev shell shows nothing without the simulator. It is also why it is worth anything: a
form that runs here runs against the same shape of data as in the cockpit.

The functions on `BcUserTask` and `BcWorkflow` call the API directly. There is no event protocol in
the dev shell, because the components are not wrapped. What each function does:

| Function                                 | What the shell does |
|------------------------------------------|---------------------|
| `open`, `navigateToWorkflow`             | go to the shell's own page of this user task or this case |
| `assign`, `unassign`, `claim`, `unclaim` | the simulator does not answer them; show a toast that says so, and resolve |
| `getUserTasks`                           | call `POST /workflow/{id}/usertasks` and wrap the tasks again |
| `sendTo`, `receiveMessage`               | carry the message to the subscribed component of the other area, as the cockpit does; keep messages until the area subscribes, and stop on `unsubscribe` |
| `showNotification`                       | show the toast, as the cockpit does |
| `openDialog`                             | show the component in a modal layer, as the cockpit does |

Doing nothing is allowed, as long as the developer notices. A function that silently does nothing is
the worst choice, because the developer then looks for the bug in their form.

`getUserTasks` must be real. A case page without its user tasks is no case page.

## The dev shell simulator

A Spring Boot application in `development/dev-shell-simulator` of the `business-cockpit`
repository, built as a runnable jar and attached to a release. It runs on port 8079 and keeps
everything in an in-memory H2 database. No MongoDB, no BPMS and no cockpit are needed.

It has two sides.

Towards the dev shell, it speaks the official GUI API under `/official-api/v1`. It answers five
calls of it: a user task, the task list, a case, the case list and the user tasks of a case.
Paging and sorting happen in memory, the page size is 20 by default, and the default order is
`createdAt` descending.

Towards the workflow module, it speaks the BPMS API under `/bpms/api/v1` and `/bpms/api/v1_1`. A
workflow module registers there, and its reports about user tasks and cases come in there, the same
as with the cockpit.

The module is pointed at it with one key: `vanillabp.cockpit.rest.base-url` points to
`http://localhost:8079` instead of to the cockpit. The address is the root; the transport adds
`/bpms/api/v1_1` itself, and that is exactly the path the simulator answers. No credentials are
needed, because the simulator leaves its data paths open. Where else the key may be set is in the
wiki under
[Connecting a workflow module](https://github.com/vanillabp/business-cockpit/wiki/Connecting-a-workflow-module).

There is also a small interface of its own for the shell, under `/dev-shell/user`:

| Call                            | For whom    | What it does |
|---------------------------------|-------------|--------------|
| `GET /dev-shell/user/all/id`    | shell       | the ids of all users |
| `GET /dev-shell/user/all`       | shell       | all users with details |
| `GET /dev-shell/user/`          | shell       | the id of the user chosen now, as text |
| `POST /dev-shell/user/{userId}` | shell       | switch the user; `---` logs out |
| `GET /dev-shell/user/details`   | application | the user chosen now, from the cookie |
| `GET /dev-shell/user/{userId}`  | application | one user by id |

The pretend users are in `development/dev-shell-simulator/users.yaml`, under the key
`dev-shell-simulator.users`. Each user has `id`, `email`, `first-name`, `last-name`, `groups` as a
comma-separated list, and `attributes` as a free map. `john`, `jane` and `joe` come with it, with the
groups `ADMIN` and `RISK_ASSESSMENT`.

These users are three things at once: the entries of the user choice in the shell, the user
directory of the simulator, and their groups become the rights in the token. The simulator does not
resolve groups on purpose. A group hierarchy is the business application's job.

Nobody logs in. The simulator writes the same JWT into the same cookie the cockpit uses, and switching
the user is a `POST`. The secret for signing is in plain text in its configuration, because the point
is not security but a real token in the right shape. The data paths `/official-api/**` and
`/bpms/api/**` are open to everyone.

The simulator is an application on purpose, not a set of test data. What it answers must be what the
cockpit answers, or a form runs here and not there.

What it does not answer is the rest of the GUI API: claim, assign, mark as read, follow-up, the update
form of the lists, the user search, the suggestions for the search field, the notifications and the
update stream. A shell that calls one of these calls into nothing.

## The proxy of the dev server

The shell runs on the dev server of the framework, the simulator on port 8079. So that the cookie
travels along and the browser sees no other origin, the dev server forwards:

| Path               | Target |
|--------------------|--------|
| `/official-api/**` | `http://localhost:8079` |
| `/dev-shell/**`    | `http://localhost:8079` |
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
- The actions that change a user task are not simulated, see
  [Where the data comes from](#where-the-data-comes-from).

## Developing inside the cockpit instead

Some customers want to develop directly in the cockpit. That has an advantage and two drawbacks.

The advantage: global CSS styles of the cockpit sometimes change how a form looks, and the dev shell
does not show that unless it uses the same styles. Shadow DOM and the theme built into the dev shell
keep this difference small.

The drawbacks: the whole cockpit has to be set up, with MongoDB, while the dev shell uses an
in-memory H2 database. And parts the cockpit loads with `import()` are not updated while the
developer types; every change needs a production build and a reload in the browser.
