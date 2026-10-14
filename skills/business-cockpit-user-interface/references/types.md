# The types library

*Part of the description in [`../SKILL.md`](../SKILL.md).*

VanillaBP gives no types for the user interface. The generated user interface publishes them
itself, as `<scope>/bc-types` (`Q-NPM-SCOPE`). A workflow module installs this package to build its
parts. The package says what a part may expect as input and which functions it may call. It does
not depend on module federation and has no dependencies, see
[project.md](project.md#the-types-library).

Everything that crosses the line between the cockpit and a module is in it. Nothing else is.

1. [Data from the GUI API](#data-from-the-gui-api)
1. [Translatable and Person](#translatable-and-person)
1. [Column](#column)
1. [BcUserTask and BcWorkflow](#bcusertask-and-bcworkflow)
1. [What a part gets](#what-a-part-gets)
1. [The event protocol](#the-event-protocol)
1. [The extension points](#the-extension-points)

## Data from the GUI API

`UserTask`, `Workflow`, `Person` and `Group` are the data of the GUI API, as the OpenAPI
description `official-gui-api` defines them. The types library carries them as TypeScript
interfaces, generated from the same file the user interface generates its client from, see
[gui-api.md](gui-api.md#generating-the-client). Generate them; do not copy them by hand.

`uiUriType` is a string there and not a type of its own. Which values exist is agreed between the
cockpit and its modules, and the server does not know them. The library exports the agreed values
as constants: `EXTERNAL` and `NATIVE_MF`, or the value from `Q-UI-URI-TYPE`.

## Translatable and Person

```typescript
/** A text per language. The keys are language codes like 'en' or 'de'. */
export interface Translatable {
  [languageKey: string]: string;
}

export interface Person {
  id: string;
  display?: string;
  displayShort?: string;
  email?: string;
  avatar?: number;
  details?: { [key: string]: unknown };
}
```

`Person` has the same fields as the person of the GUI API. A column of type `person` expects this
shape at its path.

## Column

A workflow module defines the columns of a list with two functions, one for user tasks and one for
cases:

```typescript
export type ColumnsOfUserTaskFunction = (userTask: UserTask) => Column[] | undefined;

export type ColumnsOfWorkflowFunction = (workflow: Workflow) => Column[] | undefined;

export interface Column {
  title: Translatable;
  path: string;      // path of the value within the UserTask or Workflow object
  type?: 'value' | 'i18n' | 'person' | 'date' | 'date-time' | 'time';
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
  resizeable: boolean;
}
```

The functions are called once per row, without waiting. They must be fast and must not fetch
anything. They return `undefined` when the module has no columns for this row.

`path` is the key. It is at the same time

* the path where the value is in the row, resolved with dots as separators,
* the path your user interface sends to the server as `sort`,
* the path it sends as `path` of a `SearchQuery`,
* and the key under which the columns of different workflow modules are merged.

So it must match the document the server stores. `details.customer.name` hits the business data
the workflow module reported. See [gui-api.md](gui-api.md#sorting) for what this means on the server.

`title` is a text per language. `type` decides how the cell is drawn, see
[behaviour.md](behaviour.md#the-columns-a-workflow-module-brings). `priority` orders the columns,
the smallest number first. `width` is a CSS size, and the empty string means "take the rest".
`show` may hide a column. `sortable`, `filterable` and `resizeable` say what the user may do with
the column. The server does not read `filterable`; the user interface does.

## BcUserTask and BcWorkflow

These are the objects a part of a module works with. They are the data from the server plus
functions the cockpit provides. Every function returns a promise, because below it runs the
[event protocol](#the-event-protocol):

```typescript
export interface BcUserTask extends UserTask {
  open: () => Promise<void>;
  navigateToWorkflow: () => Promise<void>;
  assign: (userId: string) => Promise<void>;
  unassign: (userId: string) => Promise<void>;
  claim: () => Promise<void>;
  unclaim: () => Promise<void>;
}

export interface BcWorkflow extends Workflow {
  navigateToWorkflow: () => Promise<void>;
  getUserTasks: BcUserTasksProvider;
}

export type BcUserTasksProvider = (
  activeOnly: boolean,
  limitListAccordingToCurrentUsersPermissions: boolean,
) => Promise<Array<BcUserTask>>;

export interface BcToast {
  title?: string;
  message: string;
  status: 'info' | 'warning' | 'error' | 'success';
  dismissible?: boolean;   // true: stays until the user clicks it away
}

export type ToastFunction = (toast: BcToast) => Promise<void>;
```

Each of these functions is a promise of the cockpit. A form may call `claim` and expect that the
task belongs to the user afterwards and that the list knows it. `getUserTasks` calls
`POST /workflow/{workflowId}/usertasks` and returns the tasks as `BcUserTask` again, with the same
functions.

Build them all. A function that does nothing cannot be told apart from a bug by the author of a
form.

## What a part gets

The host sets plain data as properties of the custom element. The wrapper in the module turns it
into the objects above:

| Part               | Properties the host sets                                                          | What the module's component gets |
|--------------------|-----------------------------------------------------------------------------------|----------------------------------|
| user task form     | `userTask`, `language`, `currentUser`                                             | `BcUserTask`, the language, the user, `toast` |
| workflow page      | `workflow`, `language`, `currentUser`                                             | `BcWorkflow`, the language, the user, `toast` |
| other visual parts | `workflowModule`, `language`, `currentUser`, and what the extension point defines | the same, plus `toast` |

`workflowModule` has the fields `GET /workflow-module` returns: `id`, `version`, `uri`.

`language` is the language the user interface shows, for example `en`. A part follows it when it
changes. `currentUser` is the user from `GET /app/current-user`, with the groups.

## The event protocol

The library holds the protocol of [module-federation.md](module-federation.md#the-event-protocol):

- the event names `bc-request` and `bc-response`, as constants,
- the types of the request and the answer, the action names and the error codes,
- `requestFromCockpit(element, action, args, timeoutMs?)`, which dispatches the request, waits for
  the answer with the same `requestId` and resolves or rejects,
- `toBcUserTask(element, data)` and `toBcWorkflow(element, data)`, which add the functions to the
  plain data,
- `defineBcElement({ mount, update, unmount })`, which builds a custom element with an open shadow
  root, reads the properties, runs the protocol and calls the module's three functions.

The host side of the protocol is in the cockpit user interface, not in the library. A module never
needs it.

## The extension points

The library names every extension point of this cockpit: the four minimum ones and those from
`Q-EXTENSION-POINTS`. For each one it exports the exposed name, the type of the export, and the
properties the host sets. For example:

```typescript
export const USER_TASK_FORM = './UserTaskForm';      // exports: element
export const WORKFLOW_PAGE = './WorkflowPage';       // exports: element
export const USER_TASK_LIST = './UserTaskList';      // exports: userTaskListColumns
export const WORKFLOW_LIST = './WorkflowList';       // exports: workflowListColumns
export const HEADER_BUTTONS = './HeaderButtons';     // an extension point of this organisation
```

It also exports the names of the CSS custom properties of the theme (`Q-THEME-TOKENS`), so a module
can use them inside its shadow DOM.
