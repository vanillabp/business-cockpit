# What the user expects

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Here is what a cockpit user interface does, not what it looks like. The look comes from the
interview. What follows is the behaviour users know and workflow modules rely on. Where a point
depends on an interview answer, the question id is named.

1. [The whole](#the-whole)
2. [Controls from the design library](#controls-from-the-design-library)
3. [The screens](#the-screens)
4. [The header and the menu](#the-header-and-the-menu)
5. [Counts](#counts)
6. [The task lists](#the-task-lists)
7. [How the columns of a list come about](#how-the-columns-of-a-list-come-about)
8. [The columns a workflow module brings](#the-columns-a-workflow-module-brings)
9. [The actions on a user task](#the-actions-on-a-user-task)
10. [Opening and working on a user task](#opening-and-working-on-a-user-task)
11. [The case list](#the-case-list)
12. [The page of a case](#the-page-of-a-case)
13. [Search, filter, sort](#search-filter-sort)
14. [Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows)
15. [Live updates](#live-updates)
16. [Notifications](#notifications)
17. [User settings](#user-settings)
18. [Language and texts](#language-and-texts)
19. [Extension points](#extension-points)
20. [Who sees what](#who-sees-what)

## The whole

The Business Cockpit is the central user interface of a company. On one side, it gives employees
the user tasks that the digitised business processes create for them. On the other side, it lets
them look up the current state of a business process and, where needed, act on it apart from the
tasks.

For the company a business process is usually a business case. That can be an external case, like
"open an account" at a bank, or an internal one, like approving an employee's leave request. So the
user interface rarely says "process". It uses one word for a case, the one from the interview
(`Q-WORDING`), and never mixes it with another. In VanillaBP a workflow is the process instance,
the run of a process for one case. This description says "case".

The general functions of the cockpit are built into its frontend and its backend: the lists,
claiming, the notifications, the language. Extension points are something else. They give a
workflow module a place in the cockpit for a function of its own, apart from the page of a user
task and the page of a case, see [Extension points](#extension-points). Because the cockpit is often
used across the whole company, such a function may have little to do with processes. An example is
a page where a user sets a substitute for an absence.

Many developers find it unusual that the parts of a module are loaded into the cockpit at runtime,
because it is not how web applications are usually built. The dev shell is the answer for the
modules' developers, see [dev-shell.md](dev-shell.md).

## Controls from the design library

The cockpit uses the design library of `Q-DESIGN-LIBRARY` fully, not only for its colours. Every
control is one of its components and has its look. An action is a button, an icon button or a menu
entry, never a plain link: claim, propose, the note, the tools of a list, "back to the list". The
toolbar above a list, the dialogs, the chips of a filter, the inputs and the toasts are its
components too, and the lists have the look of its table. Only what leads to another page is a
link, like the title of a row. A user who knows the library then knows at once what can be clicked.

A list of virtual rows may need its own rows instead of the table component of the library, see
[Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows). It still takes the
look of that table: its header, its lines and its spacing. With a library of styles only, such as
Tailwind, the cockpit builds these controls in one style from its classes, the same way everywhere.

The cockpit has a light look only, unless `Q-THEME` asks for a dark mode. Without a dark mode it
stays light also when the operating system of the user is set to dark. With a dark mode it follows
that setting, and the colours of the rows, the legend and the charts have a dark form each. When
the user may also choose the look in the menu, the choice is a user setting like any other, under
the key `bc:theme`, see [User settings](#user-settings).

### Toasts

A toast is a component of the design library as well. Toasts lie above everything, also above a
modal dialog, and a toast that stays until clicked away can be clicked away while the dialog is
open. A modal dialog often makes the rest of the page unreachable, so put the toasts where the
dialog does not block them, see
[rough-edges.md](rough-edges.md#a-toast-outside-a-modal-dialog-cannot-be-clicked). Every toast is
in the language of the user, see [Language and texts](#language-and-texts).

## The screens

Which screens exist is decided per organisation (`Q-SCREENS`). The minimum is:

|   Screen    |       Path        |                                                 What it shows                                                 |
|-------------|-------------------|---------------------------------------------------------------------------------------------------------------|
| all tasks   | `/tasks`          | all user tasks the user may see                                                                               |
| my tasks    | `/my-tasks`       | the user tasks assigned to the user or naming them as a candidate, see [gui-api.md](gui-api.md#the-endpoints) |
| case list   | `/workflows`      | the cases; there is exactly one case list                                                                     |
| a case      | `/workflows/<id>` | the page from the workflow module                                                                             |
| a user task | `/tasks/<id>`     | the page from the workflow module, in the frame of the cockpit                                                |

The paths are a proposal (`Q-ROUTES`). What matters is that every screen has an address that can be
shared and bookmarked, and that a link into the middle works. With more than one language the paths
are translated by default, see [Translated paths](#translated-paths). The pages of a workflow module
always have `/<workflowModuleId>/<pageId>`, see [Extension points](#extension-points).

The cockpit changes its screen inside the running application. A menu entry, a link, the back and
forward buttons of the browser and every action that leads to another screen use the router of the
framework, which works with the history API of the browser (`history.pushState`). None of them
loads a page. Only a link that opens a new tab, such as the task tab or "open in new tab", loads
the application. Build this with the frame, from its first story on. A menu that reloads the page
hides leaks: the measurement of `SRCH-018` then never sees the list unmounted, because the
browser throws the whole page away (`NAV-034`).

Often there is more:

- A dashboard as the start page (`Q-DASHBOARD`). What it shows is up to the organisation. An example:
  a pie chart of all open user tasks by task type, and a second one of all open cases by kind. A
  click on a slice opens the matching list, narrowed to that type. Each slice is one request to the
  count endpoint, with a filter on the type, see [gui-api.md](gui-api.md#counting).

  Which types there are comes from the data, not from a configuration, and the bundle names no task
  type or case kind of a workflow module (`PKG-009`). The GUI API cannot list the distinct values of
  a field, so the backend the skill creates answers it with an aggregation of MongoDB over every user
  task or case the user may see, ended or not, see [backend.md](backend.md#the-dashboard). A task
  type is the pair of `bpmnProcessId` and `taskDefinition`, with the text of `taskDefinitionTitle`
  as its label; a case kind is the value of the field of `Q-CASE-KIND`, see
  [The fields behind the fixed columns](#the-fields-behind-the-fixed-columns). Then the user
  interface asks the count endpoint for
  the slices, one filter per type with `searchQueries` on these paths. One request takes up to 20
  filters, so the user interface sends the types in groups of 20, all at once, and draws each slice
  when its count arrives. A type no user task or case of the user has reported yet is not on the
  dashboard; it comes with the first one.

  A filter matches its text anywhere in the value. When the id of one type holds the id of another,
  for example `TaxiRide` and `TaxiRideExpress`, the count of the shorter one holds the longer one
  too. The user interface subtracts the counts of the types whose id holds it, and the list a slice
  opens shows both, so say so in the tooltip of such a slice.

  A tile counts only what the count endpoint can narrow to. A filter (`SearchQuery`) matches a text
  in a field or in the full text. It knows no range, so "due before now" cannot be asked for, and a
  tile "overdue" can neither be counted with one request nor open a list narrowed to it. When the
  user wants such a tile, say so in the interview, and offer one of these: a tile which
  opens the list sorted by the due date, so the overdue tasks come first, without a count; a count
  by the dashboard endpoint of the backend, which can ask MongoDB for `dueDate` before now, with the
  list sorted by the due date behind it; or no such tile. The interview decides (`Q-DASHBOARD`). Its
  default is the count by the dashboard endpoint when the skill creates the backend, and the tile
  without a count otherwise.

  The slices take the chart colours of `Q-THEME`, in their order.

  The dashboard follows the update stream like every other count, see [Counts](#counts): an event
  of its kind asks its counts again, at most once per delivery, unless `Q-LIVE-MODE` is
  `refresh-button`. It asks the backend for the types only when it opens, so a new type shows the
  next time the user opens the dashboard.

- Sub-lists (`Q-SUB-LISTS`), narrowed to a role or to some task types. An example: a company has a
  list of committees a user can be a member of. There is one card per committee (that is, per
  role), with the number of tasks the user can see and the number the user has claimed. A user
  without the role sees 0/0. The cards stand on the start page, in a column to the left of the
  list, or in both places, as the interview decided. A click on a card opens the list, narrowed to
  it. Which views exist is always the organisation's choice, because the task query allows many
  kinds of filtering.

- The notification settings (`Q-NOTIFICATIONS`), see [Notifications](#notifications).

- Pages that a workflow module delivers (`Q-EXTENSION-POINTS`).

### Translated paths

With more than one language (`Q-LANGUAGES`), each screen has one path per language by default
(`Q-ROUTES`), for example `/tasks/4711` and `/aufgabe/4711`, or `/workflows/0815` and
`/vorgang/0815`.

- The path of every language works in every language of the user interface. An English user sends
  `/tasks/4711` to a German colleague, and the link opens the task for her.
- After the screen opens, the address changes to the path of the viewer's language, here
  `/aufgabe/4711`. The router replaces the address and adds no entry to the history, so the back
  button does not lead to the foreign path. Query parameters stay as they are.
- When the user switches the language, the address changes to the path of the new language in the
  same way.
- Links the user interface draws, like the title of a row, use the path of the current language.
- The pages of a workflow module keep `/<workflowModuleId>/<pageId>` in every language. Their ids
  come from the module and are not translated.

The interview agrees on the words of the paths, see
[interview.md](interview.md#check-an-answer-before-you-save-it). Propose these, and for another
language words of the same kind. When `Q-WORDING` picks another word for a case, propose paths with
that word, for example `/faelle` and `/fall/<id>` for `Fall`.

|   Screen    |      English      |      German       |
|-------------|-------------------|-------------------|
| all tasks   | `/tasks`          | `/aufgaben`       |
| my tasks    | `/my-tasks`       | `/meine-aufgaben` |
| a user task | `/tasks/<id>`     | `/aufgabe/<id>`   |
| case list   | `/workflows`      | `/vorgaenge`      |
| a case      | `/workflows/<id>` | `/vorgang/<id>`   |
| dashboard   | `/dashboard`      | `/uebersicht`     |

A path has no umlaut, so `ä` becomes `ae`, and so on. The link in a notification mail uses paths
of its own, see [Notifications](#notifications).

## The header and the menu

The application header is the one bar of the cockpit. There is no second bar for a user task,
neither above nor below the form.

At its very left the header shows the title of the application from `GET /app/info`, for example
"Business Cockpit". It is a link to the start page. It stands there in every view, also while a user
task or a case is open.

Next to the title the header has a toggle with three parts: `My tasks`, `All tasks` and `Cases`,
using the word from `Q-WORDING`. Each part shows its count in brackets, for example `My tasks (4)`,
see [Counts](#counts). The toggle opens the list without narrowing.

What else the header shows depends on the view:

- While a user task is open, the header shows the information about the task (`Q-TASK-HEADER-INFO`)
  and its actions (`Q-TASK-HEADER-ACTIONS`), see
  [Opening and working on a user task](#opening-and-working-on-a-user-task).
- While a case is open, the header shows the information about the case
  (`Q-WORKFLOW-HEADER-INFO`) and its actions (`Q-WORKFLOW-HEADER-ACTIONS`), for example the
  note, see [The page of a case](#the-page-of-a-case).
- In every other view it shows what the interview decided (`Q-HEADER`), for example the title, the
  buttons modules add (`Q-EXTENSION-POINTS`) and the refresh button (`Q-LIVE-MODE` is
  `refresh-button`).

The menu opens from a burger icon. At its top it shows the user who is logged in, with a round
badge and the short form of the name. The badge shows the initials, taken from the long form of the
name, or from the user id without one. With pictures of persons (`Q-AVATARS`) it shows the picture
of the user, and the initials are its placeholder, see [Pictures of persons](#pictures-of-persons).

- On mouse-over of the badge: the long form of the name, the user id and the email address.
- On mouse-over of the name: an overview of all rights of the user and where each right comes from,
  see [The rights of the user](#the-rights-of-the-user).

Below the user come the menu entries: the screens of the interview, the notification settings and
the entries of modules. Below the entries comes the language switch (`Q-LANGUAGE-SWITCH`), and at
the end the logout. The logout ends the login at the identity provider as well, not only in the
cockpit. Otherwise the next reload would log the user in again at once, see
[backend.md](backend.md#logging-out-at-the-provider).

### Pictures of persons

The GUI API of the cockpit has no picture of a person. So by default every badge of a person shows
the initials, and that is the whole badge.

With `Q-AVATARS` the application has pictures. The backend of the application serves the picture of
a person by user id, from an endpoint of its own, see
[backend.md](backend.md#the-pictures-of-persons). Its source is the directory of the organisation,
for example Microsoft Entra ID through Microsoft Graph. The backend names the address in
`business-cockpit.additional-properties`, under the key `person-picture-uri`, with the placeholder
`{userId}`, for example `/person-pictures/{userId}`. `GET /app/info` returns it in
`additionalProperties`. The user interface puts the user id into the placeholder, encoded as a part
of a path, see [types.md](types.md#the-picture-of-a-person). Without the key the user interface
shows the initials only and asks for no picture, so a backend without the endpoint still works.

The initials stay the default, and they are the placeholder of the picture:

- The badge shows the initials at once. When the picture has loaded, it takes their place. The
  badge keeps its size and its round shape, and the picture fills it, cut to the circle. So nothing
  moves when the picture comes.
- A person without a picture keeps the initials. The backend answers 404 for them.
- A picture that does not load, for whatever reason, leaves the initials. The user interface shows
  no error and logs nothing for the user. It does not ask for that picture again until the page is
  loaded again.
- The picture is a plain `<img>` with the address above. The browser sends the cookie of the login
  with it, because the address is on the same origin. The picture has an empty `alt`, because the
  name stands next to it or in the details.

The rule holds wherever the user interface shows a person with a badge:

- the badge of the user at the top of the menu,
- the details of a person on mouse-over, for the assignee, a candidate user, and a column of type
  `person`: a larger badge above the long form of the name,
- the hits of the search when the user proposes a task to a person,
- the head of the overview of the rights, see [The rights of the user](#the-rights-of-the-user).

The cells of a list, for example the assignee, stay text, with the short form of the person, as
described in [Fixed columns](#fixed-columns). This is decided: a list with thousands of rows asks
for no picture, and the row height does not change. Only the details on mouse-over load the picture
of that one person.

The parts of workflow modules show no pictures in 1.0. They do not get the address of the
pictures, see [types.md](types.md#the-picture-of-a-person).

The overview of the rights shows the badge of the user at its head, as the top of the menu does.
It opens below the name, aligned to the right edge of the name, whether the badge shows initials or
a picture.

### The rights of the user

The organisation gives a user groups in its identity provider, for example `AccountManagement`. A
workflow module turns groups into more groups and into rights with its group hierarchy
(`GroupHierarchyMapping`). Each entry of the hierarchy says: a member of this group also gets these
groups and rights. The module configures it under
`vanillabp.workflow-modules.<id>.cockpit.group-hierarchy`. A hierarchy that changes while the
application runs comes from a bean of type `WorkflowModuleGroupHierarchy` instead. The module sends the
hierarchy to the cockpit when it registers. Users often do not know why they may do something, or
why not. The overview answers that.

A hierarchy in the configuration of a module looks like this. Each key is a group, and its list
holds the groups and rights a member of that group also gets:

```yaml
vanillabp:
  workflow-modules:
    orders:
      cockpit:
        group-hierarchy:
          AccountManagement:
            - User
            - Orders.Cancel
          User:
            - Orders.View
```

The cockpit gives the data for all modules in one answer, see
[Where the data comes from](#where-the-data-comes-from).

#### How it opens

The overview opens below the user's name when the mouse moves over the name. It closes when the
mouse leaves both the name and the overview. The mouse can move into the overview, so the user can
scroll a long table and select a name to copy it. The overview looks like the details of the
badge, aligned to the right edge of the name. It is at most about two thirds of the window high
and scrolls inside when it is longer.

The keyboard and touch open it as well (`NAV-035`). The name can take the focus. When it gets the
focus, the overview opens, and the focus can move into the overview. `Escape` closes it and puts
the focus back on the name. It also closes when the focus leaves both the name and the overview.
On a touch screen there is no mouse-over: a tap on the name opens the overview, a second tap
closes it, and so does a tap outside the name and the overview.

The data is asked on the first mouse-over, not when the application starts. It is kept until the
page is loaded again. While it loads, the overview shows "Loading …".

#### What it shows

From top to bottom:

1. The badge of the user, as at the top of the menu: the initials, or with `Q-AVATARS` the picture
   with the initials as its placeholder, see [Pictures of persons](#pictures-of-persons).
2. The title "Roles and rights".
3. One sentence: "The organisation gives you roles. Rights decide what you may do in an
   application. Each application defines its own rights and derives them from your roles."
4. For each workflow module with a group hierarchy: the name of the module as a heading, and a
   table with the columns "Name", "Kind" and "Given by".

The rows of the table of one module:

- A group the identity provider gives the user directly, when this module's hierarchy has an entry
  for it: kind "Role", given by "Organisation".
- For every group of the user with an entry in the hierarchy, one row per target of the entry: the
  name of the target, and the group of the entry as "Given by". The kind is "Role" if the target
  has an entry of its own in the hierarchy, so it stands for more. Otherwise the kind is "Right".
- A target the identity provider also gives the user directly is not shown again as a target. It
  has its row with "Organisation" instead, in the table named further below.
- A target that comes from two groups has two rows, one for each group. This is wanted: the user
  sees every way a right comes to them.

Each row shows one step only. A right that comes over several steps shows the group of the last
step, and that group has its own row. So the user follows the chain upwards, row by row. An
example with the hierarchy above, for a user the organisation gives `AccountManagement`:

|       Name        | Kind  |     Given by      |
|-------------------|-------|-------------------|
| AccountManagement | Role  | Organisation      |
| User              | Role  | AccountManagement |
| Orders.Cancel     | Right | AccountManagement |
| Orders.View       | Right | User              |

Roles come first, then rights, and each part is in the alphabetical order of the name. The kind
decides the order, so no naming rule for groups or rights is needed. The
names are ids and are shown as they are, not translated. The title, the sentence, the column titles
and the kinds are translated.

The overview shows only what the user has. Entries of groups the user does not have are left out.
A module without a hierarchy is left out.

Each group of `givenByOrganisation` has its row with "Organisation" in the table of every module
whose hierarchy has an entry for it, that is, whose `entries` hold it as `group`. A group of the
organisation that no module has an entry for stands in a first table, before the tables of the
modules and without a module heading: the same table the overview shows when no module is left
(`NAV-036`). So every role the organisation gives shows at least once, and the user always finds
the row a "Given by" points at.

The roles of the organisation are shown even when no module is left, for example while no
workflow module with a group hierarchy has registered yet. Then `modules` is empty, but
`givenByOrganisation` still lists the groups of the user. The overview shows them in one table
with no module heading, each with kind "Role" and given by "Organisation", in alphabetical order.
The overview does not say "No roles or rights are known" while it knows a role.

The overview shows the one sentence "No roles or rights are known." in two cases only: the request
fails, or the answer has neither a module nor a group in `givenByOrganisation`.

The cockpit merges the hierarchies of all modules, so a group that one module's hierarchy gives
counts in every module. Such a group has its own row only in the table of the module that gives
it. In the other tables it shows only as "Given by".

#### Where the data comes from

One request gives the data for all modules: `GET /gui/api/v1/app/current-user/group-hierarchy`,
from the cockpit's own OpenAPI file, see [gui-api.md](gui-api.md). The cockpit has the
hierarchies of all modules and the groups the user directory gives. The answer looks like this:

```json
{
  "givenByOrganisation": ["AccountManagement"],
  "modules": [
    {
      "workflowModuleId": "orders",
      "entries": [
        {
          "group": "AccountManagement",
          "givenByOrganisation": true,
          "targets": ["Orders.Cancel", "User"]
        },
        {
          "group": "User",
          "givenByOrganisation": false,
          "targets": ["Orders.View"]
        }
      ]
    }
  ]
}
```

- `givenByOrganisation` at the top lists all groups the user directory gives the user, also those
  without an entry in any hierarchy. These are the rows with "Organisation".
- `modules` has one element for each workflow module with a group hierarchy, sorted by
  `workflowModuleId`. A module without a hierarchy is not in the answer. The heading of a table is
  the `workflowModuleId`, because the cockpit knows no other name of a module.
- `entries` holds only the entries of groups the user has, sorted by `group`, with sorted
  `targets`. Each target is a row with the `group` of its entry as "Given by". A module whose
  hierarchy has no entry for the user comes with empty `entries`.
- A target is "Role" if some entry of the answer, in any module, has it as `group`. Otherwise it is
  "Right".
- The answer is only about the user who is logged in. The path names no user.

`GET /gui/api/v1/app/current-user` is no help here. Its `groups` are all groups after the
hierarchies of all modules are applied, and they do not say where a group came from.

The endpoint is `UserRightsApiController` of the module `business-cockpit` in this repository,
and the test `TheGroupHierarchyOfTheUserTest` of the module `container` shows its answers. Use them
to check what the overview must show.

## Counts

A count in brackets stands next to every part of the toggle in the header and next to every
primary filter of a list. Each count is a request to the count endpoint, see
[gui-api.md](gui-api.md#counting).

A count in the toggle uses the default primary filter of the interview (`Q-PRIMARY-FILTERS`,
`Q-WORKFLOW-FILTERS`), not the primary filter the user chose last. The list itself opens with the
one chosen last, see [The status line](#the-status-line). So the count in the toggle always means
the same, for every user and in every browser, and it does not change when the user switches the
primary filter of a list.

All counts are asked again:

- when the application opens,
- when the user opens a list; its own count then comes with the rows,
- when an event of the update stream comes for the kind of the count, `UserTask` or `Workflow`; at
  most once per delivery of the stream,
- when the user comes back to the tab, on `visibilitychange` to `visible`, for example after
  working in another application.

There is one exception: `ClosedTasksOnly`. This count would grow without end and is rarely needed.
It is not shown in brackets. It is asked only when the mouse moves over its button, and shown then.
It is not kept and not updated by the update stream.

## The task lists

Users find their user tasks and complete them in time. Closed tasks are documentation:
the user can see with which input a task was completed back then (`Q-CLOSED-TASKS`).

### Which tasks

A task list shows all user tasks the user may see. Which these are is decided on
the server, roughly "candidate groups plus candidate users plus assignee, minus excluded candidate
users, plus admitted users". The user interface never computes it.

### Claiming

A user takes a task actively (claim) and becomes its assignee. The server does not
check a claim before work on a task. Some organisations want claiming informal, others hard: the
cockpit lays a barrier over the page until the user has claimed the task (`Q-CLAIM`), see
[The barrier](#the-barrier).

### The status line

The status line is the footer of the list. It stands below the rows, does not scroll with them and
is always visible. On its left it has a group of buttons to choose the primary filter. The interview
decides which primary filters are offered and which is the default (`Q-PRIMARY-FILTERS`). Possible
are `OpenTasksWithFollowUp` (all open tasks), `OpenTasksWithoutFollowUp` (open tasks without those
put aside until a follow-up date), `OpenTaskOnlyFollowUp` and `ClosedTasksOnly`. Without the
follow-up feature (`Q-FOLLOW-UP`) the status line offers neither `OpenTasksWithoutFollowUp` nor
`OpenTaskOnlyFollowUp`, and without closed tasks (`Q-CLOSED-TASKS`) it does not offer
`ClosedTasksOnly`. The proposed default is `OpenTasksWithoutFollowUp`, so a task put aside stays
out of sight until its date, see [Follow-up](#follow-up). Without the follow-up feature it is
`OpenTasksWithFollowUp`. The server takes `OpenTasksWithFollowUp` when a request has no mode, but
the user interface always sends one, see
[gui-api.md](gui-api.md#the-primary-filter-which-rows-come-at-all). The buttons show a short,
translated label with the count in brackets, see [Counts](#counts). The interview agrees on the
labels (`Q-PRIMARY-FILTERS`). Propose these, and for another language words of the same kind:

|       Primary filter       |  English  |    German     |
|----------------------------|-----------|---------------|
| `OpenTasksWithoutFollowUp` | Open      | Offen         |
| `OpenTasksWithFollowUp`    | All open  | Alle offenen  |
| `OpenTaskOnlyFollowUp`     | Follow-up | Wiedervorlage |
| `ClosedTasksOnly`          | Closed    | Erledigt      |

The primary filter last chosen is a user setting and is used again next time, in every browser, see
[User settings](#user-settings). Each list keeps its own, under a key of its own, for example
`bc:tasks:primaryFilter`, `bc:my-tasks:primaryFilter` and `bc:workflows:primaryFilter`. A choice
in `All tasks` does not change `My tasks`. The counts in the toggle of the header do not follow it, see
[Counts](#counts). On its right, the status line shows a colour legend for the states
of a row, for example new, changed and removed, and the looks of a completed, a cancelled and a
suspended row, see [How the state of a row looks](#how-the-state-of-a-row-looks).

### A row

Every user task has one row. The columns are there as the task defines them: filled,
sortable and filterable. The list keeps itself up to date, see [Live updates](#live-updates).

The user sees the context of each task, that is, the columns this kind of task needs. So the columns
shown depend on what is in the list. Usually a user does not see all possible columns, because their
area of work limits the kinds of tasks. And predefined lists sometimes show only tasks with the same
or similar columns.

A row whose task the user has not read yet is bold, the whole row. The user can mark tasks as read or
unread, in the list with the tool of the toolbar and on the task page. Opening a task marks it as
read.

### How the state of a row looks

A user task or a case is active, completed, cancelled or suspended. Each state has its own look. It
holds for the rows of the task lists and of the case list, and for the application header while a
task or a case is open:

|   State   | Background |             Text             |
|-----------|------------|------------------------------|
| active    | normal     | normal                       |
| completed | grey       | normal                       |
| cancelled | grey       | normal, title struck through |
| suspended | normal     | lighter                      |

Only the title of a cancelled row is struck through, not the other cells. The header of an open task
or case shows no title, so there nothing is struck through: a cancelled task or case has the grey
background only.

The fields of the GUI API decide the state, see [gui-api.md](gui-api.md#how-a-row-ended) and
[gui-api.md](gui-api.md#suspended-tasks-and-cases). `endedAt` decides first: a row with `endedAt`
is completed or cancelled, as `endReason` says (`COMPLETED` or `CANCELLED`). A task or a case that
was cancelled while it was suspended keeps `suspendedAt`, but it is cancelled and looks cancelled.
Only a row without `endedAt` and with `suspendedAt` is suspended. All other rows are active.

The organisation may want other or more markers (`Q-STATE-MARKERS`). The default is the table above,
with no symbols. One choice is a symbol right before the title: none for an active row, a green
check mark for a completed one, a pause symbol for a suspended one and a prohibition sign for a
cancelled one. The backgrounds and the lighter text stay. With symbols, nothing is struck through,
so the symbol stays easy to see. The header of an open task
or case has no title, so there the symbol stands at the start of the header.

What the page of a task or a case shows is the module's business, whatever the state. The cockpit
handles the row and the page the same way, open or closed, active or suspended. The only things it
adds are the look above and, for a suspended task, the barrier and the disabled actions, see
[Suspended tasks](#suspended-tasks).

### Fixed columns

Some columns every task has (`Q-TASK-COLUMNS`). The organisation picks them and
their order. The proposed default is id, business id (`businessId`, the business id of the case),
title, due (`dueDate`), assignee, candidate users, notes, follow-up (`followUpDate`) and ended
(`endedAt`). Case kind and created (`createdAt`) can be added.
The notes column needs notes (`Q-NOTES`). The follow-up column only shows
for `OpenTasksWithFollowUp` and `OpenTaskOnlyFollowUp`. The ended column only shows for
`ClosedTasksOnly`. Some organisations want fixed columns only and no dynamic ones, because the
cockpit is used in one area where the list holds known values (`Q-MODULE-COLUMNS`).

#### The fields behind the fixed columns

The interview names the columns in the words of the user. Each name stands for a field of the GUI
API, and that field is the `path` of the column, for sorting and for filtering. The word of
`Q-WORDING` changes the heading of a column, never its path: a column "case kind" or "Vorgangsart"
has the path of `Q-CASE-KIND`, never a made-up `caseKind`. Do not make up a path from the name of a
column.
A path the list does not know is answered with 400, see [gui-api.md](gui-api.md#sorting).

| Column in the interview | Path in the task list | Path in the case list |                      What the cell shows                       |
|-------------------------|-----------------------|-----------------------|----------------------------------------------------------------|
| id                      | `id`                  | `id`                  | the id                                                         |
| business id             | `businessId`          | `businessId`          | the business id                                                |
| title                   | `title`               | `title`               | the text of the user's language (`Translatable`)               |
| case kind               | see below             | see below             | see below                                                      |
| case title              | `workflowTitle`       | `title`               | the text of the user's language (`Translatable`)               |
| task kind               | `taskDefinition`      | (none)                | the text of `taskDefinitionTitle`, `taskDefinition` without it |
| created                 | `createdAt`           | `createdAt`           | the date and time                                              |
| due                     | `dueDate`             | (none)                | the date and time                                              |
| follow-up               | `followUpDate`        | (none)                | the date and time                                              |
| ended                   | `endedAt`             | `endedAt`             | the date and time                                              |
| assignee                | `assignee`            | (none)                | the person                                                     |
| candidate users         | `candidateUsers`      | (none)                | the persons                                                    |
| notes                   | `note`                | `note`                | the note                                                       |

The GUI API has no field for the kind of a case. The interview picks one (`Q-CASE-KIND`):

|      `Q-CASE-KIND`      | Path in the task list  | Path in the case list |                What the cell shows                |
|-------------------------|------------------------|-----------------------|---------------------------------------------------|
| `bpmnProcessId`         | `bpmnProcessId`        | `bpmnProcessId`       | the id of the BPMN process                        |
| the case title          | `workflowTitle.<lang>` | `title.<lang>`        | the text of the user's language                   |
| a code, `details.<key>` | `details.<key>`        | `details.<key>`       | the code, an empty cell when the module sent none |

With the case title, the path for sorting and filtering names the language of the user, for
example `workflowTitle.de`, because the field holds a text per language.

The fixed columns behave and look different from the columns filled with a module's data:

- `id`: a checkbox to select the row for bulk actions. The id itself is not shown.
- `title`: the text is a link that opens the full view. A task opens in a tab of its own
  (`Q-OWN-WINDOW`, see
  [The task in a tab of its own](#the-task-in-a-tab-of-its-own-q-own-window)), a case inside the
  application, in the same tab. It is a real `<a>` element, so "open in new tab" from the right
  mouse button works.
- `assignee`: the short form of the person. On mouse-over of the name, the details of the person:
  the long form, the user id and the email address, and with `Q-AVATARS` the badge of the person
  above them, see [Pictures of persons](#pictures-of-persons). On mouse-over of the cell, a checkbox appears
  inside the cell that claims the task. It appears whether or not the details show. If the current
  user has claimed the task, a vertical bar stands before the short form, and the names to the right
  of the bar stay aligned with all other names. In that case the checkbox is checked and gives the
  task back.
- `candidateUsers`: the column is as wide as an icon. Its header is an icon of several people. With
  one person the cell shows an icon of one person, with several the icon of several people. On
  mouse-over a list of all persons appears, each with a bin icon that removes the person. On
  mouse-over of a person, the same details as for the assignee.
- `createdAt`, `dueDate`, `followUpDate`, `endedAt`: the localised date if it is not today, with the
  time without seconds on mouse-over. If the date is today, the time instead of the date, with the
  date on mouse-over.

### Bulk actions

The selected tasks can be claimed, given back, proposed to another person (they become
candidate users), marked as read or unread, put on follow-up, and given a note (`Q-BULK-ACTIONS`).
A selection may hold ended or suspended tasks; what each action does with them is in
[Ended and suspended tasks in a selection](#ended-and-suspended-tasks-in-a-selection).
The selected cases of the case list can be given a note; that is its one bulk action. The selection
is a set of ids. It stays when rows leave the window of loaded rows, see
[Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows). Switching to another
list, for example from `My tasks` to `All tasks`, drops it. The rows of the other list are other
rows, and an action must never reach a task the user no longer sees.

### The tools and the look of a list

The tools of a list stand in a toolbar above the rows, in the task lists and in the case list. They
are the bulk actions and the button that resets all searches and filters. The full-text search
comes after them and takes the rest of the width of the toolbar, see
[Full-text search](#full-text-search).

The tools are always shown. A tool that acts on the selection is disabled while no row is selected,
so the user sees what the list offers before choosing a row. A tool that needs more input shows its
details over the list, for example the search for a person when the user proposes the selected
tasks. The details lie over the rows and do not move them, so the selection stays in sight.

A row has no tools of its own. To act on one task, for example to propose it, to mark it as read
or to put it on follow-up, the user selects its row and takes the tool in the toolbar. Only three
actions also sit in the cells of a row: claim and give back in the assignee cell, removing a
candidate in the candidate users column, and the note in the notes column, see
[Fixed columns](#fixed-columns) and [Notes](#notes). On the open task, the application header
offers its actions, see [The header while a task is open](#the-header-while-a-task-is-open).

The background of every list is white, so the rows read well. Where the design library gives the
page no white background, the list is frosted glass: white and partly transparent, so the
background shows through, and blurred where the browser can, in CSS with `backdrop-filter`.

### When a list does not load

A request of a list can fail: the server does not answer, or it answers with an error. Then the
list shows no rows, and its toolbar and its status line stay. A toast in the language of the user
says that the list could not be loaded, see [Toasts](#toasts). It shows no status code and no text
of the server. The next action that loads the list tries again, for example a click on a primary
filter, a new search or a new sort. This holds for the task lists and the case list alike.

A 401 is not such a failure. It means that the login has ended, and the browser goes to the login,
see [gui-api.md](gui-api.md#staying-logged-in).

### Notes

User tasks and cases can carry notes (`Q-NOTES`). The endpoints are
`PATCH /usertask/{id}/note` and `PATCH /usertask/note` for user tasks, and the same paths below
`/workflow` for cases. A note
can be written in three places:

- in the list, per row, in the notes column,
- in the list, once for all selected rows, as a bulk action,
- on the full view, in the application header, as an input field that is visible at once and not
  hidden behind a button, see [The header while a task is open](#the-header-while-a-task-is-open).

The same holds for the case list and the page of a case.

### Follow-up

A user can put a task on follow-up until a date (`Q-FOLLOW-UP`). It then leaves
`OpenTasksWithoutFollowUp` until the date has come.

The user picks a day, not a time. The task comes back at the start of that day, at 00:00 in the
time zone of the user's browser. So the user interface sends `followUpDate` as that moment, in ISO
8601 with the offset of the time zone, for example `2026-12-01T00:00:00+01:00`.

## How the columns of a list come about

A list shows user tasks of several workflow modules at the same time. A workflow module does not
define columns per module. It exports one function for user tasks and one for cases. The function
takes the object of one user task or one case and returns the columns for it, see
[types.md](types.md#column). The cockpit calls it for every row it loads.

So the columns of a list are:

- the fixed columns of the cockpit (`Q-TASK-COLUMNS`, `Q-WORKFLOW-COLUMNS`), always, whether a module
  defines them or not,
- plus the union of the columns of all rows in the window of loaded rows.

The window is larger than what the user sees, see
[Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows). When the user
scrolls and new pages load, rows of other task kinds may come in, and their columns are added. When
far pages are dropped from the window, the columns only they had go away again. The columns always
fit the rows that are loaded at this moment.

### The first draw

A list that first shows the fixed columns and then adds the module columns flickers, and users
dislike that. So:

1. Ask for the first 20 rows. That is about one screen.
2. Ask the [loader](module-loading.md#one-registry-and-one-loader) for the list part
   (`UserTaskList` or `WorkflowList`) of each module these rows come from. It is a data entry
   without a framework, so this is fast. A part that is already loaded is not loaded again. The
   registry of modules is in memory since the start, so nothing is asked for it here.
3. Call the column function for each of the 20 rows, and build the list with these columns.
4. While the list stands, load the rest of the window, as many rows as are needed to fill the
   visible area plus one page.

The first 20 rows are a good try, no guarantee. A row further down may bring a column the first 20
did not have, and then the header changes after all. Keep the change small: add the column at the
place its `priority` gives it, and do not rebuild the list.

A list often has more columns than the browser window has room for. Then the list scrolls
horizontally, and the header scrolls with it.

A scroll bar must not make the list jump. Many browsers show a scroll bar only when it is needed,
and then it takes room instead of lying over the content. While the list loads, the vertical scroll
bar appears, the list gets about 10 pixels narrower, and a list that reacts to its width draws
itself again. The same happens with the height when the horizontal scroll bar appears. So reserve
the room for both scroll bars always, whether the list scrolls or not. In CSS that is
`scrollbar-gutter: stable` for the vertical bar and `overflow-x: scroll` for the horizontal one. No
change of size that a scroll bar causes may start a new draw of the list.

### The rules of the merge

- The columns are merged by `path`. When rows of different kinds bring the same `path`, the column
  is shown once. Its title and its other fields come from the first row in the window that has it,
  so the choice does not depend on the order in which pages arrived.
- A fixed column stays as the cockpit defines it. A module column with the same `path` does not
  replace it.
- The fixed columns come first, in the order of the interview. The columns of the modules follow
  after the last fixed column, sorted by `priority`, the smallest number first. A column with
  `show: false` is not drawn.
- A row without a value for a column has an empty cell.
- A module whose list part does not load must not keep the whole list empty. Its rows show the fixed
  columns and a note in the cells the module would have filled.

## The columns a workflow module brings

A user sees, in as little space as possible, the information about each task or case
needed to judge the row.

The value of a cell is `column.path`, resolved against the row with dots as separators. So
`details.customer.name` reaches into the reported business data.

Each `type` expects a different structure at `path`, and the cockpit draws it like this:

|   `type`    |                    Value at `path`                    |                                                                                                                 How it is drawn                                                                                                                 |
|-------------|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `value`     | a single value                                        | localised: an integer with thousands separators, a decimal number with the local decimal mark, a boolean as "Yes" or "No" in the user's language. Text left-aligned, everything else right-aligned, decimal numbers aligned at the decimal mark |
| `i18n`      | a map with language codes as keys and texts as values | the text in the language of the user interface, falling back to the default language                                                                                                                                                            |
| `person`    | a `Person`                                            | `displayShort`, else `email`, else `id`. On mouse-over: `display`, an empty line, `id`, `email`; with `Q-AVATARS` the badge of the person above them                                                                                            |
| `date`      | a string `YYYY-MM-DD`                                 | the localised date                                                                                                                                                                                                                              |
| `date-time` | a string in ISO 8601 extended format                  | the localised date, a space and the localised hour and minute. Never a `T` between date and time, as in the ISO value. On mouse-over also the seconds, in the same form                                                                         |
| `time`      | a string `hh:mm[:ss]`                                 | the localised hour and minute. On mouse-over also the seconds, if present                                                                                                                                                                       |

The rule of `date-time` holds wherever the user interface shows a date with a time: in the fixed
columns, in the header and on mouse-over. It shows the localised date, a space and the localised
time, and never the `T` of the ISO 8601 value.

Without `type` the cell is drawn like `value`. An object at the path is shown as a short note that
this column needs a type, not as `[object Object]`.

What is sent to the server as `sort` depends on the `type` of the column. This is one of the least
visible places of the whole contract:

| `type` of the column | What is sent as `sort`  |
|----------------------|-------------------------|
| `i18n`               | `<path>.<language>`     |
| `person`             | `<path>.sort,<path>.id` |
| everything else      | `<path>`                |

The reason for the second row: a person is stored in the document with the fields `id`, `fulltext`
and `sort`, and `sort` is the value to sort by. The application builds it itself, for example from
last name and first name. The general rule, which holds for filtering too: a path reaches into the
document the server stores, not into the JSON you got. For `details.*` both are the same. For
`assignee` they are not: the API returns `display`, `displayShort` and `email`, and the
stored fields are `id`, `fulltext` and `sort`.

Whether a module may draw cells itself is an open question, see
[module-loading.md](module-loading.md#cells-drawn-by-a-workflow-module). It depends on a way to
do it without starting a framework once per cell, and none is known yet. More column
types out of the box, for example a currency, are a wish for later.

## The actions on a user task

For one row or for several selected rows. In a list, each action is a tool of the toolbar and acts on
the selected rows, see [The tools and the look of a list](#the-tools-and-the-look-of-a-list). Only
claim, unclaim, remove candidate and the note also sit in the cells of a row.

|      Action      |                      What it means                      |
|------------------|---------------------------------------------------------|
| Claim            | the user becomes the assignee                           |
| Unclaim          | the assignee is removed                                 |
| Propose          | a person the user searched for becomes a candidate user |
| Remove candidate | a person is removed from the candidate users            |
| Mark as read     | sets `read`, the row is no longer bold                  |
| Mark as unread   | takes `read` back                                       |
| Follow-up        | sets or deletes the follow-up date                      |

Claiming a task that already has another assignee takes it away from a colleague, see
[rough-edges.md](rough-edges.md#claiming-overwrites). So the user interface always asks first, every
time. The question has no checkbox "don't ask again", and no user setting turns it off.

A bulk claim asks once for all selected tasks that colleagues hold, with their number, for example:

- English: "3 of the selected tasks are held by colleagues. Claim them as well and take them away
  from your colleagues?"
- German: "3 der ausgewählten Aufgaben sind Kolleginnen oder Kollegen zugewiesen. Auch diese
  übernehmen und ihnen wegnehmen?"

On yes, the claim goes out for all selected tasks. On no, nothing is sent, and the user can take
the rows of the colleagues out of the selection. When the selection also holds ended or suspended
tasks, the question of
[Ended and suspended tasks in a selection](#ended-and-suspended-tasks-in-a-selection) comes first.

To propose a task, the user searches for persons in a field, and the user interface calls
`POST /user` with a maximum number of hits for that.

What these actions change stays. The report that creates a user task sets assignee, candidate users,
candidate groups and excluded candidate users. After that no report of the workflow module changes
these fields any more. Only claiming, unclaiming, proposing and removing in the cockpit change the
assignee and the candidate users. So a task does not vanish from a user's list because the workflow
module gives it to somebody else. One exception is a task that names nobody and is therefore shown
to everybody: it takes the names from the first later report that has some. The admitted users
(`admittedUsers`) are the other exception: every report may set them.

The update stream takes some time. So show the effect of the user's own action as soon as the server
has accepted it, for example the read marker or the new assignee. The stream confirms it a moment
later. This holds for an action on the open task as well: the list shows it at once, also when the
list is in another tab, see
[What the list shows of the task tab](#what-the-list-shows-of-the-task-tab). A user interface
without live updates (`Q-LIVE-MODE` is `refresh-button`) reads the row again after every action.

Closed tasks: disable claim, unclaim, propose, remove candidate, follow-up and note when `endedAt`
is set. The server refuses them with 409, see [gui-api.md](gui-api.md#ended-tasks). Mark as read and
unread stay enabled. A selection that holds ended tasks is not simply disabled, see
[Ended and suspended tasks in a selection](#ended-and-suspended-tasks-in-a-selection).

### Suspended tasks

A suspended task cannot be worked on until it is activated again. It is suspended while
`suspendedAt` is set and `endedAt` is not, see
[How the state of a row looks](#how-the-state-of-a-row-looks).

The server refuses claim, unclaim, assign (propose) and remove candidate for a suspended task with
409, see [gui-api.md](gui-api.md#suspended-tasks-and-cases). The BPMS refuses to complete the task,
and that is the answer the module's form gets when it sends the form.

The user interface does not wait for these answers. While a task is suspended, it disables claim,
unclaim, propose and remove candidate, in the row and in the header of the open task. Taking a task
that nobody can work on only confuses the colleagues. Follow-up, note, mark as read and mark as
unread stay enabled. They organise the user's own work and do not touch the task in the BPMS.

A bulk action is not simply disabled when the selection holds suspended tasks, see
[Ended and suspended tasks in a selection](#ended-and-suspended-tasks-in-a-selection).

A 409 for a task that was not suspended when the user acted means the task was suspended or ended
in the meantime. Read the task again, redraw the row and tell the user which of the two it was.

On the page of the task, the cockpit lays its barrier over the areas, whatever `Q-CLAIM` says, see
[The barrier](#the-barrier). The barrier says that the task is suspended. So the form cannot be
sent, and the user still reads it.

When the task is activated again, an update event comes, the cockpit reads the task again and
`suspendedAt` is gone. The actions are enabled again, and the barrier goes away, without a reload.
If claiming is hard and the user has not claimed the task, the barrier of the claim stays.

### Ended and suspended tasks in a selection

This section owns the rule for a bulk action whose selection holds ended or suspended tasks. The
server answers a list that holds such a task with 409 and changes none of the tasks, see
[gui-api.md](gui-api.md#ended-tasks) and
[gui-api.md](gui-api.md#suspended-tasks-and-cases). So these tasks must never go out with an action
the server refuses for them. A bulk action is still not simply disabled:

|                   Action                    |    Ended tasks in the selection    | Suspended tasks in the selection |
|---------------------------------------------|------------------------------------|----------------------------------|
| claim, give back, propose, remove candidate | ask first, then leave them out     | ask first, then leave them out   |
| follow-up, note                             | leave them out, without a question | send for all selected tasks      |
| mark as read, mark as unread                | send for all selected tasks        | send for all selected tasks      |

"Ask first" means a question worded for the action and translated, for example "The selection
also holds ended or suspended tasks. Propose all but these?" On yes, the user interface sends the
request without the ended and the suspended tasks. On no, it sends nothing. A task that is ended
and suspended at the same time counts as ended, see
[How the state of a row looks](#how-the-state-of-a-row-looks).

Follow-up and note leave the ended tasks out without a question. An ended task is a record and
takes no note and no follow-up date any more, so the user loses nothing. They go out for the
suspended tasks, because they organise the user's own work. When only ended tasks are selected, the
tools that would send nothing are disabled, as for a single ended task.

## Opening and working on a user task

The user works on the task to complete it in business terms.

The page comes from the workflow module, not from the cockpit. The cockpit puts it into its frame
and shows the general functions in the application header.

### Opening

The title in the list is a link to the task page. A click opens the task at once in a tab of its
own, and the list stays in its tab (`Q-OWN-WINDOW`, by default yes), see
[The task in a tab of its own](#the-task-in-a-tab-of-its-own-q-own-window). Without it, a click
opens the page inside the single-page application, in the same tab. The page of a case always opens
in the same tab. Apart from the task tab, the cockpit never opens a task or a case in a tab of its
own by itself. The title is a real link, so a user who wants another tab can still choose "open in
new tab".

There is one exception. A task whose `uiUriType` is `EXTERNAL` opens its address in a tab of its
own, so the list stays. That is the module's choice, made through the type, not the form of the
address. The address itself is built by the one rule of
[module-loading.md](module-loading.md#which-data-finds-the-part). From a task to its case
always leads to the cockpit's own case page, not to the address of the task. The task says where the
task is shown, and that is not where the case is shown.

### The header while a task is open

The task actions of the cockpit always sit in the application header. There is no header or footer
of the task's own. The header shows information about the task (`Q-TASK-HEADER-INFO`), for example
task kind or case kind. It does not show the task title, because the form shows it, and it does not
repeat what the modules show in the area `information` (`Q-TASK-INFORMATION-AREA`), by default the
business id and the case title. So with the default areas it shows little or nothing.

It offers the task actions the interview chose (`Q-TASK-HEADER-ACTIONS`). The interview holds the
default list, see [interview.md](interview.md#a-user-task-and-a-case). Of claim and give back, the
header shows claim while the user does not hold the task, and give back while the user holds it.
Claiming a task of another user asks first, as in the list. The note is an input field at the top
of the header, visible at once, so the user reads and writes it without a click on a button.
"Mark as unread" is in the default, because an opened task counts as read. For an ended or a
suspended task the header disables the same actions as its row, see
[The actions on a user task](#the-actions-on-a-user-task). In the task tab, the red button "back to
the list" stands in place of the burger menu, see
[The task in a tab of its own](#the-task-in-a-tab-of-its-own-q-own-window).

The header stands in the same place for all kinds of tasks, so it is a visual anchor for the user.
The controls of the actions are always in the same place. Its content can never break it: each
value of the information gets its own share of the width, and each value that is too long for its
share is cut with "..." on its own. One long value never pushes the others out.

The header has the look of the state of the task, the same as its row: grey for an ended task,
lighter text for a suspended one, see
[How the state of a row looks](#how-the-state-of-a-row-looks).

The header belongs to the cockpit, not to the workflow module.

### The task in a tab of its own (`Q-OWN-WINDOW`)

The use case: a customer calls and wants to know about their case while the user is in the middle
of a form. The user must not have to close the form and lose the input.

So a task opens at once in a tab of its own, and the list stays in its tab. The user goes back to
the list, looks the case up, and comes back to the form, which still holds the input. Whether the
browser shows a tab or a window is up to the browser.

A click on the title of a task in a list does this:

1. The list page sets `window.name = 'bc-list'`. It does this right before it opens the task, not
   only when it loads, so the name is there whatever happened to the tab before. The task tab finds
   the list by this name when it has no other way.
2. It calls `window.open(taskUrl)`, without `noopener`. The task tab then has the list as its
   `window.opener`.
3. The title stays a real link. "Open in new tab" with the right mouse button still works, only the
   task tab then has no `window.opener`.

`userTask.open` of a module part opens a task in the same way.

The task tab keeps the application header, because it holds the task actions. Only the burger menu
goes. In its place stands a red icon button "back to the list".

`window.opener` only works while both pages share one browsing context group. `noopener`, a link
with `target="_blank"` and no `rel="opener"`, and a different `Cross-Origin-Opener-Policy` on the two
pages all cut it: the task tab then has no `opener`. So the cockpit sends no `noopener`, and if it
sets a `Cross-Origin-Opener-Policy`, every page of the cockpit sets the same value.

#### Back to the list

The button asks first in a dialog of the cockpit whether the input may be lost, and the user can
stay. It asks only when the current user is the assignee of the task. For any other task the user
could not change anything, so the button goes back at once.

The cockpit does not use `window.confirm` for the question. A browser lets a page bring another
window to the front only for a few seconds after a click, and a user who reads a `window.confirm`
for longer has used that time up. The click on the dialog's confirm button gives a fresh one. So the
steps below run in that click handler, or in the click handler of the button when there is no
question:

1. The cockpit removes its `beforeunload` handler first. Otherwise the browser asks a second time
   when the tab closes itself in step 4.
2. If `window.opener` is not `closed`, has the cockpit's origin and still shows a list
   (`location.pathname`), the cockpit calls `focus()` on it. The path matters: a list tab that went
   to another page of the cockpit is still open, and `focus()` would bring that page to the front
   instead.
3. If there is no usable `window.opener`, for example because the user opened the task with "open in
   new tab", the cockpit asks over a `BroadcastChannel` whether a list tab is alive and waits at
   most 300 ms. A `BroadcastChannel` works without any window object, also after `noopener`. If a
   list answers, `window.open('', 'bc-list')` brings that tab to the front. If none answers, the
   cockpit never calls `window.open`, because the browser would open an empty tab. The task tab
   shows the list itself instead: the list of all tasks, `/tasks` in the path of the user's
   language, with the primary filter the user chose last. The task tab does not know from which
   list the task was opened, so it always takes this one.
4. The task tab closes itself with `window.close()`. A browser closes a tab this way when a script
   opened it, and the click on the title did. A tab the user opened with "open in new tab" may stay
   open. Then the cockpit shows a hint that the list is in another tab or window.

The calls that bring the list to the front are fixed for version 1.0.0 of the checklist; build them
as written. `focus()` on the list and `window.open('', 'bc-list')` were tried by hand and with real
clicks in Chrome, Edge, Chromium and Firefox, and in Safari 26 on macOS. All of them bring the list
to the front, as a tab and as a window, also after the task tab was reloaded. The answer of the list
came within 0 to 13 ms, well inside the time a click gives. Only the WebKit browsers of Linux (GNOME
Web, the Playwright MiniBrowser) leave the list behind. A tab in the background never brings itself
to the front, in no browser, also not when it receives a `postMessage` or a `BroadcastChannel`
message. So the call must come from the tab the user just clicked in.

The code of both pages, in plain JavaScript. `LIST_PATHS`, `warnOnLeave`, `showListHere`,
`showHint` and `readRowAgain` come from your cockpit. Wrap the code in your framework, but keep the
order of the steps. Before the call that brings the list to the front, the code waits for nothing
but the first answer of the list:

```js
// the list page
const channel = new BroadcastChannel('bc-cockpit');
channel.onmessage = (e) => {
  if (e.data?.type === 'list-ping') {
    channel.postMessage({ type: 'list-pong', id: e.data.id, visible: document.visibilityState === 'visible' });
  } else if (e.data?.type === 'task-changed') {
    readRowAgain(e.data.userTaskId);              // the user's own change in the task tab
  }
};

function openTask(event, taskUrl) {              // the click on the title of a task
  event.preventDefault();                         // the title stays a real link
  window.name = 'bc-list';                        // before the task tab exists
  window.open(taskUrl);                           // never 'noopener'
}

// the task page
const channel = new BroadcastChannel('bc-cockpit');

function askList(timeoutMs) {                     // the answer of a list, or null
  return new Promise((resolve) => {
    const id = crypto.randomUUID();
    const on = (e) => {
      if (e.data?.type === 'list-pong' && e.data.id === id) {
        clearTimeout(timer); channel.removeEventListener('message', on); resolve(e.data);
      }
    };
    const timer = setTimeout(() => { channel.removeEventListener('message', on); resolve(null); }, timeoutMs);
    channel.addEventListener('message', on);
    channel.postMessage({ type: 'list-ping', id });
  });
}

function listByReference() {
  try {
    const list = window.opener;
    return list && !list.closed && list.location.origin === location.origin
      && LIST_PATHS.includes(list.location.pathname) ? list : null;
  } catch { return null; }                        // another origin: not usable
}

function mayLoseInput(userTask, currentUser) {    // ask, and warn on closing, only then
  return userTask.assignee === currentUser.id;
}

function tellTheList(userTaskId) {                // after the server accepted an action
  channel.postMessage({ type: 'task-changed', userTaskId });
}

// runs in the click handler of the button, or of the confirm button of the cockpit's own dialog
async function backToTheList() {
  window.removeEventListener('beforeunload', warnOnLeave);
  const ref = listByReference();
  if (ref) {
    ref.focus();
  } else if (await askList(300)) {
    window.open('', 'bc-list');
  } else {
    return showListHere();                        // no list answered: this tab shows the list
  }
  window.close();
  setTimeout(() => { if (!window.closed) showHint(); }, 300);   // "The list is open in another tab or window."
}
```

The end-to-end tests prove what a browser test can see: the task opens in a new tab and the list
stays, the task tab has the list as its `window.opener`, the list answers the question over the
channel, no empty tab opens when no list answers, the dialog of the cockpit comes first for a task
of the user and not for any other, and the task tab closes. A browser test cannot prove that a
window really came to the front, because the window manager decides that. So the test checks the
`visibilityState` the list reports, not the screen.

Not tried yet: Safari 27 and 18, Safari on iOS, Chrome on Android, and the window mode on Windows,
where the window manager may decide differently. The way stays the same there. Where the list stays
behind, the user still finds it in its tab.

#### Closing the task tab

Closing or reloading the task tab warns with `beforeunload`, and the user can stay. Like the
question of the button, it warns only when the current user is the assignee of the task. A browser
shows this question only after the user has interacted with the page, and some browsers (GNOME Web)
never show it. So the dialog of the cockpit stays the real protection for "back to the list", and
`beforeunload` only covers closing the tab.

#### What the list shows of the task tab

What the user changes on the open task shows in the list at once: mark as read, also by opening
the task, mark as unread, the note, the follow-up, claim and unclaim. The list is in another tab and
does not see the click. So when the server has accepted the change, the task tab names the task on
the `BroadcastChannel` `bc-cockpit`, and the list reads that row again. It does not wait for the
update stream, which takes some time and sends no event for read and unread at all. The row shows
the new data without a highlight, because the change is the user's own, see
[Live updates](#live-updates).

### What the page does

1. It loads the user task and marks it as read. If that fails, it shows a note with a button that
   tries again.
2. It runs the three steps of [Ask, load, draw](module-loading.md#ask-load-draw). Ask: it gets the
   data entry `UserTaskForm` of the task's module from the
   [loader](module-loading.md#one-registry-and-one-loader) and calls `userTaskForm` with the task.
   Load: it loads the visual entry the answer names and waits until its web components are
   defined. Draw: it puts the web components into their places and sets their properties, see
   [types.md](types.md#what-a-part-gets).
3. Each step has its own loading state and its own note. The module has no form for this task: the
   page says so. The data entry or the visual entry fails to load: a note with a button that
   tries again. A web component is not defined in time: a note that names the module, the entry and
   the web component.

The page loads the entries only when it is shown. The list loads nothing in advance: not when the
mouse rests on a row, and not when a row gets the focus. The short wait when the page opens is
fine. What was loaded once is used again. The page asks the same
[loader](module-loading.md#one-registry-and-one-loader) every time, so each data entry and each
visual entry of a module is loaded only once for each build of the module. Opening a second task of
the same kind, or the same task again, loads no entry again, and when its web components are
defined already, the page draws them at once. The loader only reads the manifest of the module
again when its last read is older than a minute, to notice a new build. After a new build it loads
the entries of the new build, without a reload of the page, see
[module-loading.md](module-loading.md#a-new-build-of-a-module).

### The areas and the frame

The function returns either one web component or one per area (`Q-TASK-PAGE-AREAS`), see
[module-loading.md](module-loading.md#the-page-of-a-user-task-or-a-case). A string is the
name of one web component, the all-in-one form: it gets the whole area, with no column. An object
names a web component per area, the laid-out form.

Besides the areas there is always the area `all`. It is the one web component which shows the whole
page, and every cockpit knows it. The module kit exports its name as `AREA_ALL` (`'all'`), and the
types library of the cockpit exports it as well, next to the names of this cockpit's areas, for
example `AREA_BODY`, `AREA_INFORMATION` and `AREA_ACTIONS`, see
[types.md](types.md#the-extension-points). A module which wants to run in every cockpit returns an
object with `all` and the areas, for example `{ all, body, information, actions }`. The user task
blueprint of VanillaBP does this with one small function per form that builds all four names.

The cockpit picks what to draw:

1. When the page has no areas, because the answer to `Q-TASK-PAGE-AREAS` was none, the cockpit
   draws the web component of `all`.
2. When the page has areas, the cockpit draws the areas of the object which it knows.
3. When the page has areas, but the object names none of them, the cockpit draws the web component
   of `all` as well. This is how a user task deliberately does not follow the areas: its module
   names only `all`, or returns a string.
4. When there is neither, the module has no form for this cockpit, and the page says so.

The cockpit never draws `all` and the areas together. With the default convention, the areas are:

- `body` is the main area. A long form scrolls in it.
- `information` is the top of a column on the right, with basic information about the task, for
  example the case number and the customer. If it is too small, it scrolls on its own.
- `actions` is the bottom of that column, with the completing actions, usually buttons like
  "Accept" and "Reject". The column fills the whole visible height and does not scroll with a long
  form, so the action buttons are always visible.

The use of the main parts is always the same: functions in the header, information and action
buttons in the column on the right. The cockpit lays them out, so modules of different teams look
alike. The areas talk to each other through `sendTo` and `receiveMessage`, for example so that the
"Complete" button becomes active only when the form has its required fields.

One rule holds for the page of a user task and the page of a case alike: long content scrolls in
its own area, the application header stays in place, and the content of a page never breaks the
frame of the cockpit.

### The barrier

If claiming is hard (`Q-CLAIM`), the cockpit lays a barrier over the areas of the page until the
user has claimed the task. It also lays it over the page of a suspended task, until the task is
activated again, however claiming is set up, see [Suspended tasks](#suspended-tasks). Usually it
is a half-transparent layer. The barrier belongs to the cockpit. How it looks is up to the
organisation, with two rules:

- The content under the layer stays readable. The user wants to see the task before taking it.
- Each area can still be scrolled. A layer inside the scroll container of the area, as high as the
  whole content, does this: the mouse wheel and touch scrolling still move the area, but a click
  does not reach the form.

The layer stops the mouse, not the keyboard. A user can still reach a field with the Tab key. So
while the barrier lies over an area, the cockpit also sets the attribute `inert` on the host
element of the module's web component in it, and removes it when the barrier goes. An element with
`inert` and everything inside it, its shadow root included, takes no focus, no click and no key
press. The scroll container of the area is the cockpit's own box and does not get `inert`, so it
still scrolls.

There is no barrier over the page of a task that has ended (`endedAt`), claimed or not. Nobody can
claim it any more, and claim is disabled, see
[The actions on a user task](#the-actions-on-a-user-task). The form decides itself whether it shows
the ended task read-only, see below.

When the user claims the task, the cockpit sets the `userTask` property of the module's web
components again, with the user as the assignee, see [module-loading.md](module-loading.md#actions).
Then the barrier goes away, without a reload.

Whether the form is read-only is not the cockpit's business. The form, that is the visual entry of
the module, decides it itself from `assignee` of the user task together with the current user (the
properties `userTask` and `currentUser`, see [types.md](types.md#what-a-part-gets)). A form may also
be read-only when `endedAt` or `suspendedAt` is set, or the module may show another presentation
of a closed task instead. The cockpit only passes the objects and the current user. It never makes a form read-only
and never checks that a form is. So, with the new `userTask` after a claim, a form which was
read-only shows itself ready to edit.

### Everything inside the page is the module's

All requests that are needed to show data or to complete the form go through the proxy `/wm/`
directly to the workflow module. The cockpit knows nothing about them. Whether a form saves a draft
automatically, by hand with a save button, or not at all, the form decides.

### Changes by others

When somebody else changes the task while the user has it open, the user is
told right away. For example: "User X has claimed this task. Please talk to them before you save or
complete, to avoid losing data." The message is a sticky toast that the user has to click away.

A change by others is an event of the update stream for the id of the shown task whose
`initiatorKind` is `USER` and whose `initiator` is not the id of the user who is logged in. The
event carries both fields, see [gui-api.md](gui-api.md#the-update-stream). The page decides from the
event alone and does not read the task again for this. Reading again would come too late: a second
change between the event and the answer could have changed the initiator.

- `initiatorKind` is `USER` and `initiator` is the current user: the user's own change, no toast.
- `initiatorKind` is `USER` and `initiator` is another user: the toast comes and names that user.
  The event carries only the user id. So the toast takes the short form of the name
  (`displayShort`) from a person the page already has with that id, the assignee or a candidate
  user of the task, also of the task as it is read again after the event. When none of them has
  that id, the toast shows the user id. It asks no endpoint for the name.
- `initiatorKind` is `SYSTEM` (reported by the workflow module) or `COCKPIT` (a job of the cockpit
  itself): no toast. Nobody else is working on the task.

The page still reads the task again for its content, see [How it works](#how-it-works), whatever the
initiator is.

The same holds for the open page of a case, see [The page of a case](#the-page-of-a-case).

## The case list

Users find cases, get a first idea of their state from the columns, and open the
details page when needed.

Technically the case list works like the task lists. It is the same code, not a copy: one list
component draws the task lists and the case list. It gets the kind of its rows, user tasks or
cases, and the kind names only what differs, see below: the endpoints of the GUI API, the fixed
columns, the primary filters, the actions of a row and the type of the update events. Paging, the
window of loaded rows, the search, the filters, the sorting, the toolbar, the selection, the status
line and the live updates exist once and serve both kinds. So the case list has every feature of
the task lists from its first story on, and a fix in one list reaches the other (`WL-014`). Never
build the case list as a component of its own, not even as a first step with simple paging.

These rules of the task lists hold for it unchanged, and they are written down once, in the
sections named:

- the status line as the footer below the rows, with its buttons of the primary filters, their
  labels and counts, and the legend, see [The status line](#the-status-line) and [Counts](#counts),
- the primary filter last chosen, kept as a user setting, see [User settings](#user-settings),
- the toolbar with the tools, the full-text search and the white background, see
  [The tools and the look of a list](#the-tools-and-the-look-of-a-list),
- the look of a row by its state, see [How the state of a row looks](#how-the-state-of-a-row-looks),
- the columns of the modules and the rules of their merge, see
  [How the columns of a list come about](#how-the-columns-of-a-list-come-about),
- searching, filtering and sorting, see [Search, filter, sort](#search-filter-sort),
- paging, the window of loaded rows and the memory, see
  [Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows),
- the live updates, see [Live updates](#live-updates),
- the selection, and that switching to another list drops it, see [Bulk actions](#bulk-actions).

The differences follow.

### Which cases

The server selects the cases from `accessibleToUsers` and `accessibleToGroups`.
These two fields are not shown as columns.

### Primary filters

The interview decides which are offered (`Q-WORKFLOW-FILTERS`). By default these are `InProgress`
and `Done`, and `All` is possible as well. The default of the cockpit is `InProgress`. A suspended
case has not ended, so it shows in `InProgress`.

Each button shows a short, translated label with its count in brackets. The count of
`ClosedTasksOnly` is the only one asked on mouse-over, see [Counts](#counts), so `Done` and `All`
show their count like `InProgress`. The interview agrees on the labels. Propose these, and for
another language words of the same kind:

| Primary filter |   English   |     German     |
|----------------|-------------|----------------|
| `InProgress`   | In progress | In Bearbeitung |
| `Done`         | Done        | Abgeschlossen  |
| `All`          | All         | Alle           |

The dev shell is different: it always asks with `All`, for user tasks and for cases, see
[dev-shell.md](dev-shell.md#what-it-shows).

### Rows

Every case has one row. Modules define their own columns for the case list, apart from the task
columns. The case list has its own fixed columns (`Q-WORKFLOW-COLUMNS`), by default with the
business id (`businessId`) right after the id. There is no read marker and no read function. A case
list without bulk actions has no selection column. Notes work as in the task list, per row and for
all selected rows, see [Notes](#notes).

The ended column (`endedAt`) shows only for the primary filters that can hold an ended case,
`Done` and `All`, and not for `InProgress`. It is the counterpart of the ended column of a task
list, which shows only for `ClosedTasksOnly`, see [Fixed columns](#fixed-columns).

A case row looks as its state says, the same as a task row, see
[How the state of a row looks](#how-the-state-of-a-row-looks). So in `Done` every row is grey, and a
cancelled case also has its title struck through. A suspended case in `InProgress` has lighter text.

The case list has the same status line as a task list, as a footer below the rows, see
[The status line](#the-status-line). On its right it shows the same legend: the looks of a
completed, a cancelled and a suspended case, and of a new, a changed and a removed row. The tools,
the full-text search in their toolbar and the white background are the same as well, see
[The tools and the look of a list](#the-tools-and-the-look-of-a-list).

Some organisations want fixed columns only, as for the task lists (`Q-MODULE-COLUMNS`).

## The page of a case

The user looks at the details of a case. The workflow module may also offer actions
apart from the regular flow of the process here, for example "cancel case", which the BPMN process
handles in an event-based sub-process.

The page works like the page of a user task, with these differences:

- It opens inside the application, in the same tab, also when a user task opens in a tab of its own
  (`Q-OWN-WINDOW`). There is no red button "back to the list". "Open in new tab" with the right
  mouse button on a link to the case works.
- The application header has a form of its own for a case, like the one for a user task. It shows
  information about the case (`Q-WORKFLOW-HEADER-INFO`), by default nothing. It never shows the
  case title or the business id, because the main area of the page shows them, and the more areas
  the page has, the less goes into the header. It shows the actions on the case
  (`Q-WORKFLOW-HEADER-ACTIONS`), for example the note of the case, as an input field that is visible
  at once. It
  shows no task actions. The same rule holds as for the task: each value has its own share of the
  width, and the controls stay in the same place. There is no header or footer of the case's own.
- The header has the look of the state of the case, the same as its row, see
  [How the state of a row looks](#how-the-state-of-a-row-looks). A suspended case gets no barrier;
  what its page offers is the module's business.

The page asks the loader for `WorkflowPage` and calls `workflowPage` with the case. The function
names the visual entry and one web component for the all-in-one form, or one per area
(`Q-WORKFLOW-PAGE-AREAS`, by default the same areas as on the page of a user task). The page runs
the same three steps, with the
same loading and error states as the task page,
and gives the module `getUserTasks` for the tasks of the case. Long content scrolls in its own area
and never breaks the frame of the cockpit. The areas and `all` are picked as on the page of a user
task, see [The areas and the frame](#the-areas-and-the-frame), with `Q-WORKFLOW-PAGE-AREAS` in
place of `Q-TASK-PAGE-AREAS`. A case changed by others while its page is open shows the same toast,
see [Changes by others](#changes-by-others).

## Search, filter, sort

The user copes well with many rows.

### Full-text search

Both lists have it. It stands in the toolbar above the rows, after the tools, and takes the rest of
the width of the toolbar, see [The tools and the look of a list](#the-tools-and-the-look-of-a-list).
The input has an x button that clears it. The field offers
suggestions: it asks from three characters on, 300 milliseconds after the last key, and shows at most
20 suggestions in the order the server returns them, the most frequent first. A suggestion is taken
with a click, the input with the Enter key. The search ignores upper and lower case. Send the input
as it was typed, without escaping it. The server takes it as plain text, see
[gui-api.md](gui-api.md#filtering-over-reported-business-data).

### A filter per column

For each column the user can set a search term that filters on the server,
and remove it again. Only one filter per path, see
[rough-edges.md](rough-edges.md#two-filters-on-one-path). A button resets all searches.

### Sorting

A click on a column header makes it the sort column. A second click turns the direction,
a third goes back to the default order of the list. The sort mark appears only on a column with
`sortable`.

What a column allows is set per task and case kind, by the column flags `sortable`, `filterable` and
`resizeable`.

A column of a module with `resizeable` can be made wider or narrower: the user drags the right edge
of its header. So can every fixed column, except the two that are as wide as an icon, the checkbox
of `id` and `candidateUsers`. The list keeps the width as a user setting, per list and per path, so
it holds in every browser, see [User settings](#user-settings).

A cell never grows wider than its column. A value longer than the width is cut at the width and
ends with an ellipsis, in CSS with `overflow: hidden`, `white-space: nowrap` and
`text-overflow: ellipsis`. On mouse-over the cell shows the full value, with the `title` attribute
or with the details the cell shows on mouse-over anyway (`COL-023`).

A new sort or a new search loads the list again, with a new start timestamp.

The server can refuse a sort with 400, for example one which names two fields that each hold a
list, see [gui-api.md](gui-api.md#sorting). The list then shows the text of the answer as it is,
in a toast, because the server writes it for the user. The list falls back to its default order,
and no column carries the sort mark.

A wish of customers: sorting by value instead of by what is shown. The values are stored in MongoDB
and sorted there. It would need a small structure with value and text, like `i18n`, and sorting by
the value. Cells drawn by the module would make that easy, because then the path would always be the
value. This is open.

## Paging and the window of loaded rows

A list never holds all its rows. An Angular cockpit of a customer that kept every row ran the
browser out of memory at 10,360 cases, and the page could no longer be used. So:

- The list scrolls virtually: only the rows near the visible area exist in the page. The scroll
  bar is sized from `min(totalElements, Q-MAX-LIST-ENTRIES)` and a fixed row height, see
  [The longest list](#the-longest-list-q-max-list-entries).
- It holds a window of pages (`Q-PAGE-SIZE`). The window covers the visible area plus one page
  ahead above and one below. So the user does not notice that rows load while scrolling, in either
  direction.
- Pages further away are dropped, both their rows and the columns only they had. The size of the
  window therefore depends on the height of the area the list is drawn in.
- Each page is asked with its `pageNumber`, the same filters, the same sort and the same
  `initialTimestamp`. The page number counts only the rows the server delivered, not the rows the
  list keeps on its own (see [Live updates](#live-updates)).
- Live updates only touch the rows in the window. Nothing the user cannot reach by scrolling a
  little is kept or asked again.
- The loading sign shows on the first load, not when loading more.

### Which pages load

A request for a page is an event, not a state. Keep the requests in a queue or a stream of events,
where none of them gets lost. A reactive value that keeps only its latest value, such as a signal
or a field of a store, drops requests that come fast one after the other. An Angular cockpit lost
pages this way, and some rows stayed placeholders for good.

Load what the list really draws. After a scroll, take the first and the last row the virtual
scroller has drawn. Load every page that overlaps this range, from the page of the first row to the
page of the last row, both rounded down, plus the one page ahead above and below. Some scrollers
hand over a range of their own in their lazy-loading event, and it may lie before the visible area
instead of on it. Do not rely on that range alone.

Wait for the scrolling to pause when the drawn range has left the window, after a jump or a drag of
the scroll bar. Start a timer of about 100 milliseconds with each scroll event, and load the range
drawn when it runs out. When the drawn range only moves on next to the window, as it does with the
mouse wheel, load the page ahead at once, so no placeholder shows (`SRCH-016`). The first load, a
new sort and a new search load at once too. Without the pause, the Angular cockpit sent more than
100 requests for one drag to the end of the list. With the pause it sent 5. A browser opens only six
connections per host over HTTP/1.1, so the pages at the end of the drag waited behind all the
others: three to four seconds with one million entries.

Ask for each page once. Keep a set of the pages on their way, each with a key made of page number,
page size, sort, filters and `initialTimestamp`. Remove the key when the answer comes, and also
when the request fails. Do not ask for a page that is on its way or already in the window. You may
cancel an outdated request with an `AbortSignal`, but after the pause that is rarely needed.

An answer starts no other request. A count asked again for every page that arrives multiplies the
load. Counts follow the rules in [Counts](#counts).

### What a page costs

Adding a page to the list costs the same whether the list has 1,000 or 1,000,000 entries. So
nothing as long as the total is copied or walked through for each page: no array with one slot per
entry that is copied when a page comes, no set of known ids built from all entries, no search for
the selection across all entries. Work over the window only. The Angular cockpit copied an array
of one million slots for every page, 4 to 8 MB each time, and its derived values went through all
of them.

### The longest list (`Q-MAX-LIST-ENTRIES`)

A list shows at most `Q-MAX-LIST-ENTRIES` entries, by default 100,000, in its current sort. The
count next to it still shows the real total.

There are two reasons. Browsers limit the height of an element: Chrome and WebKit to about 33.5
million pixels, Firefox to about 17.9 million. With rows 46 pixels high, Firefox stops at about
389,000 rows and Chrome at about 730,000. The scroller does not scale, so the rows below that are
out of reach. And nobody reads through 100,000 entries. The user narrows the list down with search
and filters instead.

- The limit is a named constant of its own, filled from the interview, and not a number hidden in
  the loading code. Divide Firefox's 17.9 million pixels by the row height of the theme. The result
  must be larger than the limit.
- The list is `min(totalElements, Q-MAX-LIST-ENTRIES)` rows long. The page that crosses the limit
  is cut at the limit.
- The count shows `totalElements` or the number of the count endpoint, never the length of the
  list.
- A live update may make a capped list a little longer than the limit. That is fine. The next page
  that loads must not take it for an error and throw the list away.
- A new sort shows other entries, the first ones of the new order. That is correct, and the notice
  says so.
- The test of `SRCH-025` runs in Chromium and in Firefox. So `playwright.config.ts` has a project
  for each browser from the first story of the UI repository on, and both browsers are installed
  with `npx playwright install chromium firefox`. The Firefox project may run only the tests that
  need it, with `grep: /SRCH-025/`. With another tool of `Q-E2E-TOOL`, set up its Firefox runner in
  the same way. Chromium has a stop of its own at the end of a long wheel scroll, see
  [rough-edges.md](rough-edges.md#the-scroll-bar-stops-short-of-the-end-in-chromium).

The cap lives in the browser only, and the server still counts every row. A cap on the server would
make counting cheaper, but then the real total is unknown. That is for later, if counting in the
database gets too slow.

A capped list shows that it is capped:

- After the last entry comes one more row, the notice: "Only the first 100,000 entries in the
  current sort order are listed. Please narrow down the list with more criteria." It spans all
  columns and has the style of the theme for notes. It cannot be selected, and a click or a double
  click on it opens nothing. It is not an entry in the data of the list. The list only gets a flag
  that it is capped, and draws the row itself.
- The same text shows on mouse-over of the count, and of every other count above the limit, for
  example in the toggle of the header.

The numbers in it are formatted for the language, see [Language and texts](#language-and-texts).

### Memory that comes back

A virtual scroller creates and destroys rows all the time. If anything keeps a reference to a
destroyed row, the memory grows with the distance scrolled, not with the size of the window. The
memory test below reads the heap once, at the end, and that does not catch it. So:

- Scrolling, however long and however slowly, makes neither the heap nor the number of DOM nodes
  grow. In the Angular cockpit, scrolling slowly down and up once through 2,000 entries took the
  heap from 16 MB to 393 MB and the DOM from 3,000 to 355,000 nodes. A library component in each
  row added a listener to a global service and never removed it, see
  [rough-edges.md](rough-edges.md#a-component-library-keeps-destroyed-rows).
- Leaving the list frees what it held. A service or store that lives longer than the list empties
  its rows and its load options when the list closes. Opening the list again adds no second
  subscription on top of the first. The base of this check is the memory after the user has left
  the list once, not before the list first opened. The user leaves the list inside the page, by a
  menu entry or a link, never by a reload, see [The screens](#the-screens). The first visit loads
  code that stays on purpose: the parts of the modules, the generated client, the texts. So open
  and leave the list once, measure, then open and leave it again and measure once more
  (`SRCH-018`).
- A row draws as few components as it can. Each component in a row is created and destroyed
  thousands of times while the user scrolls. A tooltip, a checkbox or a skeleton is cheap once and
  expensive ten thousand times. Create such a component when it is needed, for example on
  mouse-over.

Two things are measured, and they pull in opposite directions. The memory must stay low, and the
list must scroll smoothly. A larger window of loaded rows scrolls more smoothly, because the next
rows are already there, but it costs more memory. Find the size of the window that passes both
tests, and do not trade one for the other.

The data for both tests is large and mixed. Report 30,000 user tasks or cases for the three
test modules the TypeScript simulator of the end-to-end tests serves, see
[project.md](project.md#the-three-test-modules). The simulator keeps them in memory. Each module
brings other columns, so the rows differ the way they do in production. Run the tests on the
primary filter for closed tasks as well (`ClosedTasksOnly`). Closed tasks are never removed, so
that is where the most rows pile up.

An end-to-end test proves the memory. It is mandatory:

1. Fill the simulator the test runs against with that data.
2. Open the case list and scroll from the top to the bottom.
3. Force a garbage collection and read the JavaScript heap with Playwright through the Chrome
   DevTools Protocol: a `CDPSession` with `HeapProfiler.collectGarbage` and then
   `Runtime.getHeapUsage`.
4. The used heap is below the limit from the interview (`Q-MEMORY-LIMIT`, 150 MB by default).

The same test runs for the task list, with each primary filter the interview chose, closed tasks
included.

A second end-to-end test proves the scrolling. It is mandatory too:

1. Use the same data and open the list.
2. Scroll the way a user reads: with the mouse wheel, a few rows at a time, for about ten seconds.
   During that time, count the frames that take longer than 50 ms, with a `PerformanceObserver` on
   `long-animation-frame`. At most a few may be that long.
3. While scrolling at that speed, no placeholder row is visible. A placeholder is the empty or grey
   row the list shows instead of data that has not arrived yet.
4. Then drag the scroll bar fast, from the top to the bottom. Placeholders may show now, but each
   one is replaced by its data soon after the scrolling stops.

The CDP calls and `long-animation-frame` exist in Chromium only, so these tests run in Chromium.

### Measuring a list

Every list has a measurement that anyone can run again, a Playwright script in `e2e/` that talks
to the Chrome DevTools Protocol. It goes through these steps:

1. The list has loaded.
2. Scroll slowly to the end and back up.
3. Scroll fast to the end and back up.
4. Jump to 37 %, 71 % and 12 % of the scroll bar.
5. Open another screen and come back to the list, twice. The values after the first time are the
   base of the second time.

Before each value, force a garbage collection with `HeapProfiler.collectGarbage`. Then write down
the heap (`Runtime.getHeapUsage`), the DOM nodes and event listeners (`Memory.getDOMCounters`), the
number of page requests and which pages they asked for, and the number of empty rows right away and
3 seconds later. The checklist items `SRCH-017` to `SRCH-021` read their values from it.

When the memory grows, take a heap snapshot and look for the shortest path that keeps a detached
DOM node alive. That path names the cause.

A large total needs no large backend. Let Playwright answer the list requests with `page.route`:
pass the request on, raise `totalElements` in the answer, for example to 1,000,000, and answer page
`n` with the rows of page `n` modulo the real number of pages. So the test with one million entries
(`SRCH-025`) runs against the 30,000 rows of the simulator.

Unit tests cover the loading and the cap. They control the timers of the test framework, so a
pause of 100 milliseconds takes no real time:

- Several pages asked for fast, one after the other, all load.
- A page that is on its way or in the window is not asked for again.
- Fast scroll events load only the range drawn after the pause.
- Opening the list twice adds no second subscription, and closing it frees its rows.
- A capped list is not longer than the limit, shows the real total, cuts the page that crosses the
  limit and keeps rows a live update added above the limit. Below the limit the count is exact.
- The notice row shows only for a capped list, and only after the last row.

## Live updates

Changes to the data shown become visible soon, without the user doing anything.

### How it looks (`Q-LIVE-MODE` is `live`)

A new task appears in the list, highlighted in one colour. A change shows right away, highlighted
in another colour. A task that ended while the user watched takes the look of its end, see
[How the state of a row looks](#how-the-state-of-a-row-looks): grey, and a cancelled one also has
its title struck through. A task that is suspended while the user watches gets lighter text, and one
activated again gets its normal text back. These markers stay, so a user who comes back to the
screen sees what happened. A button acknowledges the changes. Then the markers go away, and so do the
ended rows a list of open rows still showed.

A change the current user made is not marked as changed. The user expects it, so the row just shows
the new data. Each event of the update stream says who made the change, in `initiator` and
`initiatorKind`, see [gui-api.md](gui-api.md#the-update-stream). A change is the user's own when
`initiatorKind` is `USER` and `initiator` is the id of the current user. Every other change is marked,
also one with `initiatorKind` `SYSTEM` (reported by the workflow module) or `COCKPIT` (a job of the
cockpit itself): the list reads the row again and highlights it in colour like any other change.
Several events for one row can come before the row is read again. The row is marked unless all of
them were the user's own.

### Experiment (`Q-LIVE-MODE` is `auto-acknowledge`)

The marker of a row goes away by itself, for example three seconds after the mouse has left the
row. This is still to be validated. It does not hold for rows that have to leave the list, the
ones marked as removed. They stay until the user opens the list again.

### Refresh button only (`Q-LIVE-MODE` is `refresh-button`)

Some organisations want no live update at all. The header has a refresh button that becomes active
when changes were reported. Nothing else moves.

### The rules

- A list with a primary filter for open rows still contains rows that ended while the user
  watched. Scrolling must still work correctly.
- The counts are updated when something changes, see [Counts](#counts).
- A row never disappears under the user's hands. It is coloured and stays, until the user
  acknowledges, or until its page leaves the window.

### How it works

1. When the list is built, keep the timestamp the server returned. That is the `initialTimestamp`
   from [gui-api.md](gui-api.md#the-timestamp-that-keeps-a-list-stable).
2. Listen on the update stream to the name of the list's kind, `UserTask` or `Workflow`. Take the id
   from each event. Every type leads to the same action, except `RELOAD`, which loads the whole list
   again. It keeps the `initialTimestamp`, the primary filter, the filters and the sorting, so the
   markers and the ended rows stay. Only acknowledging, a new sort, a new search and opening the
   list again start a new timestamp. A list whose first request went out after the stream was open
   skips the first `RELOAD` of that stream, see
   [gui-api.md](gui-api.md#what-a-stream-remembers).
3. Read the rows of the window again. If the window starts at the top of the list, ask with `PUT`
   for as many rows as the window holds, plus a reserve of one page, and leave out the ids of the
   changed rows. They come back in full. If the window starts further down, read its pages again
   with `POST` and the same `initialTimestamp`, see
   [gui-api.md](gui-api.md#the-second-form-of-the-same-query). Either way, send the same filters
   as for the first page, also `workflowModuleIds`, and for cases also `businessIds`. A list that
   shows only some workflow modules does not ask at all for an event whose `workflowModuleId`
   belongs to none of them.
4. Take the order of the answer. A new row stands where the sort of the list puts it, not at the
   top: the server keeps it at that place in every later page too, and a row drawn somewhere else
   would show twice. A changed row whose sorted value changed moves to its new place, with its
   marker.
5. Set the state of each returned row:
   * ended after the start timestamp: `ENDED`,
   * otherwise created after the start timestamp: `NEW`,
   * otherwise changed after the start timestamp, not only by the user's own changes (see
     [How it looks](#how-it-looks-q-live-mode-is-live)): `UPDATED`,
   * otherwise `INITIAL`.
6. A row in the window the server no longer returns stays in place and becomes `REMOVED_FROM_LIST`.
7. The user's selection stays.
8. The counts are asked again, see [Counts](#counts).
9. Acknowledging loads the list again with a new start timestamp. That drops the markers and the
   ended rows a list of open rows still showed.

The states `INITIAL`, `NEW`, `UPDATED`, `ENDED` and `REMOVED_FROM_LIST` each have a look from the
theme (`Q-THEME`), and the legend in the status line explains them. An `ENDED` row has the
look of its end, see [How the state of a row looks](#how-the-state-of-a-row-looks).

An event of the type `FOLLOWUP` is the reason a list must not only ask about rows it knows. The
server sends it when a follow-up date has come. The task was filtered out of the list until then. It
only appears because of this event.

An open task page listens too, and so does an open page of a case. When its task or case changes,
the page reads it again and sets the property of the module's web components again (`userTask` or
`workflow`). From the event alone, it shows the toast described in
[Changes by others](#changes-by-others) when another user made the change.

## Notifications

Users learn about new tasks they could take, even when the cockpit is not open in their
browser.

A menu entry of its own leads to the settings (`Q-NOTIFICATIONS`). The user chooses, for all cases
and per kind of case, whether they want notifications and on which channel. The endpoints are
`GET /notifications/media`, `GET` and `PUT /notifications/config`, `GET /notifications/recipient-config`,
`PUT /notifications/recipient-config/{mediumType}` and `GET /notifications/workflows`.

The screen of the settings has three parts, all from these endpoints:

- The channels. `GET /notifications/media` names them, each with a `type`, for example `email`,
  and a translated `name`. A channel is a medium in the words of the API.
- A table of what the user wants. Its columns are the channels. Its first row is "all cases", the
  flags `globalAllViaMedium`, one per channel. Then comes one row per kind of case. A kind of case
  here is a process of a workflow module, one entry of `GET /notifications/workflows` with
  `workflowModuleId`, `bpmnProcessId` and a translated `workflowTitle`. That list holds the
  processes of which the user can see user tasks. It does not follow `Q-CASE-KIND`. Such a row
  overrides the first row, in `perWorkflow` under the key `<workflowModuleId>#<bpmnProcessId>`: a
  flag per channel in `allViaMedium`, or `none` for no notification at all. A row without an entry
  follows the first row. `PUT /notifications/config` stores the whole table.
- Where each channel reaches the user, for example the address of the mails.
  `GET /notifications/recipient-config` answers one entry per channel with its fields, each with a
  translated `title` and `description` and its `value`.
  `PUT /notifications/recipient-config/{mediumType}` stores the fields of one channel as a map from
  the `type` of a field to its value. It answers 400 with one line of text for a value the channel
  cannot use, like a mail address which is not one mailbox, and the screen shows that text at the
  field.

The names of the channels and of the processes are shown in the language of the user, as every
`Translatable` is, see [Language and texts](#language-and-texts).

A link in a notification mail points to the task page. It must work without a detour over the start
page, also when the user has to log in first. The mail is written by the template of the
`business-cockpit` library, with a path per language: `/aufgabe/<id>` in German and `/task/<id>` in
every other language, and `/aufgaben` or `/tasks` for the task list, unless the backend configures
the paths of `Q-ROUTES`, see [backend.md](backend.md#the-configuration). The backend the skill
creates configures them. The user interface still answers `/task/<id>` and `/aufgabe/<id>`, and
`/tasks` and `/aufgaben`, because an older mail or a backend without the keys uses them: each opens
its screen and replaces the address with the path of the user's language, as a translated path
does, see [Translated paths](#translated-paths).

## User settings

Users expect the cockpit to remember their choices in every browser they use. So every user
setting is stored on the server, in the map `customSettings` of the user. The cockpit has endpoints
that read and write one entry of it, see [gui-api.md](gui-api.md#user-settings).

This holds for every setting, whether the user sets it on purpose or the user interface only
remembers it. Examples:

|                         Setting                         | Explicit or implicit  |
|---------------------------------------------------------|-----------------------|
| the language                                            | explicit, in the menu |
| the primary filter last chosen                          | implicit              |
| the width of a column the user resized                  | implicit              |
| the order or the hidden state of columns, where offered | implicit              |

The generated user interface has one global settings service for this. Every part of it that keeps
a setting uses the service, and nothing else calls the endpoints. The service:

- reads an entry the first time somebody asks for it, and keeps it in a cache in memory,
- answers every later question from the cache,
- writes a change to the server and updates the cache at once, so the page does not wait,
- names its keys with a prefix of the user interface and a fixed scheme, with `:` as the
  separator, for example `bc:tasks:primaryFilter` or `bc:columns:tasks:width:<path>`. The server
  refuses a key with a `.`, so the dots of a path become `:` as well:
  `bc:columns:tasks:width:details:customer:name`,
- keeps implicit settings that change often, like a column width while dragging, until the change
  has stopped for a moment, and writes only then.

Never use `localStorage`, `sessionStorage` or a cookie of your own for a setting. They stay in one
browser, and the user sees another cockpit on the next computer.

## Language and texts

All texts are translated, with a default language chosen in advance (`Q-LANGUAGE-DEFAULT`). The
interview asks whether the user can switch the language (`Q-LANGUAGE-SWITCH`).

The languages on offer come from the backend. `GET /app/info` returns `additionalProperties`, which
the application fills from `business-cockpit.additional-properties`, for example
`languages: en,de` (`Q-LANGUAGES`), see [gui-api.md](gui-api.md#the-endpoints). The switch sits in
the menu, below the menu entries. The choice is a user setting, see [User settings](#user-settings).

The texts are in separate files, one per language. None is written into the code.

The build checks that the language files are complete. Every file must have every key of the file
of the default language. If a key is missing, the build fails and names the file and the key. The
fallback below stays as well, for texts that do not come from these files, like the `i18n` cells of
a module. See [project.md](project.md#the-user-interface-bundle).

The chosen language must reach every place where a text per language is drawn: the column header,
the cell, the title of a row, and every web component of a module, through the property `language`.
This includes notifications. The cockpit's own toasts, for example the one about a task changed by
another user, come from the language files. A module passes the text of `showNotification` in the
language it got, see [types.md](types.md#bcusertask-and-bcworkflow). Every toast is in the
default language, or in the language the user chose.

Decide once what happens when a language is missing, and keep to it everywhere: take the default
language, and if that is missing too, the first language there is. A header, a cell of type `i18n`
and the title of a row behave the same.

Numbers are formatted with `Intl.NumberFormat` for the chosen language: `1.000.000` in German,
`1,000,000` in English. This holds for the counts and for the number in the notice of a capped list,
see [The longest list](#the-longest-list-q-max-list-entries). Keep one formatter per language. A
formatter of the framework that needs locale data registered at the start, or that does not follow
a switch of the language at runtime, is not enough. Angular's `DecimalPipe` is one of them.

The server has the key `business-cockpit.default-locale`. It does not set the language of the user
interface.

## Extension points

Extension points give workflow modules a place for functions only they have, outside the page of a
user task and the page of a case (`Q-EXTENSION-POINTS`). Examples:

- Buttons in the header. A module adds a button, for example "Start", next to the others.
- Menu entries and pages. A module adds a page and an entry in the menu that leads to it. The
  route of such a page is always `/<workflowModuleId>/<pageId>`. The router of the cockpit adds a
  route for every page of every module the user may see. The cockpit's own routes come first.
- Modal dialogs. A part of a module may open a modal dialog, for example the "Start" button in
  the header shows a start form above everything.

The rules:

- An extension point outside the lists and the two pages asks every workflow module the user may
  see. The lists and the two pages only ask the module of the row, the task or the case.
- Which extension points exist is decided per organisation. It is a contract between the cockpit
  and the workflow modules, and it holds for all modules. The types library writes it down, see
  [types.md](types.md#the-extension-points).
- A module that does not fill an extension point causes no error. It is ignored.

How it works is in [module-loading.md](module-loading.md#the-other-extension-points).

## Who sees what

Different groups of users may get different views (`Q-VISIBLE-GROUPS`). How is up to the
organisation. The groups are taken from the first call of `GET /app/current-user`, because the JWT
may carry fewer groups, or none.

This only shapes the user interface. What a user may see and do is decided by the server for every
request.
