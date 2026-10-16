# Loading the parts of a workflow module

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The cockpit shows lists. What a user sees after a click on a row comes from the workflow module.
The module builds its parts as a federated bundle, and the cockpit loads them into its own pages
at runtime. The mechanism is native federation.

The wiki page
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites)
describes the module's side: what it offers and how it builds. Here is the other side, what the
host does. The host is your user interface.

1. [Native federation](#native-federation)
1. [The rules](#the-rules)
1. [Extension points](#extension-points)
1. [Which data finds the part](#which-data-finds-the-part)
1. [One registry and one loader](#one-registry-and-one-loader)
1. [When a part is loaded](#when-a-part-is-loaded)
1. [Nothing is shared](#nothing-is-shared)
1. [A visual part is a web component](#a-visual-part-is-a-web-component)
1. [The page of a user task or a case](#the-page-of-a-user-task-or-a-case)
1. [The other extension points](#the-other-extension-points)
1. [The event protocol](#the-event-protocol)
1. [Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module)

## Native federation

Native federation is module federation built on browser standards: ES modules and import maps. It
does not depend on the UI framework or on the bundler. So the cockpit and each workflow module pick
their own framework, their own version and their own build tool.

Two sides:

- A workflow module builds its parts with `@softarc/native-federation`. It uses an adapter for its
  bundler: `@softarc/native-federation-esbuild` for esbuild (React, Vue, plain JavaScript),
  `@angular-architects/native-federation` for the Angular CLI. There is no official adapter for
  Vite today. Other bundlers implement the adapter contract. Check the list of adapters at
  [native-federation.com](https://native-federation.com) before you advise a module. The build
  writes `remoteEntry.json`, the description of what the module exposes.
- The host loads the parts with `@softarc/native-federation-orchestrator` and `es-module-shims`.
  The host needs no federation build of its own. The orchestrator is plain JavaScript and runs in
  any page.

The name of a remote is the `workflowModuleId`. The remote entry lies at the root of the module's
web path, so the cockpit reaches it at `/wm/<workflowModuleId>/remoteEntry.json`.

## The rules

Every part is loaded once. A part is the pair of a workflow module and an exposed name. The host
keeps one promise per pair and hands out the same one to every caller.

A part never comes from the cockpit's own bundle. The cockpit never contains a user interface part
of a business process, see [`../SKILL.md`](../SKILL.md#what-a-cockpit-user-interface-never-does).

Every exposed part is a plain JavaScript function. A function that stands for something visible
returns the names of web components. The module registers these web components itself, and each of
them draws into its own shadow DOM.

Nothing is shared between the host and the modules, except the types library and DOM events, see
[Nothing is shared](#nothing-is-shared).

## Extension points

An extension point is a place in the cockpit where a workflow module may put a part. The general
functions of a cockpit, like the lists, claiming or the notifications, are built into the cockpit's
frontend and backend. They are no extension points. An extension point gives a module a place for
something only this module has, outside the page of a user task and the page of a case.

Four extension points are the bare minimum, and every generated cockpit has them:

| Exposed name     | What the module exports                                            | Where the cockpit uses it |
|------------------|--------------------------------------------------------------------|---------------------------|
| `./UserTaskList` | `userTaskListColumns`, a function from a user task to its columns  | the task lists |
| `./WorkflowList` | `workflowListColumns`, a function from a case to its columns       | the case list |
| `./UserTaskPage` | `userTaskPage`, a function from a user task to web component names | the page of a user task |
| `./WorkflowPage` | `workflowPage`, a function from a case to web component names      | the page of a case |

An organisation may add more (`Q-EXTENSION-POINTS`). Examples: buttons in the header, menu
entries, pages of a module, and modal dialogs those open, see
[The other extension points](#the-other-extension-points). For the cockpit there is no difference
between the four and the others. They all come from the same organisation's generated user
interface. The cockpit and the modules of one organisation agree on the list, and it holds for
every module. Write it into the types library, see [types.md](types.md#the-extension-points).

An extension point outside the lists, the page of a user task and the page of a case, like the
header, asks every module the user may see. The four minimum ones only ask the module of the row,
the user task or the case. A module that does not expose the name is no error. It is ignored,
without a message to the user. Only a module that exposes the name and fails to load is worth a
note.

The exposed name is the contract, and so are the names of the exports. A module that exposes
`./UserTaskPage` but names its export differently is loaded and shows nothing. Report that in the
console with the module id.

## Which data finds the part

Three fields of each user task and each case from the GUI API say what to load. They have the same
names in both objects:

| Field              | Meaning |
|--------------------|---------|
| `workflowModuleId` | the workflow module the row comes from, and the name of the remote |
| `uiUriType`        | a string agreed between this user interface and the module |
| `uiUri`            | the `uiUriPath` the module reported, unchanged |

First the address, which depends on `uiUri` only:

- A `uiUri` that starts with `http://` or `https://` is a whole address. The user interface opens
  it as it is and loads nothing into its own page. The module has pages of its own.
- Every other `uiUri` is a path of the module. The address is `/wm/<workflowModuleId>/` plus this
  path.

Then the kind of loading, which depends on `uiUriType`. The server stores the value and never
compares it to anything. VanillaBP knows no names for it. The values are a convention between this
user interface and its workflow modules (`Q-UI-URI-TYPE`), and the types jar carries them as
constants, see [project.md](project.md#the-types-jar). The defaults:

- `NATIVE_MF`: the module reports `uiUriPath` as the path of its remote entry, `/remoteEntry.json`.
  The cockpit loads the parts from `/wm/<workflowModuleId>/remoteEntry.json`.
- `EXTERNAL`: the name for a module that has pages of its own and reports a whole address. It is
  only a name. The address decides that a page opens as it is, not this name.

The server does not build the address for you: it carries the value and does not read it.

An unknown `uiUriType` with a path that is no whole address is no error of the server. It is a
module built for another user interface. Show the row anyway, and tell the user that this row
cannot be opened here.

## One registry and one loader

No list, no page and no header loads a module part on its own. One central place does it:

1. The registry. After `GET /app/current-user`, the user interface calls
   `GET /gui/api/v1/workflow-module` once. It lists the modules the user may see. The answer stays in
   memory, for every part of the user interface. A module whose `uri` is empty has no web path and
   nothing to load.
2. The loader. It is the only code that talks to the orchestrator. A component only asks it:
   "give me the exposed part `./UserTaskPage` of the module `taxi-ride`". The loader answers with a
   promise of the module's exports.

What the loader does:

1. It initialises the orchestrator once, in shim mode, so that remotes can be added later:
   `initFederation({}, { ...useShimImportMap({ shimMode: true }) })`. It keeps the functions it
   returns, `initRemoteEntry` and `loadRemoteModule`. Adding remotes after the start only works in
   shim mode.
2. It registers each module of the registry on its own:
   `initRemoteEntry('/wm/<id>/remoteEntry.json', '<id>')`. One at a time and in parallel, so that a
   module that fails to answer does not stop the others. A module without a remote entry (404) has
   no federated parts. The loader remembers that and does not ask again in this session.
3. A row of a module that is not in the registry yet, because the module registered after the page
   loaded, makes the loader call `GET /gui/api/v1/workflow-module/{id}` for that one module. It adds
   the module to the registry and registers it the same way.
4. `loadRemoteModule('<id>', './UserTaskPage')` returns the module's exports. The loader keeps the
   promise per pair of module and exposed name, so a second caller gets the same one.

The orchestrator keeps what it loaded for the life of the page. A module that is deployed again
while a tab is open is loaded fresh with the next reload of the page. Check the options of the
orchestrator version you use (`storage`, `profile.overrideCachedRemotes`) if you want another
behaviour.

## When a part is loaded

The promise of the loader is the signal. Four states look different to the user, and your user
interface tells them apart:

| State         | How you know it                                                                            | What the user sees |
|---------------|--------------------------------------------------------------------------------------------|--------------------|
| loading       | the promise has not settled yet                                                            | a loading sign |
| loaded        | the promise resolved, and the export is there                                              | the part of the module |
| failed        | the promise rejected, the export is missing, or a web component it names is not registered | a note and a button that loads again |
| not federated | `uiUri` is a whole address, or the module has no remote entry                              | what the cockpit knows itself |

A failed load stays failed until the user tries again. Retrying asks the loader again for this pair,
and the loader calls `initRemoteEntry` and `loadRemoteModule` again. If it fails again, offer to
reload the page.

A module may also export `buildVersion` and `buildTimestamp`. The cockpit does not need them to
know that a part is loaded. Show them in a details view, if at all.

## Nothing is shared

The host shares nothing with the modules, and the modules should share nothing with each other.
The only things that cross the line are the types library and DOM events. Every module brings its
own framework in its own version. That is what lets an Angular cockpit load a React form, and a
module upgrade its framework without asking anybody.

It costs little, because every part is loaded once. A module's framework is loaded once per module
and not once per element.

In `federation.config` a module therefore shares no singleton with other remotes. It does not put
anything on `window` that another part reads, and it registers no global CSS.

CSS frameworks are the hard part. A global stylesheet of the cockpit changes the look of a form, and
the stylesheet of a form changes the look of the cockpit. That is why every web component of a
module renders into a shadow DOM.

## A visual part is a web component

The module defines its web components itself, with `customElements.define`, when its exposed module
is evaluated. A function that returns the name of a web component must only return a name that is
registered at that moment. The host checks it with `customElements.get(name)` right after the
function returns. A name that is not registered is a failed load.

The names must not collide across modules. A custom element name is lower case and has a hyphen. By
convention it starts with the module id, for example `taxi-ride-assess-risk-form`.

What a web component does:

- It attaches an open shadow root and renders everything into it, its styles included.
- It reads its input from properties the host sets. On the page of a user task, `userTask`; on the
  page of a case, `workflow`; for every web component, `language` and `currentUser`. The values are
  the plain data of the GUI API, see [types.md](types.md#what-a-part-gets).
- It renders again when the host sets a property again. The host does that when the update stream
  says the user task or the case changed.
- It asks the cockpit for anything through the [event protocol](#the-event-protocol).
- It frees its framework's resources in `disconnectedCallback`.

The theme reaches the shadow DOM as CSS custom properties. The host sets them on its root, for
example `--bc-color-primary` or `--bc-font-family`, and they are inherited into every shadow tree
(`Q-THEME-TOKENS`). Their names are part of the types library.

The types library offers a helper, `defineBcElement`, that builds and registers such a web
component from three plain functions of the module: `mount`, `update` and `unmount`. A React module
writes `mount = (root, props) => createRoot(root).render(<Form {...props} />)`, a Vue module uses
`createApp`, and so on. So no module has to write the shadow root and the protocol by hand.

## The page of a user task or a case

The module does not export a component for these pages. It exports a function:

```ts
userTaskPage: (userTask: UserTask) => PageAreas | undefined;
workflowPage: (workflow: Workflow) => PageAreas | undefined;

type PageAreas = { [area: string]: string };   // area -> name of a web component
```

The function returns `undefined` when it does not know this user task or this case. Otherwise it
returns, per area, the name of the web component that fills it:

```ts
{ body: 'taxi-ride-assess-risk-form',
  information: 'taxi-ride-assess-risk-information',
  actions: 'taxi-ride-assess-risk-actions' }
```

Which areas exist is a convention, not a law (`Q-PAGE-AREAS`). The default convention:

| Area          | Where the cockpit puts it |
|---------------|---------------------------|
| `body`        | the main area; a long form scrolls in it |
| `information` | the top of a column on the right, for example the case number and the customer |
| `actions`     | the bottom of that column, for example "Accept" and "Reject"; it never scrolls away |

A module that wants the whole page for itself returns `body` only. The cockpit then gives `body`
the whole area and draws no column. So each module chooses between the laid-out form and the
all-in-one form, and the cockpit decides where the parts sit. This keeps the usability the same
across modules when several teams build them.

The areas of one page often have to talk to each other. For example, "Complete" in `actions` only
becomes active when the form in `body` has its required fields. For this `BcUserTask` and
`BcWorkflow` have `sendTo(target, message)` and `receiveMessage(handler)`, see
[types.md](types.md#bcusertask-and-bcworkflow). The host carries the message from the web component
of one area to the web component of the area named in `target`, as a DOM event. Nothing else is
shared between the areas.

## The other extension points

Every further extension point follows the same pattern: the module exposes a function, and a
visual one returns names of web components. Proposals for the usual ones:

| Exposed name      | What the module exports                                                          | Where the cockpit uses it |
|-------------------|----------------------------------------------------------------------------------|---------------------------|
| `./HeaderButtons` | `headerButtons: (currentUser) => string[] \| undefined`, names of web components | the application header |
| `./Pages`         | `pages: (currentUser) => Array<{ id, title, element, menu }> \| undefined`       | the router and the menu |

A page of a module has the route `/<workflowModuleId>/<id>`, always. The router of the cockpit
adds a route for every page of every module in the registry. `title` is a `Translatable`, `element`
the name of the web component, and `menu: true` adds a menu entry. The cockpit's own routes come
first, so a module id like `tasks` cannot hide a screen of the cockpit.

A module may open a modal dialog, for example a header button "Start" that shows a start form. The
button asks the cockpit with the action `cockpit.openDialog` and names the web component of the
dialog. The cockpit shows it in a modal layer above everything. The dialog closes itself with
`cockpit.closeDialog` and a result, and the promise of `cockpit.openDialog` resolves with that
result, see [Actions](#actions).

## The event protocol

A part of a module gets functions, for example `claim()` on a user task. The part calls them like
ordinary functions that return a promise. Below that, a wrapper on the module's side turns the call
into a DOM event on the web component. The cockpit runs it and answers with an event on the same
element. The wrapper then resolves or rejects the caller's promise. The helper of the types library
is that wrapper.

### Request

The wrapper dispatches on the web component itself:

```ts
element.dispatchEvent(new CustomEvent('bc-request', {
  bubbles: false,
  composed: false,
  detail: {
    requestId: crypto.randomUUID(),   // the correlation id
    action: 'userTask.claim',
    args: { userTaskId: '4711' },
  },
}));
```

### Answer

The host listens for `bc-request` on every web component it puts into the page. It answers every
request exactly once, on the same element:

```ts
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: true, result },
}));
// or
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: false, error: { code: 'REFUSED', status: 409, message: '...' } },
}));
```

### Messages between areas

`sendTo(target, message)` is the request `page.sendTo` with `target` and `message`. The host
dispatches the message on the web component of the area `target` of the same page:

```ts
targetElement.dispatchEvent(new CustomEvent('bc-message', {
  detail: { from: 'body', message },
}));
```

`receiveMessage(handler)` listens for `bc-message` on its own web component and returns a function
that stops listening. A `target` without a web component on this page is answered with
`NO_TARGET`.

### Error codes

| Code             | Who sets it | Meaning |
|------------------|-------------|---------|
| `UNKNOWN_ACTION` | host        | this cockpit does not know the action; a module may fall back |
| `REFUSED`        | host        | the server refused it; `status` carries the HTTP status |
| `FAILED`         | host        | the request did not get through, for example a network error |
| `NO_TARGET`      | host        | `sendTo` named an area that has no web component on this page |
| `TIMEOUT`        | wrapper     | no answer within the timeout |
| `DISCONNECTED`   | wrapper     | the element was removed from the page before the answer came |

### Timeout

The wrapper waits 30 seconds by default, then rejects with `TIMEOUT`. An answer that comes later is
ignored. `cockpit.openDialog` has no timeout, because it waits for the user. When the element is
disconnected, the wrapper rejects every open request with `DISCONNECTED`.

### Actions

The minimum set, matching the functions in [types.md](types.md#bcusertask-and-bcworkflow):

| Action                        | `args`                                                                    | `result` |
|-------------------------------|---------------------------------------------------------------------------|----------|
| `userTask.open`               | `userTaskId`                                                              | nothing |
| `userTask.navigateToWorkflow` | `userTaskId`                                                              | nothing |
| `userTask.claim`              | `userTaskId`                                                              | nothing |
| `userTask.unclaim`            | `userTaskId`                                                              | nothing |
| `userTask.assign`             | `userTaskId`, `userId`                                                    | nothing |
| `userTask.unassign`           | `userTaskId`, `userId`                                                    | nothing |
| `workflow.navigateToWorkflow` | `workflowId`                                                              | nothing |
| `workflow.getUserTasks`       | `workflowId`, `activeOnly`, `limitListAccordingToCurrentUsersPermissions` | an array of user tasks |
| `page.sendTo`                 | `target`, `message`                                                       | nothing |
| `cockpit.toast`               | `title`, `message`, `status`, `dismissible`                               | nothing |
| `cockpit.openDialog`          | `element`, `data`                                                         | what the dialog closed with |
| `cockpit.closeDialog`         | `result`                                                                  | nothing |

The four claim and assign actions resolve once the server has accepted the change. The server
answers them without the changed user task. The new state reaches the part when the host sets the
`userTask` property again, after the update stream or after the host read the task again.

An organisation's own extension points add their own actions under their own prefix. A new action
is additive. An old cockpit answers `UNKNOWN_ACTION`, and the module decides what to do.

## Cells drawn by a workflow module

This is an open question. It is not required and not generated until a solution exists
(`Q-CELL-RENDERING`).

The wish: a module draws single cells of a list itself, for example a traffic light for a risk
rating, buttons that complete a user task in place, or details loaded on mouse-over. It is a good
feature.

The problem: a list has many cells. If each cell is a web component that starts the module's
framework inside its own shadow root, the framework is instantiated hundreds of times in the
browser's memory, and the list gets slow.

Directions that might work, none of them proven:

- One framework runtime per module and many mounts. The module exports `mount`, `update` and
  `unmount` for a cell as plain functions. The host calls them for each visible cell. The framework
  is loaded once, and each cell is a small root of it, not a new application.
- The window of loaded rows keeps the number of cells small anyway, because only the rows around
  the visible area exist at all, see
  [behaviour.md](behaviour.md#paging-and-the-window-of-loaded-rows).

A separate analysis will work this out. Until then, cells are drawn by the cockpit from the column
types, see [behaviour.md](behaviour.md#the-columns-a-workflow-module-brings).
