# What the user expects

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Here is what a cockpit user interface does, not what it looks like. The look comes from the
interview. What follows is the behaviour users know and workflow modules rely on. Where a point
depends on an interview answer, the question id is named.

1. [The whole](#the-whole)
1. [The screens](#the-screens)
1. [The header and the menu](#the-header-and-the-menu)
1. [Counts](#counts)
1. [The task lists](#the-task-lists)
1. [How the columns of a list come about](#how-the-columns-of-a-list-come-about)
1. [The columns a workflow module brings](#the-columns-a-workflow-module-brings)
1. [The actions on a user task](#the-actions-on-a-user-task)
1. [Opening and working on a user task](#opening-and-working-on-a-user-task)
1. [The case list](#the-case-list)
1. [The page of a case](#the-page-of-a-case)
1. [Search, filter, sort](#search-filter-sort)
1. [Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows)
1. [Live updates](#live-updates)
1. [Notifications](#notifications)
1. [User settings](#user-settings)
1. [Language and texts](#language-and-texts)
1. [Extension points](#extension-points)
1. [Who sees what](#who-sees-what)

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

Many developers find module federation unusual, because it is not how web applications are usually
built. The dev shell is the answer for the modules' developers, see [dev-shell.md](dev-shell.md).

## The screens

Which screens exist is decided per organisation (`Q-SCREENS`). The minimum is:

| Screen      | Path              | What it shows |
|-------------|-------------------|---------------|
| all tasks   | `/tasks`          | all user tasks the user may see |
| my tasks    | `/my-tasks`       | the user tasks where the user is the assignee |
| case list   | `/workflows`      | the cases; there is exactly one case list |
| a case      | `/workflows/<id>` | the page from the workflow module |
| a user task | `/tasks/<id>`     | the page from the workflow module, in the frame of the cockpit |

The paths are a proposal (`Q-ROUTES`). What matters is that every screen has an address that can be
shared and bookmarked, and that a link into the middle works. The pages of a workflow module
always have `/<workflowModuleId>/<pageId>`, see [Extension points](#extension-points).

Often there is more:

- A dashboard as the start page (`Q-DASHBOARD`). What it shows is up to the organisation. An example:
  a pie chart of all open user tasks by task type, and a second one of all open cases by kind. A
  click on a slice opens the matching list, narrowed to that type. Each slice is one request to the
  count endpoint, with a filter on the type, see [gui-api.md](gui-api.md#counting).
- Sub-lists (`Q-SUB-LISTS`), narrowed to a role or to some task types. An example: a company has a
  list of committees a user can be a member of. The start page has one card per committee (that is,
  per role), with the number of tasks the user can see and the number the user has claimed. A user
  without the role sees 0/0. Which views exist is always the organisation's choice, because the
  task query allows many kinds of filtering.
- The notification settings (`Q-NOTIFICATIONS`), see [Notifications](#notifications).
- Pages that a workflow module delivers (`Q-EXTENSION-POINTS`).

## The header and the menu

The application header is the one bar of the cockpit. There is no second bar for a user task,
neither above nor below the form.

On its left the header has a toggle with three parts: `My tasks`, `All tasks` and `Cases`, using
the word from `Q-WORDING`. Each part shows its count in brackets, for example `My tasks (4)`, see
[Counts](#counts). The toggle opens the list without narrowing.

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

The menu opens from a burger icon. At its top it shows the user who is logged in, with the avatar
and the short form of the name.

- On mouse-over of the avatar: the long form of the name, the user id and the email address.
- On mouse-over of the name: an overview of all rights of the user and where each right comes from,
  based on the group hierarchy mapping (`GroupHierarchyMapping`). **Placeholder:** a later
  version of this skill specifies this part. Until then, generate an empty slot for it and no
  overview.

Below the user come the menu entries: the screens of the interview, the notification settings and
the entries of modules. Below the entries comes the language switch (`Q-LANGUAGE-SWITCH`), and at
the end the logout.

## Counts

A count in brackets stands next to every part of the toggle in the header and next to every
primary filter of a list. Each count is a request to the count endpoint, see
[gui-api.md](gui-api.md#counting). A count in the toggle uses the primary filter the list would open
with.

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
form can only be edited after the user has claimed the task (`Q-CLAIM`).

### The status line

On the left, the status line has a group of buttons to choose the primary filter. The interview
decides which primary filters are offered and which is the default (`Q-PRIMARY-FILTERS`). Possible
are `OpenTasksWithFollowUp` (all open tasks), `OpenTasksWithoutFollowUp`, `OpenTaskOnlyFollowUp` and
`ClosedTasksOnly`. The buttons show a short, translated label, for example "Open" or "Closed", with
the count in brackets, see [Counts](#counts). The primary filter last chosen is a user setting and
is used again next time, in every browser, see [User settings](#user-settings). On the right, the
status line shows a colour legend for the states of a row, for example new, changed, removed and
inactive.

### A row

Every user task has one row. The columns are there as the task defines them: filled,
sortable and filterable. The list keeps itself up to date, see [Live updates](#live-updates).

The user sees the context of each task, that is, the columns this kind of task needs. So the columns
shown depend on what is in the list. Usually a user does not see all possible columns, because their
area of work limits the kinds of tasks. And predefined lists sometimes show only tasks with the same
or similar columns.

A row whose task the user has not read yet is bold, the whole row. The user can mark tasks as read or
unread, in the list and on the task page. Opening a task marks it as read.

### Closed and cancelled rows

In `ClosedTasksOnly` a completed task looks like any other row, without grey. Only a cancelled task
looks inactive: grey, with its title struck through. One style stands for "inactive", so there is no
second colour for a second kind of inactive. `endReason` tells the two apart: `COMPLETED` or
`CANCELLED`, see [gui-api.md](gui-api.md#how-a-row-ended).

What the page of a task or a case shows is the module's business, whatever the status. The cockpit
only handles the row and the page the same way, open or closed, active or inactive.

### Fixed columns

Some columns every task has (`Q-TASK-COLUMNS`). The organisation picks them and
their order, for example id, case kind, title, assignee, candidate users, created (`createdAt`), due
(`dueDate`), follow-up (`followUpDate`), ended (`endedAt`), notes. The follow-up column only shows
for `OpenTasksWithFollowUp` and `OpenTaskOnlyFollowUp`. The ended column only shows for
`ClosedTasksOnly`. Some organisations want fixed columns only and no dynamic ones, because the
cockpit is used in one area where the list holds known values (`Q-MODULE-COLUMNS`).

The fixed columns behave and look different from the columns filled with a module's data:

- `id`: a checkbox to select the row for bulk actions. The id itself is not shown.
- `title`: the text is a link that opens the full view. It is a real `<a>` element, so "open in new
  tab" from the right mouse button works.
- `assignee`: the short form of the person. On mouse-over of the name, the details of the person:
  the long form, the user id and the email address. On mouse-over of the cell, a checkbox appears
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

The selected tasks can be claimed, proposed to another person (they become
candidate users), marked as read or unread, put on follow-up, and given a note (`Q-BULK-ACTIONS`). The selection
is a set of ids. It stays when rows leave the window of loaded rows, see
[Paging and the window of loaded rows](#paging-and-the-window-of-loaded-rows).

### Notes

User tasks and cases can carry notes (`Q-NOTES`). The endpoints come with prompt `1507`. A note
can be written in three places:

- in the list, per row, in the notes column,
- in the list, once for all selected rows, as a bulk action,
- on the full view, in the application header, see
  [The header while a task is open](#the-header-while-a-task-is-open).

The same holds for the case list and the page of a case.

### Follow-up

A user can put a task on follow-up until a date (`Q-FOLLOW-UP`). It then leaves
`OpenTasksWithoutFollowUp` until the date has come.

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
2. Ask the [loader](module-federation.md#one-registry-and-one-loader) for the list part
   (`UserTaskList` or `WorkflowList`) of each module these rows come from. A part that is
   already loaded is not loaded again. The registry of modules is in memory since the start, so
   nothing is asked for it here.
3. Call the column function for each of the 20 rows, and build the list with these columns.
4. While the list stands, load the rest of the window, as many rows as are needed to fill the
   visible area plus one page.

The first 20 rows are a good try, no guarantee. A row further down may bring a column the first 20
did not have, and then the header changes after all. Keep the change small: add the column at the
place its `priority` gives it, and do not rebuild the list.

### The rules of the merge

- The columns are merged by `path`. When rows of different kinds bring the same `path`, the column
  is shown once. Its title and its other fields come from the first row in the window that has it,
  so the choice does not depend on the order in which pages arrived.
- A fixed column stays as the cockpit defines it. A module column with the same `path` does not
  replace it.
- The columns are sorted by `priority`, the smallest number first. A column with `show: false` is
  not drawn.
- A row without a value for a column has an empty cell.
- A module whose list part does not load must not keep the whole list empty. Its rows show the fixed
  columns and a note in the cells the module would have filled.

## The columns a workflow module brings

A user sees, in as little space as possible, the information about each task or case
needed to judge the row.

The value of a cell is `column.path`, resolved against the row with dots as separators. So
`details.customer.name` reaches into the reported business data.

Each `type` expects a different structure at `path`, and the cockpit draws it like this:

| `type`      | Value at `path`                                       | How it is drawn |
|-------------|-------------------------------------------------------|-----------------|
| `value`     | a single value                                        | localised: an integer with thousands separators, a decimal number with the local decimal mark, a boolean as "Yes" or "No" in the user's language. Text left-aligned, everything else right-aligned, decimal numbers aligned at the decimal mark |
| `i18n`      | a map with language codes as keys and texts as values | the text in the language of the user interface, falling back to the default language |
| `person`    | a `Person`                                            | `displayShort`, else `email`, else `id`. On mouse-over: `display`, an empty line, `id`, `email` |
| `date`      | a string `YYYY-MM-DD`                                 | the localised date |
| `date-time` | a string in ISO 8601 extended format                  | the localised date and, next to it, the localised hour and minute. On mouse-over also the seconds |
| `time`      | a string `hh:mm[:ss]`                                 | the localised hour and minute. On mouse-over also the seconds, if present |

Without `type` the cell is drawn like `value`. An object at the path is shown as a short note that
this column needs a type, not as `[object Object]`.

What is sent to the server as `sort` depends on the `type` of the column. This is one of the least
visible places of the whole contract:

| `type` of the column | What is sent as `sort` |
|----------------------|------------------------|
| `i18n`               | `<path>.<language>` |
| `person`             | `<path>.sort,<path>.id` |
| everything else      | `<path>` |

The reason for the second row: a person is stored in the document with the fields `id`, `fulltext`
and `sort`, and `sort` is the value to sort by. The application builds it itself, for example from
last name and first name. The general rule, which holds for filtering too: a path reaches into the
document the server stores, not into the JSON you got. For `details.*` both are the same. For
`assignee` they are not: the API returns `display`, `displayShort`, `email` and `avatar`, and the
stored fields are `id`, `fulltext` and `sort`.

Whether a module may draw cells itself is an open question, see
[module-federation.md](module-federation.md#cells-drawn-by-a-workflow-module). It depends on a way to
do it without starting a framework once per cell, and prompt `1500` looks for one. More column
types out of the box, for example a currency, are a wish for later.

## The actions on a user task

For one row or for several selected rows:

| Action           | What it means |
|------------------|---------------|
| Claim            | the user becomes the assignee |
| Unclaim          | the assignee is removed |
| Propose          | a person the user searched for becomes a candidate user |
| Remove candidate | a person is removed from the candidate users |
| Mark as read     | sets `read`, the row is no longer bold |
| Mark as unread   | takes `read` back |
| Follow-up        | sets or deletes the follow-up date |

Claiming a task that already has another assignee takes it away from a colleague, see
[rough-edges.md](rough-edges.md#claiming-overwrites). So the user interface asks first. The question
has a checkbox "don't ask again". When the user ticks it, the choice is kept as a user setting, see
[User settings](#user-settings), and the question no longer comes, in any browser.

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
later. A user interface without live updates (`Q-LIVE-MODE` is `refresh-button`) reads the row again
after every action.

Closed tasks: disable these actions when `endedAt` is set, see
[rough-edges.md](rough-edges.md#closed-tasks-can-still-be-claimed).

## Opening and working on a user task

The user works on the task to complete it in business terms.

The page comes from the workflow module, not from the cockpit. The cockpit puts it into its frame
and shows the general functions in the application header.

### Opening

The title in the list is a link to the task page. A click opens it in the same tab, and
"open in new tab" works too. A task whose `uiUriType` is `EXTERNAL` opens its address in a tab of
its own, so the list stays. The type decides this, not the form of the address. The address itself
is built by the one rule of
[module-federation.md](module-federation.md#which-data-finds-the-part). From a task to its case
always leads to the cockpit's own case page, not to the address of the task. The task says where
the task is shown, and that is not where the case is shown.

### The header while a task is open

The task actions of the cockpit always sit in the application header. There is no header or footer
of the task's own. The header shows information about the task (`Q-TASK-HEADER-INFO`), for example
task kind, case kind and case number, and offers claim, unclaim, propose to a person, follow-up and
the note of the task (`Q-TASK-HEADER-ACTIONS`). It also offers "mark as unread", because an opened
task counts as read.

The header stands in the same place for all kinds of tasks, so it is a visual anchor for the user.
The controls of the actions are always in the same place. Its content can never break it: each
value of the information gets its own share of the width, and each value that is too long for its
share is cut with "..." on its own. One long value never pushes the others out.

The header belongs to the cockpit, not to the workflow module.

### Open as own window (`Q-OWN-WINDOW`)

The use case: a customer calls and wants to know about their case while the user is in the middle
of a form. The user must not have to close the form and lose the input.

The header offers "open as own window". It opens the list in a new tab with `window.open`, and the
cockpit keeps the window object this returns. The task stays in this tab. The application header
stays as well, because it holds the task actions. Only the burger menu goes. In its place stands a
red icon button "back to the list".

The button cancels the work on the task and brings the tab of the list to the front, with `focus()`
on the kept window object, as long as that tab still exists. If the tab is gone, or the browser does
not allow it, the cockpit shows a hint that the list is in another tab.

Before that, the cockpit warns that the input may be lost, and the user can stay. The same warning
comes when the user closes the tab (`beforeunload`).

**To verify:** whether `focus()` on the kept window object brings the tab to the front is up to the
browser. Probe `1508` checks it: a small example application in plain JavaScript that shows what
works in which browser today. Until its result is in, the checklist items `TF-007`, `TF-017` and
`TF-018` say they are to be verified with `1508`.

### What the page does

1. It loads the user task and marks it as read.
2. It asks the [loader](module-federation.md#one-registry-and-one-loader) for `UserTaskForm` of
   the task's module and calls `userTaskForm` with the task.
3. While one of the two is running, it shows a loading state. On an error, a note with a button that
   tries again. There are two different errors: the task could not be loaded, or the part of the
   module could not be loaded.
4. It puts the web components the function named into their places and sets their properties, see
   [types.md](types.md#what-a-part-gets).

### The areas and the frame

The function returns either one web component or one per area (`Q-PAGE-AREAS`), see
[module-federation.md](module-federation.md#the-page-of-a-user-task-or-a-case). A string is the
name of one web component, the all-in-one form: it gets the whole area, with no column. An object
names a web component per area, the laid-out form. With the default convention:

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

If claiming is hard (`Q-CLAIM`), the areas of the page cannot be edited before the user has claimed
the task. Usually a half-transparent layer lies over them. How it looks is up to the organisation.

### Everything inside the page is the module's

All requests that are needed to show data or to complete the form go through the proxy `/wm/`
directly to the workflow module. The cockpit knows nothing about them. Whether a form saves a draft
automatically, by hand with a save button, or not at all, the form decides.

### Changes by others

When somebody else changes the task while the user has it open, the user is
told right away. For example: "User X has claimed this task. Please talk to them before you save or
complete, to avoid losing data." The message is a sticky toast that the user has to click away.

## The case list

Technically the same holds as for the task lists. Only the differences are listed here.

Users find cases, get a first idea of their state from the columns, and open the
details page when needed.

### Which cases

The server selects the cases from `accessibleToUsers` and `accessibleToGroups`.
These two fields are not shown as columns.

### Primary filters

The interview decides which are offered (`Q-WORKFLOW-FILTERS`). By default these are `Active` and
`Inactive`, and `All` is possible as well.

### Rows

Every case has one row. Modules define their own columns for the case list, apart from the task
columns. The case list has its own fixed columns (`Q-WORKFLOW-COLUMNS`). There is no read marker
and no read function. A case list without bulk actions has no selection column. Notes work as in
the task list, per row and for all selected rows, see [Notes](#notes).

In `Inactive` a completed case looks like any other row. Only a cancelled case looks inactive: grey,
with its title struck through, the same as for user tasks. `endReason` tells the two apart.

Some organisations want fixed columns only, as for the task lists (`Q-MODULE-COLUMNS`).

## The page of a case

The user looks at the details of a case. The workflow module may also offer actions
apart from the regular flow of the process here, for example "cancel case", which the BPMN process
handles in an event-based sub-process.

The page works like the page of a user task, with these differences:

- There is no "open as own window". "Open in new tab" with the right mouse button works.
- The application header has a form of its own for a case, like the one for a user task. It shows
  information about the case (`Q-WORKFLOW-HEADER-INFO`), for example case kind and case number,
  and the actions on the case (`Q-WORKFLOW-HEADER-ACTIONS`), for example the note of the case. It
  shows no task actions. The same rule holds as for the task: each value has its own share of the
  width, and the controls stay in the same place. There is no header or footer of the case's own.

The page asks the loader for `WorkflowPage` and calls `workflowPage` with the case. The function
returns one web component for the all-in-one form, or one per area, the same areas as on the page
of a user task (`Q-PAGE-AREAS`). The page has the same loading and error states as the task page,
and gives the module `getUserTasks` for the tasks of the case. Long content scrolls in its own area
and never breaks the frame of the cockpit.

## Search, filter, sort

The user copes well with many rows.

### Full-text search

Both lists have it. The input has an x button that clears it. The field offers
suggestions: it asks from three characters on, 300 milliseconds after the last key, shows at most 20
suggestions, and shows a note instead when there are more than twenty. A suggestion is taken with a
click, the input with the Enter key. The search ignores upper and lower case. Escape the input, see
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

A new sort or a new search loads the list again, with a new start timestamp.

A wish of customers: sorting by value instead of by what is shown. The values are stored in MongoDB
and sorted there. It would need a small structure with value and text, like `i18n`, and sorting by
the value. Cells drawn by the module would make that easy, because then the path would always be the
value. This is open.

## Paging and the window of loaded rows

A list never holds all its rows. An Angular cockpit of a customer that kept every row ran the
browser out of memory at 10,360 cases, and the page could no longer be used. So:

- The list scrolls virtually: only the rows near the visible area exist in the page. The scroll
  bar is sized from `totalElements` and a fixed row height.
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

An end-to-end test proves it. It is mandatory:

1. Fill the cockpit the test runs against with 20,000 cases, for example from a simulator that
   reports them through the BPMS API.
2. Open the case list and scroll from the top to the bottom.
3. Force a garbage collection and read the JavaScript heap with Playwright through the Chrome
   DevTools Protocol: a `CDPSession` with `HeapProfiler.collectGarbage` and then
   `Runtime.getHeapUsage`.
4. The used heap is below the limit from the interview (`Q-MEMORY-LIMIT`, 150 MB by default).

The same test runs for the task list. The CDP calls exist in Chromium only, so this test runs in
Chromium.

## Live updates

Changes to the data shown become visible soon, without the user doing anything.

### How it looks (`Q-LIVE-MODE` is `live`)

A new task appears in the list, highlighted in one colour. A change shows right away, highlighted
in another colour. A task that ended while the user watched looks inactive, grey; a cancelled one
also has its title struck through. These markers stay, so a user who comes back to the screen sees
what happened. A button acknowledges the changes. Then the markers and the inactive rows go away.

A change the current user made is not marked as changed. The user expects it, so the row just shows
the new data. The update stream does not say who made a change, see
[rough-edges.md](rough-edges.md#an-event-says-that-something-changed-not-what). So the user
interface remembers the ids of the rows the user just changed through the cockpit, and the next
change of such a row comes without a marker.

### Experiment (`Q-LIVE-MODE` is `auto-acknowledge`)

The marker of a row goes away by itself, for example three seconds after the mouse has left the
row. This is still to be validated. It does not hold for rows that have to leave the list, the
ones marked as removed. They stay until the user opens the list again.

### Refresh button only (`Q-LIVE-MODE` is `refresh-button`)

Some organisations want no live update at all. The header has a refresh button that becomes active
when changes were reported. Nothing else moves.

### The rules

- A list with a primary filter for active rows still contains rows that became inactive while the
  user watched, the ones reported as ended. Scrolling must still work correctly.
- The counts are updated when something changes, see [Counts](#counts).
- A row never disappears under the user's hands. It is coloured and stays, until the user
  acknowledges, or until its page leaves the window.

### How it works

1. When the list is built, keep the timestamp the server returned. That is the `initialTimestamp`
   from [gui-api.md](gui-api.md#the-timestamp-that-keeps-a-list-stable).
2. Listen on the update stream to the name of the list's kind, `UserTask` or `Workflow`. Take the id
   from each event. Every type leads to the same action, except `RELOAD`, which loads the whole list
   again.
3. Read the rows of the window again. If the window starts at the top of the list, ask with `PUT`
   for as many rows as the window holds, plus a reserve of one page, and leave out the ids of the
   changed rows. They come back in full. If the window starts further down, read its pages again
   with `POST` and the same `initialTimestamp`, see
   [gui-api.md](gui-api.md#the-second-form-of-the-same-query).
4. Set the state of each returned row:
   * ended after the start timestamp: `ENDED`,
   * otherwise created after the start timestamp: `NEW`,
   * otherwise changed after the start timestamp, by somebody else: `UPDATED`,
   * otherwise `INITIAL`.
5. A row in the window the server no longer returns stays in place and becomes `REMOVED_FROM_LIST`.
6. The user's selection stays.
7. The counts are asked again, see [Counts](#counts).
8. Acknowledging loads the list again with a new start timestamp. That drops the markers and the
   inactive rows.

The states `INITIAL`, `NEW`, `UPDATED`, `ENDED` and `REMOVED_FROM_LIST` each have a look from the
theme (`Q-THEME`), and the legend in the status line explains them. `ENDED` uses the one style for
inactive rows.

An event of the type `FOLLOWUP` is the reason a list must not only ask about rows it knows. The
server sends it when a follow-up date has come. The task was filtered out of the list until then. It
only appears because of this event.

An open task page listens too. When its task changes, it reads the task again, sets the property of
the module's web components again, and shows the toast described in
[Opening and working on a user task](#opening-and-working-on-a-user-task).

## Notifications

Users learn about new tasks they could take, even when the cockpit is not open in their
browser.

A menu entry of its own leads to the settings (`Q-NOTIFICATIONS`). The user chooses, for all cases
and per kind of case, whether they want notifications and on which channel. The endpoints are
`GET /notifications/media`, `GET` and `PUT /notifications/config`, `GET /notifications/recipient-config`,
`PUT /notifications/recipient-config/{mediumType}` and `GET /notifications/workflows`.

A link in a notification mail points to the task page. It must work without a detour over the start
page, also when the user has to log in first.

## User settings

Users expect the cockpit to remember their choices in every browser they use. So every user
setting is stored on the server, in the map `customSettings` of the user. Prompt `1505` adds it,
with endpoints that read and write one entry, see [gui-api.md](gui-api.md#user-settings).

This holds for every setting, whether the user sets it on purpose or the user interface only
remembers it. Examples:

| Setting                                                 | Explicit or implicit |
|---------------------------------------------------------|----------------------|
| the language                                            | explicit, in the menu |
| "don't ask again" before claiming                       | explicit, in the question |
| the primary filter last chosen                          | implicit |
| the width of a column the user resized                  | implicit |
| the order or the hidden state of columns, where offered | implicit |

The generated user interface has one global settings service for this. Every part of it that keeps
a setting uses the service, and nothing else calls the endpoints. The service:

- reads an entry the first time somebody asks for it, and keeps it in a cache in memory,
- answers every later question from the cache,
- writes a change to the server and updates the cache at once, so the page does not wait,
- names its keys with a prefix of the user interface and a fixed scheme, for example
  `bc.tasks.primaryFilter` or `bc.columns.tasks.<path>.width`,
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

The chosen language must reach every place where a text per language is drawn: the column header,
the cell, the title of a row, and every web component of a module, through the property `language`.

Decide once what happens when a language is missing, and keep to it everywhere: take the default
language, and if that is missing too, the first language there is. A header, a cell of type `i18n`
and the title of a row behave the same.

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
- Which extension points exist is decided per organisation. It is a module federation contract
  between the cockpit and the workflow modules, and it holds for all modules. The types library
  writes it down, see [types.md](types.md#the-extension-points).
- A module that does not fill an extension point causes no error. It is ignored.

How it works is in [module-federation.md](module-federation.md#the-other-extension-points).

## Who sees what

Different groups of users may get different views (`Q-VISIBLE-GROUPS`). How is up to the
organisation. The groups are taken from the first call of `GET /app/current-user`, because the JWT
may carry fewer groups, or none.

This only shapes the user interface. What a user may see and do is decided by the server for every
request.
