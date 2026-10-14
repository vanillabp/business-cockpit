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
1. [Loading a part](#loading-a-part)
1. [When a part is loaded](#when-a-part-is-loaded)
1. [Nothing is shared](#nothing-is-shared)
1. [A visual part is a web component](#a-visual-part-is-a-web-component)
1. [The event protocol](#the-event-protocol)
1. [Cells drawn by a workflow module](#cells-drawn-by-a-workflow-module)

## Native federation

Native federation is module federation built on browser standards: ES modules and import maps. It
does not depend on the UI framework or on the bundler. So the cockpit and each workflow module pick
their own framework, their own version and their own build tool.

Two sides:

- A workflow module builds its parts with `@softarc/native-federation`. It uses an adapter for its
  bundler: `@softarc/native-federation-esbuild` for esbuild (React, Vue, plain JavaScript),
  `@angular-architects/native-federation` for the Angular CLI. Other bundlers implement the adapter
  contract. Check the list of adapters at [native-federation.com](https://native-federation.com)
  before you advise a module. The build writes `remoteEntry.json`, the description of what the
  module exposes.
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

A visual part is a custom element with a shadow DOM. A part without a surface, like the columns of
a list, is a plain JavaScript function.

Nothing is shared between the host and the modules, see [Nothing is shared](#nothing-is-shared).

## Extension points

An extension point is a place in the cockpit where a workflow module may put a part. Four of them
are the bare minimum, and every generated cockpit has them:

| Exposed name     | What the module exports                 | Where the cockpit uses it |
|------------------|-----------------------------------------|---------------------------|
| `./UserTaskForm` | `element`, a custom element constructor | the page of a user task |
| `./WorkflowPage` | `element`, a custom element constructor | the page of a case |
| `./UserTaskList` | `userTaskListColumns`, a function       | the task lists |
| `./WorkflowList` | `workflowListColumns`, a function       | the workflow list |

An organisation may add more (`Q-EXTENSION-POINTS`). Examples: buttons in the header, delivered by
a module; menu entries and the pages behind them, delivered by a module. Such an extension point
looks like the four above: an exposed name, and `element` for a visual part or a function for data.
There is no difference between the four and the others. The cockpit and the modules of one
organisation agree on the list, and it holds for every module. Write it into the types library, see
[types.md](types.md#the-extension-points).

An extension point that is not about one row, like the header, asks every module the user may see.
That list is `GET /gui/api/v1/workflow-module`. A module that does not expose the name is no error.
It is ignored, without a message to the user. Only a module that exposes the name and fails to load
is worth a note.

The exposed name is the contract, and so are the names of the exports. A module that exposes
`./UserTaskForm` but names its export differently is loaded and shows nothing. Report that in the
console with the module id.

## Which data finds the part

Three fields of each user task and each case from the GUI API say what to load. They have the same
names in both objects:

| Field              | Meaning |
|--------------------|---------|
| `workflowModuleId` | the workflow module the row comes from, and the name of the remote |
| `uiUriType`        | a string that says how the user interface of this row is built |
| `uiUri`            | the path the module reported; for `EXTERNAL` the address of the page |

`uiUriType` is read only by the frontend. The server stores it and never compares it to anything.
Only one value is fixed: `EXTERNAL`. Every other value is agreed between the cockpit user interface
and the workflow modules (`Q-UI-URI-TYPE`). For native federation the default is `NATIVE_MF`, and
the workflow modules have to report exactly this value.

For `NATIVE_MF` the module reports `uiUri` as the path of its remote entry, `/remoteEntry.json`.
The address is `/wm/<workflowModuleId>` plus this path. The server does not do this for you: it
carries the value and does not read it.

For `EXTERNAL` there is nothing to load. The address is used as the module reported it, and your user
interface opens it in a window of its own. A prefix in front of it would send the browser to a route
of the cockpit that does not exist. Such a module delivers no columns, no forms and no extensions.

An unknown value is no error of the server. It is a module built for another user interface. Show
the row anyway, and tell the user that this row cannot be opened here.

## Loading a part

1. After `GET /app/current-user`, ask `GET /gui/api/v1/workflow-module`. It lists the modules the
   user may see. A module whose `uri` is empty has no web path and nothing to load.
2. Initialise the orchestrator once, in shim mode, so that remotes can be added later:
   `initFederation({}, { ...useShimImportMap({ shimMode: true }) })`. Keep the functions it
   returns, `initRemoteEntry` and `loadRemoteModule`.
3. Register each module on its own:
   `initRemoteEntry('/wm/<id>/remoteEntry.json', '<id>')`. One at a time and in parallel, so that a
   module that fails to answer does not stop the others. A module without a remote entry (404) has
   no federated parts; remember that, and do not ask again in this session.
4. A module that is not in the list yet, because it registered after the page loaded, is added the
   same way the first time a row of it shows up.
5. `loadRemoteModule('<id>', './UserTaskForm')` returns the module's exports. Read the export the
   [table above](#extension-points) names.
6. A visual part: define its custom element once, under a tag name the host picks, see
   [A visual part is a web component](#a-visual-part-is-a-web-component).

The orchestrator keeps what it loaded for the life of the page. A module that is deployed again
while a tab is open is loaded fresh with the next reload of the page. Check the options of the
orchestrator version you use (`storage`, `profile.overrideCachedRemotes`) if you want another
behaviour.

## When a part is loaded

The promise of `loadRemoteModule` is the signal. Four states look different to the user, and your
user interface tells them apart:

| State         | How you know it                                              | What the user sees |
|---------------|--------------------------------------------------------------|--------------------|
| loading       | the promise has not settled yet                              | a loading sign |
| loaded        | the promise resolved, and the export is there                | the part of the module |
| failed        | the promise rejected, or the export is missing               | a note and a button that loads again |
| not federated | `uiUriType` is `EXTERNAL`, or the module has no remote entry | what the cockpit knows itself |

A failed load stays failed until the user tries again. Retrying calls `initRemoteEntry` and
`loadRemoteModule` again for this pair. If it fails again, offer to reload the page.

A module may also export `buildVersion` and `buildTimestamp`. The cockpit does not need them to
know that a part is loaded. Show them in a details view, if at all.

## Nothing is shared

The host shares nothing with the modules, and the modules should share nothing with each other.
Every module brings its own framework in its own version. That is what lets an Angular cockpit load
a React form, and a module upgrade its framework without asking anybody.

It costs little, because every part is loaded once. A module's framework is loaded once per module
and not once per element.

In `federation.config` a module therefore shares no singleton with other remotes. It does not put
anything on `window` that another part reads, and it registers no global CSS.

CSS frameworks are the hard part. A global stylesheet of the cockpit changes the look of a form, and
the stylesheet of a form changes the look of the cockpit. That is why every visual part renders into
a shadow DOM.

## A visual part is a web component

The module exports `element`, a class that extends `HTMLElement`. The host defines it once:

```ts
const tag = `bc-${slug(workflowModuleId)}-${slug(part)}`;  // for example bc-taxi-ride-user-task-form
if (!customElements.get(tag)) {
  customElements.define(tag, exports.element);
}
```

The host picks the tag name, so two modules never collide. A module never defines its own tag for
an extension point.

What the element does:

- It attaches an open shadow root and renders everything into it, its styles included.
- It reads its input from properties the host sets. For a user task form, `userTask`; for a
  workflow page, `workflow`; for every visual part, `language` and `currentUser`. The values are
  the plain data of the GUI API, see [types.md](types.md#what-a-part-gets).
- It renders again when the host sets a property again. The host does that when the update stream
  says the user task or the case changed.
- It asks the cockpit for anything through the [event protocol](#the-event-protocol).
- It frees its framework's resources in `disconnectedCallback`.

The theme reaches the shadow DOM as CSS custom properties. The host sets them on its root, for
example `--bc-color-primary` or `--bc-font-family`, and they are inherited into every shadow tree
(`Q-THEME-TOKENS`). Their names are part of the types library.

The types library offers a helper, `defineBcElement`, that builds such an element from three plain
functions of the module: `mount`, `update` and `unmount`. A React module writes
`mount = (root, props) => createRoot(root).render(<Form {...props} />)`, a Vue module uses
`createApp`, and so on. So no module has to write the shadow root and the protocol by hand.

## The event protocol

A part of a module gets functions, for example `claim()` on a user task. The part calls them like
ordinary functions that return a promise. Below that, a wrapper on the module's side turns the call
into a DOM event on the custom element. The cockpit runs it and answers with an event on the same
element. The wrapper then resolves or rejects the caller's promise. The helper of the types library
is that wrapper.

### Request

The wrapper dispatches on the custom element itself:

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

The host listens for `bc-request` on every element it creates. It answers every request
exactly once, on the same element:

```ts
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: true, result },
}));
// or
element.dispatchEvent(new CustomEvent('bc-response', {
  detail: { requestId, ok: false, error: { code: 'REFUSED', status: 409, message: '...' } },
}));
```

### Error codes


| Code             | Who sets it | Meaning |
|------------------|-------------|---------|
| `UNKNOWN_ACTION` | host        | this cockpit does not know the action; a module may fall back |
| `REFUSED`        | host        | the server refused it; `status` carries the HTTP status |
| `FAILED`         | host        | the request did not get through, for example a network error |
| `TIMEOUT`        | wrapper     | no answer within the timeout |
| `DISCONNECTED`   | wrapper     | the element was removed from the page before the answer came |

### Timeout

The wrapper waits 30 seconds by default, then rejects with `TIMEOUT`. An answer that
comes later is ignored. When the element is disconnected, the wrapper rejects every open request
with `DISCONNECTED`.

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
| `cockpit.toast`               | `title`, `message`, `status`, `dismissible`                               | nothing |

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
- Virtualised rows, so only the cells on the screen exist at all.

A separate analysis will work this out. Until then, cells are drawn by the cockpit from the column
types, see [behaviour.md](behaviour.md#the-columns-a-workflow-module-brings).
