# Loading the parts of a workflow module

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The cockpit shows lists. What a user sees after a click on a row comes from the workflow module.
The module builds its parts as plain ES modules, and the cockpit loads them into its own pages at
runtime with `import()`. Nothing else is needed: no import map, no shim and no federation library.

The wiki page
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites)
describes the module's side: what it offers and how it builds. Here is the other side, what the
host does. The host is your user interface.

1. [Self-contained ES modules](#self-contained-es-modules)
1. [The manifest](#the-manifest)
1. [Building the entries](#building-the-entries)
1. [The rules](#the-rules)
1. [Extension points](#extension-points)
1. [Data entries carry no framework](#data-entries-carry-no-framework)
1. [Which data finds the part](#which-data-finds-the-part)
1. [One registry and one loader](#one-registry-and-one-loader)
1. [When a part is loaded](#when-a-part-is-loaded)
1. [Nothing is shared](#nothing-is-shared)
1. [A visual part is a web component](#a-visual-part-is-a-web-component)
1. [The page of a user task or a case](#the-page-of-a-user-task-or-a-case)
1. [The other extension points](#the-other-extension-points)
1. [The event protocol](#the-event-protocol)
1. [Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module)

## Self-contained ES modules

An entry is one part a module offers, for example `UserTaskForm`. Every entry is an ES module that
brings everything it needs. It imports only files of its own module, by relative paths. It has no
bare import like `import 'react'` that the page would have to resolve, and it expects nothing from
the cockpit or from another module.

The browser loads such a file with `import(url)` and caches it by its address. That is all the
cockpit uses. So the cockpit and each workflow module pick their own framework, their own version
and their own build tool. A Vue cockpit shows a React form and an Angular case page on the same
page, and a module upgrades its framework without asking anybody.

The entries of one module may share files. The bundler splits the framework into one chunk, and
every entry of the module imports that chunk. The browser loads the chunk once, because it is the
same address. So a module's framework is loaded once per module, not once per form, per page or per
cell.

The address of an entry lies below `/wm/<workflowModuleId>/`, where the cockpit serves the module.
It is the same origin as the cockpit, so a Content Security Policy needs nothing beyond
`script-src 'self'`.

## The manifest

A module describes its entries in one small file, `bc-manifest.json`, at the root of its web path.
The cockpit reaches it at `/wm/<workflowModuleId>/bc-manifest.json`. The proposal:

```json
{
  "manifestVersion": 1,
  "buildVersion": "1.4.0",
  "buildTimestamp": "2026-10-16T09:30:00Z",
  "entries": {
    "UserTaskList": "entries/user-task-list-3f9a1c2b.js",
    "WorkflowList": "entries/workflow-list-77e0d415.js",
    "UserTaskForm": "entries/user-task-form-8b21de90.js",
    "WorkflowPage": "entries/workflow-page-c40a7f13.js",
    "Header": "entries/header-1d9e5a60.js",
    "HeaderElements": "entries/header-elements-e2b81c4f.js"
  }
}
```

| Field             | Meaning |
|-------------------|---------|
| `manifestVersion` | the version of this format; `1` today; a cockpit refuses a higher one it does not know |
| `buildVersion`    | the version of the module's build, shown in a details view, if at all |
| `buildTimestamp`  | when the module was built, also only for a details view |
| `entries`         | entry name to file; the file is relative to the manifest |

Cache busting works through the file names. The bundler puts a hash of the content into every file
name, so a new build has new addresses, and the browser cannot take an old file for a new one. A
bundler that cannot do that appends `?v=<buildVersion>` to the file in the manifest. The manifest
itself has a fixed name, so it is never cached: the module serves it with `Cache-Control: no-cache`,
and the loader asks for it with `fetch(url, { cache: 'no-cache' })`.

The entry names are the keys. They have no `./` in front. That prefix comes from Webpack's module
federation and means nothing here. The types library names the keys as constants, see
[types.md](types.md#the-extension-points).

## Building the entries

Any bundler that writes ES modules works. Two things the build must do: write one file per entry
with the content hash in the name, and write `bc-manifest.json` from the bundler's own list of
outputs. A short script of ten lines does the second, for every bundler below. Nothing is marked as
external, because the entries share nothing with the page.

| Framework        | Recipe |
|------------------|--------|
| React            | Vite in library mode: `build.lib.entry` is an object of entry name to source file, `formats: ['es']`, `build.manifest: true`. Set `define: { 'process.env.NODE_ENV': '"production"' }`, because library mode leaves it alone, and React reads it. Or esbuild: `--bundle --format=esm --splitting --entry-names=[name]-[hash] --metafile=meta.json`. |
| Vue              | Vite in library mode as for React, with `@vitejs/plugin-vue`. Import the styles of a component with `?inline`, so they go into the shadow root and not into the page. |
| Angular          | Zoneless, see [Nothing is shared](#nothing-is-shared). Vite in library mode with `@analogjs/vite-plugin-angular`, or the Angular CLI with its esbuild `application` builder and one build per visual entry, with `--stats-json` for the list of outputs. |
| Plain JavaScript | esbuild or Rollup with `output.format: 'es'`. This is the recipe for every data entry, whatever the framework of the module's forms. |

The data entries need no framework compiler, see
[Data entries carry no framework](#data-entries-carry-no-framework). So a module can build them with
plain esbuild, apart from its forms, even when its forms are Angular.

The recipes are a starting point. Probe `1500` compares the ways of loading across frameworks and
measures the load time and the size per way, see
[Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module).

## The rules

Every entry is loaded once. A part is the pair of a workflow module and an entry name. The host
keeps one promise per pair and hands out the same one to every caller.

A part never comes from the cockpit's own bundle. The cockpit never contains a user interface part
of a business process, see [`../SKILL.md`](../SKILL.md#what-a-cockpit-user-interface-never-does).

Every entry exports plain JavaScript functions. A function that stands for something visible
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

| Entry          | What the module exports                                            | Kind   | Where the cockpit uses it |
|----------------|--------------------------------------------------------------------|--------|---------------------------|
| `UserTaskList` | `userTaskListColumns`, a function from a user task to its columns  | data   | the task lists |
| `WorkflowList` | `workflowListColumns`, a function from a case to its columns       | data   | the case list |
| `UserTaskForm` | `userTaskForm`, a function from a user task to web component names | visual | the page of a user task |
| `WorkflowPage` | `workflowPage`, a function from a case to web component names      | visual | the page of a case |

An organisation may add more (`Q-EXTENSION-POINTS`). Examples: buttons in the header, menu
entries, pages of a module, and modal dialogs those open, see
[The other extension points](#the-other-extension-points). For the cockpit there is no difference
between the four and the others. They all come from the same organisation's generated user
interface. The cockpit and the modules of one organisation agree on the list, and it holds for
every module. Write it into the types library, see [types.md](types.md#the-extension-points).

An extension point outside the lists, the page of a user task and the page of a case, like the
header, asks every module the user may see. The four minimum ones only ask the module of the row,
the user task or the case. A module whose manifest does not name the entry is no error. It is
ignored, without a message to the user. Only an entry that is named and fails to load is worth a
note.

The entry name is the contract, and so are the names of the exports. A module that names
`UserTaskForm` but calls its export differently is loaded and shows nothing. Report that in the
console with the module id.

## Data entries carry no framework

There are two kinds of entries:

- A data entry tells the cockpit something: the columns of a row, the pages of a module and
  their menu entries, the buttons of the header. `UserTaskList`, `WorkflowList`, `Header` and
  `Pages` are data entries.
- A visual entry registers web components and draws something: `UserTaskForm`, `WorkflowPage`
  and the entries that hold the web components of the header, the pages and the dialogs.

A data entry is minimal JavaScript without a framework. It loads without the framework files of its
module. That matters, because the cockpit loads data entries of many modules at once: the columns
for every module in a list, the header and the pages for every module the user may see. A task list
with rows of ten modules must not load ten frameworks before it draws its first row.

Tree-shaking keeps a data entry small. The bundler drops every import the entry does not use. A
helper that a data entry imports by mistake from a file that also imports React drops out as long
as the helper itself does not need React. A real dependency on the framework stays, and that is
what the check below finds.

The check runs in the build of the module. The types library ships a small script without
dependencies, `bc-check-entries`. It reads `bc-manifest.json` and fails the build when:

- an entry the manifest names has no file,
- a data entry, together with every file it imports, is larger than the limit from the interview
  (`Q-DATA-ENTRY-LIMIT`, by default 10 KB before compression),
- a data entry, or a file it imports, contains code of a framework package, for example `react`,
  `react-dom`, `vue`, `@angular/core` or `zone.js`. The script finds them by the comments and
  markers those packages leave in a bundle, and by the bundler's list of inputs when it is there.

The end-to-end tests of the cockpit check the same from the other side: loading the data entries of
a blueprint module fetches no file of the module's framework.

The types library lists the names of the data entries as `DATA_ENTRIES`, so the script and the
cockpit agree on them.

## Which data finds the part

Three fields of each user task and each case from the GUI API say what to load. They have the same
names in both objects:

| Field              | Meaning |
|--------------------|---------|
| `workflowModuleId` | the workflow module the row comes from |
| `uiUriType`        | a type agreed between this user interface and the module; it decides how the row opens |
| `uiUri`            | the `uiUriPath` the module reported, unchanged |

First the address. It is built from `uiUri` by one rule, see
[gui-api.md](gui-api.md#what-the-cockpit-serves):

- A `uiUri` that starts with `http://` or `https://` is a whole address and stays as it is.
- Every other `uiUri` is a path of the module. The address is `/wm/<workflowModuleId>/` plus this
  path.

Then `uiUriType` decides what happens with the address. Only the type decides, never the form of
the address. A convention like "a whole address opens in a new tab" breaks sooner or later, and a
type is safer. The server stores the value and never compares it to anything. VanillaBP knows no
names for it. The values are a convention between this user interface and its workflow modules
(`Q-UI-URI-TYPE`). The types library lists them, and the types jar carries them as Java constants,
see [project.md](project.md#the-types-jar). The defaults:

- `ES_MODULES`: the address is the manifest of the module. Usually the module reports the path
  `/bc-manifest.json`, so the cockpit loads the parts named in
  `/wm/<workflowModuleId>/bc-manifest.json`.
- `EXTERNAL`: the address is a page of the module's own. The cockpit opens it in a window or tab of
  its own and loads nothing into its own page. The list stays where it is.

An unknown `uiUriType` is no error of the server. It is a module built for another user interface.
Show the row anyway, and tell the user that this row cannot be opened here.

## One registry and one loader

No list, no page and no header loads a module part on its own. One central place does it:

1. The registry. After `GET /app/current-user`, the user interface calls
   `GET /gui/api/v1/workflow-module` once. It lists the modules the user may see. The answer stays in
   memory, for every part of the user interface. A module whose `uri` is empty has no web path and
   nothing to load.
2. The loader. It is the only code that calls `import()` for a module. A component only asks it:
   "give me the entry `UserTaskForm` of the module `taxi-ride`". The loader answers with a promise
   of the entry's exports.

What the loader does:

1. It reads the manifest of each module of the registry on its own:
   `fetch('/wm/<id>/bc-manifest.json', { cache: 'no-cache' })`. All at once and each on its own, so
   that a module that fails to answer does not stop the others. A module without a manifest (404)
   has no parts to load. The loader remembers that and does not ask again in this session.
2. A row of a module that is not in the registry yet, because the module registered after the page
   loaded, makes the loader call `GET /gui/api/v1/workflow-module/{id}` for that one module. It adds
   the module to the registry and reads its manifest the same way.
3. `import(new URL(file, manifestUrl).href)` returns the entry's exports. The loader keeps the
   promise per pair of module and entry name, so a second caller gets the same one.

The address of an `import()` is only known at runtime, so the cockpit's own bundler must leave it
alone. Write the hint of the bundler next to it, `/* @vite-ignore */` for Vite or
`/* webpackIgnore: true */` for Webpack. esbuild and the Angular CLI leave such an import alone by
themselves.

The browser keeps a loaded ES module for the life of the page. A module that is deployed again
while a tab is open is loaded fresh with the next reload of the page, because the new manifest
names new files.

## When a part is loaded

The promise of the loader is the signal. Four states look different to the user, and your user
interface tells them apart:

| State      | How you know it                                                                            | What the user sees |
|------------|--------------------------------------------------------------------------------------------|--------------------|
| loading    | the promise has not settled yet                                                            | a loading sign |
| loaded     | the promise resolved, and the export is there                                              | the part of the module |
| failed     | the promise rejected, the export is missing, or a web component it names is not registered | a note and a button that loads again |
| not loaded | `uiUriType` is `EXTERNAL`, or the module has no manifest                                   | what the cockpit knows itself |

A failed load stays failed until the user tries again. Retrying asks the loader again for this
pair. The loader drops the failed promise, reads the manifest again and calls `import()` again. The
browser does not cache a failed `import()`, so a second try really asks the server. If it fails
again, offer to reload the page.

`buildVersion` and `buildTimestamp` of the manifest are not needed to know that a part is loaded.
Show them in a details view, if at all.

## Nothing is shared

The host shares nothing with the modules, and the modules share nothing with each other. The only
things that cross the line are the types library and DOM events. Every module brings its own
framework in its own version. That is what lets a Vue cockpit load a React form and an Angular case
page, and a module upgrade its framework without asking anybody.

It costs little, because every entry is loaded once, and the entries of one module share their
framework chunk, see [Self-contained ES modules](#self-contained-es-modules).

Shadow DOM isolates the DOM and the styles of a web component, not its JavaScript. Every module
runs in the same `window`, with the same `customElements` registry and the same global objects. So
a module does not put anything on `window` that another part reads, registers no global CSS, and
loads no polyfill that changes a global API.

The most common of these polyfills is `zone.js`, and so Angular modules run zoneless, with
`provideZonelessChangeDetection()`. `zone.js` patches timers, promises and events of the whole
page. Two Angular modules with `zone.js` patch them twice, and the second copy fails or slows every
part down. An Angular cockpit runs zoneless as well. Zoneless change detection is stable since
Angular 20.2. A form that relies on `zone.js` to notice changes uses signals instead, or calls
`markForCheck()`.

CSS frameworks are the hard part. A global stylesheet of the cockpit changes the look of a form, and
the stylesheet of a form changes the look of the cockpit. That is why every web component of a
module renders into a shadow DOM.

## A visual part is a web component

The module defines its web components itself, with `customElements.define`, when its visual entry
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
`createApp`, an Angular module `createApplication` once per module and `createComponent` with
`hostElement` per web component. So no module has to write the shadow root and the protocol by
hand.

## The page of a user task or a case

The module does not export a component for these pages. It exports a function:

```ts
userTaskForm: (userTask: UserTask) => PageParts | undefined;
workflowPage: (workflow: Workflow) => PageParts | undefined;

type PageParts = string | PageAreas;
type PageAreas = { [area: string]: string };   // area -> name of a web component
```

The result has three forms:

- `undefined`: the function does not know this user task or this case. It is not the module's.
- A string: the name of one web component. It is the all-in-one form. The cockpit gives it the
  whole area, draws no column and sets its property `area` to `all`.
- An object: per area, the name of the web component that fills it. It is the laid-out form.

```ts
'taxi-ride-assess-risk-page'

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

An object with `body` only is still the laid-out form: `body` sits in the main area, and the column
stays empty. A module that wants the whole page returns a string. So each module chooses between
the laid-out form and the all-in-one form, and the cockpit decides where the parts sit. This keeps
the usability the same across modules when several teams build them.

The areas of one page often have to talk to each other. For example, "Complete" in `actions` only
becomes active when the form in `body` has its required fields. For this `BcUserTask` and
`BcWorkflow` have `sendTo(target, message)` and `receiveMessage(handler)`, see
[types.md](types.md#bcusertask-and-bcworkflow). The host carries the message from the web component
of one area to the web component of the area named in `target`, as a DOM event. Nothing else is
shared between the areas. An all-in-one form has one web component and needs no messages.

## The other extension points

Every further extension point follows the same pattern: the module names an entry in its manifest,
and the entry exports a function. Header buttons and pages are split in two. A data entry says what
there is, and a visual entry registers the web components. So the cockpit can build the menu and the
routes for every module without loading a single framework. Proposals for the usual ones:

| Entry            | What the module exports                                                           | Kind   | Where the cockpit uses it |
|------------------|-----------------------------------------------------------------------------------|--------|---------------------------|
| `Header`         | `header: (currentUser) => Array<{ element, entry }> \| undefined`                 | data   | the application header |
| `Pages`          | `pages: (currentUser) => Array<{ id, title, element, entry, menu }> \| undefined` | data   | the router and the menu |
| `HeaderElements` | nothing; evaluating it registers the web components of the header buttons         | visual | the application header |
| `PageElements`   | nothing; evaluating it registers the web components of the pages                  | visual | a page of a module |

`element` is the name of a web component, and `entry` names the visual entry of the same manifest
that registers it. The cockpit loads that entry only when the web component is about to be shown:
the header elements when the header draws, the elements of a page when its route opens. A module may
use other names for its visual entries; only `Header` and `Pages` are fixed.

A page of a module has the route `/<workflowModuleId>/<id>`, always. The router of the cockpit
adds a route for every page of every module in the registry. `title` is a `Translatable`, and
`menu: true` adds a menu entry. The cockpit's own routes come
first, so a module id like `tasks` cannot hide a screen of the cockpit.

A module may open a modal dialog, for example a header button "Start" that shows a start form. The
button asks the cockpit with the action `cockpit.openDialog` and names the web component of the
dialog. That web component must be registered already, usually by the same visual entry as the
button. The cockpit shows it in a modal layer above everything. The dialog closes itself with
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

Sending and receiving are both actions, so the host knows who listens.

Sending: `sendTo(target, message)` is the request `page.sendTo` with `target` and `message`.

Receiving: `receiveMessage(handler)` sends the request `page.subscribe`. The host answers with a
`subscriptionId`. From then on the host dispatches every message for this area on the web component
of the area, with this id:

```ts
targetElement.dispatchEvent(new CustomEvent('bc-message', {
  detail: { subscriptionId, from: 'body', message },
}));
```

The wrapper calls the handler for every `bc-message` with its `subscriptionId`. `receiveMessage`
returns a subscription with `unsubscribe()`. That sends `page.unsubscribe` with the id, and the
host stops delivering. When the element leaves the page, the host drops its subscriptions by
itself.

The areas of a page load at the same time, so a message may come before its target has
subscribed. The host keeps such messages, in order, and delivers them once the area subscribes. It
drops them when the page is left. A `target` without a web component on this page is answered with
`NO_TARGET`.

### Notifications

`showNotification` is the request `cockpit.showNotification`. The cockpit shows a toast with the
`text` in the look of the `level`: `info`, `success`, `warning` or `error`. With `sticky: true` the
toast stays until the user clicks it away. Otherwise it goes away by itself after a few seconds.
The text comes in the user's language; the part knows it from the property `language`.

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
| `page.subscribe`              | none; the area is the one of the element                                  | `subscriptionId` |
| `page.unsubscribe`            | `subscriptionId`                                                          | nothing |
| `cockpit.showNotification`    | `level`, `text`, `sticky`                                                 | nothing |
| `cockpit.openDialog`          | `element`, `data`                                                         | what the dialog closed with |
| `cockpit.closeDialog`         | `result`                                                                  | nothing |

The four claim and assign actions resolve once the server has accepted the change. The server
answers them without the changed user task. The new state reaches the part when the host sets the
`userTask` property again, after the update stream or after the host read the task again.

An organisation's own extension points add their own actions under their own prefix. A new action
is additive. An old cockpit answers `UNKNOWN_ACTION`, and the module decides what to do.

## Cells drawn by a workflow module

This is an open question. It is not required and not generated until a solution exists
(`Q-CELL-RENDERING`). Prompt `1500` works it out.

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

Prompt `1500` decides this with a probe. It compares three ways of loading a cell across
frameworks: the way of this description, which shares nothing and loads self-contained ES modules
with `import()`; Module Federation 2.0 with shared dependencies; and native federation. It mixes the
frameworks crosswise, for example a Vue page with cells from a React and an Angular module. It
measures the memory, how fast cells are drawn, how fast they react to the user, how many cells a
list can hold, and how long loading takes. Its result decides whether cells may relax the rule
[Nothing is shared](#nothing-is-shared). Every other part keeps the rule.

Until then, cells are drawn by the cockpit from the column types, see
[behaviour.md](behaviour.md#the-columns-a-workflow-module-brings).
