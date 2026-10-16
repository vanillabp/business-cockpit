# Reference checklist 1.0.0

*Part of the description in [`../SKILL.md`](../SKILL.md).*

This is the base checklist of a generated Business Cockpit user interface, version 1.0.0. The
generated project copies it into `CHECKLIST.md` and adds the customer's own items with the prefix
`CUST-`, see [`../SKILL.md`](../SKILL.md#phase-2-generate). Every end-to-end test names the id of the
item it proves.

Each item is one sentence that a test can prove. The column "Applies" says whether the item always
holds or depends on an interview answer, see [interview.md](interview.md).

Ids are stable. They are never reused and never renumbered. A later version of this list adds items
with new ids, changes the text of an item only when its meaning stays, and marks an item as removed
instead of deleting it. So an upgrade is the diff between two versions of this file.

| Prefix  | Area |
|---------|------|
| `PKG-`  | build outputs of the project |
| `AUTH-` | loading, login and staying logged in |
| `NAV-`  | screens, routes, the header and the menu |
| `TL-`   | task lists |
| `COL-`  | columns and cells |
| `ACT-`  | actions on user tasks |
| `TF-`   | the page of a user task |
| `WL-`   | the case list |
| `WP-`   | the page of a case |
| `SRCH-` | search, filter, sort and paging |
| `LIVE-` | live updates |
| `EXT-`  | module parts and extension points |
| `SET-`  | user settings |
| `I18N-` | language and texts |
| `NOTI-` | notifications |
| `DS-`   | dev shell |

## PKG: build outputs

| Id        | Item                                                                                                                                                                                                                              | Applies |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `PKG-001` | Removed: the types jar left the UI repository, see `PKG-015` and `PKG-016`.                                                                                                                                                       | removed |
| `PKG-002` | The GUI API client is generated at build time from the OpenAPI files of the pinned cockpit version and is not committed.                                                                                                          | always |
| `PKG-003` | The UI jar has the coordinates from the interview and holds `index.html` and the bundle at the place the backend framework serves: `static/` or `META-INF/resources/`.                                                            | always |
| `PKG-004` | Added as a dependency to the backend, the UI jar makes `/` show the user interface.                                                                                                                                               | always |
| `PKG-005` | The backend repository builds with Maven only, takes the UI jar as an ordinary dependency, and a Maven release of it succeeds.                                                                                                    | always |
| `PKG-006` | The types library is published with no dependencies and no peer dependencies.                                                                                                                                                     | always |
| `PKG-007` | Each dev shell package is published with its framework as a peer dependency and the types library as a dependency.                                                                                                                | always |
| `PKG-008` | `INTERVIEW.md` and `CHECKLIST.md` are in the project and name the version of this reference.                                                                                                                                      | always |
| `PKG-009` | The bundle contains no part of any workflow module.                                                                                                                                                                               | always |
| `PKG-010` | Removed: replaced by `PKG-016`.                                                                                                                                                                                                   | removed |
| `PKG-011` | The upload script puts jar, pom and their checksums into the Maven repository with HTTP `PUT`, and refuses to overwrite a release version that is already there.                                                                  | always |
| `PKG-012` | The nginx image serves the user interface, answers an unknown path with `index.html`, and forwards `/gui/api/`, `/wm/` and the login paths to the backend.                                                                        | always |
| `PKG-013` | Through the nginx image the update stream delivers events without delay and stays open longer than the ping interval.                                                                                                             | always |
| `PKG-014` | The UI repository has no `pom.xml`, and the backend repository has no `package.json`.                                                                                                                                             | always |
| `PKG-015` | One npm workspace builds the user interface, the types library, every dev shell and the UI jar with one command, without Maven and without Java.                                                                                  | always |
| `PKG-016` | The types jar is built with Maven outside the UI repository, from `ui-uri-types.json` of the types library of the same version; it holds one constant per entry, `EXTERNAL` included, and none points to a constant of VanillaBP. | always |
| `PKG-017` | The types library carries `ui-uri-types.json` and one TypeScript constant for each of its values.                                                                                                                                 | always |
| `PKG-018` | The types jar is built by a Maven project of its own, not by the backend application or the UI build, and its `README.md` explains the order cockpit release, user interface, types module, backend, and why.                     | always |
| `PKG-019` | The OpenAPI files are loaded from the jars of the cockpit coordinates in the Maven repository of the interview, with the token from the environment variable the interview names.                                                 | always |

## AUTH: loading and login

| Id         | Item                                                                                                      | Applies |
|------------|-----------------------------------------------------------------------------------------------------------|---------|
| `AUTH-001` | Without a login, the shell and its static files load, and the frame shows the title from `GET /app/info`. | always |
| `AUTH-002` | When `GET /app/current-user` answers without a body, the browser is sent to the login of the backend.     | always |
| `AUTH-003` | After the login, the user interface shows the user from `GET /app/current-user`.                          | always |
| `AUTH-004` | A 401 on any later request sends the browser to the login, and no error page is shown.                    | always |
| `AUTH-005` | The user interface has no login form, no user management and no rights check of its own.                  | always |
| `AUTH-006` | The user interface makes no request in the background only to stay logged in.                             | always |
| `AUTH-007` | `/logout` ends the session and shows the start page.                                                      | always |
| `AUTH-008` | The groups used to shape the screens come from `GET /app/current-user`, not from the token.               | always |

## NAV: screens, header and menu

| Id        | Item                                                                                                                                         | Applies |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `NAV-001` | `/tasks` shows all user tasks the user may see.                                                                                              | always |
| `NAV-002` | `/my-tasks` shows the user tasks where the user is the assignee.                                                                             | always |
| `NAV-003` | There is exactly one case list.                                                                                                              | always |
| `NAV-004` | A deep link to any screen, opened in a new browser, shows that screen after the login.                                                       | always |
| `NAV-005` | The dashboard shows what the interview asked for, and a click on it opens the matching list, narrowed.                                       | `Q-DASHBOARD` |
| `NAV-006` | The header has a toggle `My tasks` / `All tasks` / `Cases`, each part with its count in brackets, which opens the list without narrowing.    | always |
| `NAV-007` | Each sub-list shows its count of visible and of claimed tasks, and 0/0 for a role the user does not have.                                    | `Q-SUB-LISTS` |
| `NAV-008` | Users of the configured groups see the screens and menu entries meant for them, and others do not.                                           | `Q-VISIBLE-GROUPS` |
| `NAV-009` | The user interface uses the one word for a case from the interview in every text and never mixes it with another.                            | always |
| `NAV-010` | All counts are asked again when the application opens, when a list opens, on an update event of their kind, and on return to the tab.        | always |
| `NAV-011` | The top of the menu shows the user with avatar and short form; on mouse-over of the avatar the long form, the user id and the email address. | always |
| `NAV-012` | Placeholder: on mouse-over of the name the menu shows the rights of the user and where they come from. A later version specifies it.         | not yet specified |
| `NAV-013` | In the other views the header shows what the interview decided.                                                                              | `Q-HEADER` |

## TL: task lists

| Id       | Item                                                                                                                                                                | Applies |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `TL-001` | The status line offers the primary filters from the interview, with short translated labels.                                                                        | always |
| `TL-002` | The default primary filter is the one from the interview, by default `OpenTasksWithFollowUp`.                                                                       | always |
| `TL-003` | Each primary filter except `ClosedTasksOnly` shows its count in brackets. The count of `ClosedTasksOnly` is asked on mouse-over only, and not kept or updated live. | always |
| `TL-004` | Removed: replaced by `TL-024`.                                                                                                                                      | removed |
| `TL-005` | The primary filter last chosen is used again the next time the user opens the list.                                                                                 | always |
| `TL-006` | The status line shows a legend for new, changed, removed and inactive rows.                                                                                         | `Q-LIVE-MODE` ≠ `refresh-button` |
| `TL-007` | A row whose task the user has not read is bold.                                                                                                                     | always |
| `TL-008` | The fixed columns are the ones from the interview, in its order.                                                                                                    | always |
| `TL-009` | The follow-up column shows only for `OpenTasksWithFollowUp` and `OpenTaskOnlyFollowUp`.                                                                             | `Q-FOLLOW-UP` = yes |
| `TL-010` | The ended column shows only for `ClosedTasksOnly`.                                                                                                                  | `Q-CLOSED-TASKS` = yes |
| `TL-011` | The `id` column is a checkbox that selects the row, and the id is not shown.                                                                                        | `Q-BULK-ACTIONS` not empty |
| `TL-012` | The title is a real link that opens the task page, and "open in new tab" works on it.                                                                               | always |
| `TL-013` | The assignee cell shows the short form, and on mouse-over of the name the long form, the user id and the email address.                                             | always |
| `TL-014` | On mouse-over of the assignee cell a checkbox appears in the cell, whether or not the details show; it claims the task, or gives it back.                           | always |
| `TL-015` | A task the current user has claimed shows a bar before the name, and the names stay aligned.                                                                        | always |
| `TL-016` | The candidate users column is icon-wide and shows a one-person or a several-people icon.                                                                            | always |
| `TL-017` | On mouse-over the candidate users column lists all persons, each with a bin icon that removes the person.                                                           | always |
| `TL-018` | A date column shows the localised date, or the time if the date is today, and the other one on mouse-over.                                                          | always |
| `TL-019` | The selected tasks can be acted on together with the bulk actions from the interview.                                                                               | `Q-BULK-ACTIONS` not empty |
| `TL-020` | Removed: replaced by `TL-025` and `TF-023`.                                                                                                                         | removed |
| `TL-021` | A task put on follow-up leaves `OpenTasksWithoutFollowUp` until its date has come.                                                                                  | `Q-FOLLOW-UP` = yes |
| `TL-022` | A closed task can be listed and opened like an open one; the cockpit handles it the same whatever its status, and the module draws its page.                        | `Q-CLOSED-TASKS` = yes |
| `TL-023` | In `ClosedTasksOnly` a completed task looks like any other row, and a cancelled one is grey with its title struck through, as `endReason` says.                     | `Q-CLOSED-TASKS` = yes |
| `TL-024` | Every count comes from the count endpoint, and no page of a list is asked only for its total.                                                                       | always |
| `TL-025` | A note can be written per row in the task list and for all selected tasks at once in the list.                                                                      | `Q-NOTES` = yes |

## COL: columns and cells

| Id        | Item                                                                                                                                                  | Applies |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `COL-001` | The first 20 rows give the starting columns; then the columns follow the loaded window, growing as pages load and shrinking as far pages are dropped. | `Q-MODULE-COLUMNS` = yes |
| `COL-002` | The list parts are asked from the central loader only for the modules of loaded rows, and a part loaded once is not loaded again.                     | `Q-MODULE-COLUMNS` = yes |
| `COL-003` | Columns of different rows and modules with the same `path` appear once.                                                                               | `Q-MODULE-COLUMNS` = yes |
| `COL-004` | Columns are ordered by `priority`, the smallest first, and a column with `show: false` is not drawn.                                                  | `Q-MODULE-COLUMNS` = yes |
| `COL-005` | A module whose list part fails to load does not keep the list empty; its rows show the fixed columns.                                                 | `Q-MODULE-COLUMNS` = yes |
| `COL-006` | A `value` cell is localised, text left-aligned, other values right-aligned, decimals aligned at the decimal mark.                                     | `Q-MODULE-COLUMNS` = yes |
| `COL-007` | A boolean `value` cell shows "Yes" or "No" in the user's language.                                                                                    | `Q-MODULE-COLUMNS` = yes |
| `COL-008` | An `i18n` cell shows the text in the language of the user interface, or in the default language if missing.                                           | `Q-MODULE-COLUMNS` = yes |
| `COL-009` | A `person` cell shows `displayShort`, else `email`, else `id`, and on mouse-over `display`, `id` and `email`.                                         | `Q-MODULE-COLUMNS` = yes |
| `COL-010` | A `date` cell shows the localised date.                                                                                                               | `Q-MODULE-COLUMNS` = yes |
| `COL-011` | A `date-time` cell shows the localised date with hour and minute, and the seconds on mouse-over.                                                      | `Q-MODULE-COLUMNS` = yes |
| `COL-012` | A `time` cell shows the localised hour and minute, and the seconds on mouse-over if present.                                                          | `Q-MODULE-COLUMNS` = yes |
| `COL-013` | A cell whose value is an object shows a short note, not `[object Object]`.                                                                            | `Q-MODULE-COLUMNS` = yes |
| `COL-014` | No list shows a module column when the organisation chose fixed columns only.                                                                         | `Q-MODULE-COLUMNS` = no |
| `COL-015` | The fixed columns are always shown, and a module column with the same `path` does not replace one.                                                    | always |
| `COL-016` | The column function of a module is called for every loaded row, with that row.                                                                        | `Q-MODULE-COLUMNS` = yes |

## ACT: actions on user tasks

| Id        | Item                                                                                                                                                   | Applies |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `ACT-001` | Claim makes the user the assignee, and the row shows it.                                                                                               | always |
| `ACT-002` | Removed: replaced by `ACT-011`.                                                                                                                        | removed |
| `ACT-003` | Unclaim removes the user as assignee, and the row shows it.                                                                                            | always |
| `ACT-004` | "Propose to" a person found by search makes the person a candidate user.                                                                               | always |
| `ACT-005` | Removing a candidate user takes the person off the task.                                                                                               | always |
| `ACT-006` | Mark as read and mark as unread change the bold state of the row.                                                                                      | always |
| `ACT-007` | Setting and deleting a follow-up date works for one task and for the selected tasks.                                                                   | `Q-FOLLOW-UP` = yes |
| `ACT-008` | The effect of the user's own action shows as soon as the server has accepted it.                                                                       | always |
| `ACT-009` | The actions are disabled for a task whose `endedAt` is set.                                                                                            | always |
| `ACT-010` | A failed single action tells the user, without revealing whether the task exists.                                                                      | always |
| `ACT-011` | Claiming a task that has another assignee asks the user first; the question has "don't ask again", and that choice holds in every browser of the user. | always |

## TF: the page of a user task

| Id       | Item                                                                                                                                                                                                                                | Applies |
|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `TF-001` | Opening a task loads it, marks it as read and shows the web components the module's page function names.                                                                                                                            | always |
| `TF-002` | While the task or the module part loads, a loading state shows.                                                                                                                                                                     | always |
| `TF-003` | A task that fails to load and a module part that fails to load show two different notes, each with a retry button.                                                                                                                  | always |
| `TF-004` | The task actions sit in the application header, in the same place for every task kind; there is no header or footer of the task's own.                                                                                              | always |
| `TF-005` | The header shows the information from the interview; each value has its own share of the width and is cut with "..." on its own when too long.                                                                                      | always |
| `TF-006` | The header offers the task actions from the interview, always in the same place.                                                                                                                                                    | always |
| `TF-007` | The button "list in its own window" opens the list with `window.open` in a window of its own; the task tab keeps the application header, and a red "back to the list" button replaces the burger menu. To verify with probe `1508`. | `Q-OWN-WINDOW` = yes |
| `TF-008` | On the page of a user task and of a case, long content scrolls in its own area, the application header stays, and the frame never breaks.                                                                                           | always |
| `TF-009` | The page cannot be edited before the user has claimed the task.                                                                                                                                                                     | `Q-CLAIM` = hard |
| `TF-010` | The page can be edited without a claim.                                                                                                                                                                                             | `Q-CLAIM` = informal |
| `TF-011` | When somebody else changes the open task, a toast tells the user, and it stays until clicked away.                                                                                                                                  | `Q-LIVE-MODE` ≠ `refresh-button` |
| `TF-012` | Removed: replaced by `TF-019` and `TF-020`.                                                                                                                                                                                         | removed |
| `TF-013` | The link from a task to its case opens the cockpit's case page.                                                                                                                                                                     | always |
| `TF-014` | A link from a notification mail opens the task page directly, after a login if needed.                                                                                                                                              | `Q-NOTIFICATIONS` = yes |
| `TF-015` | Removed: replaced by `TF-021`.                                                                                                                                                                                                      | removed |
| `TF-016` | A message sent with `sendTo` from one area reaches the handler of `receiveMessage` in the named area of the same page.                                                                                                              | always |
| `TF-017` | "Back to the list" and closing the task tab both warn that input may be lost, and the user can stay. To verify with probe `1508`.                                                                                                   | `Q-OWN-WINDOW` = yes |
| `TF-018` | To verify with probe `1508` in Chromium, Firefox and WebKit: "back to the list" brings the window of the list to the front, or shows a hint when that is not possible.                                                              | `Q-OWN-WINDOW` = yes |
| `TF-019` | A task whose `uiUriType` is `EXTERNAL` opens its address in a tab of its own, and the list stays intact.                                                                                                                            | always |
| `TF-020` | The address of a task is its `uiUri` unchanged when it starts with `http://` or `https://`, and `/wm/<workflowModuleId>/` plus `uiUri` otherwise, whatever its `uiUriType`.                                                         | always |
| `TF-021` | A page function that returns a string gets the whole area for that one web component, with no column; one that returns an object gets each named web component in its area.                                                         | always |
| `TF-022` | A message sent before the target area subscribed reaches it once it subscribes, and after `unsubscribe` no message reaches it.                                                                                                      | always |
| `TF-023` | The header of an open task offers its note.                                                                                                                                                                                         | `Q-NOTES` = yes |
| `TF-024` | A click on the title of a task or a case opens its page inside the application, in the same tab; the cockpit opens no tab by itself, except for a task whose `uiUriType` is `EXTERNAL`.                                             | always |

## WL: the case list

| Id       | Item                                                                                                                                     | Applies |
|----------|------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `WL-001` | The case list offers the primary filters from the interview, by default `Active` and `Inactive`.                                         | always |
| `WL-002` | Its fixed columns are the ones from the interview, in its order.                                                                         | always |
| `WL-003` | Modules add their own case columns, apart from the task columns.                                                                         | `Q-MODULE-COLUMNS` = yes |
| `WL-004` | There is no read marker in the case list.                                                                                                | always |
| `WL-005` | `accessibleToUsers` and `accessibleToGroups` are not shown as columns.                                                                   | always |
| `WL-006` | The title opens the case page, and "open in new tab" works on it.                                                                        | always |
| `WL-007` | In `Inactive` a completed case looks like any other row, and a cancelled one is grey with its title struck through, as `endReason` says. | always |
| `WL-008` | A note can be written per row in the case list and for all selected cases at once in the list.                                           | `Q-NOTES` = yes |
| `WL-009` | The default primary filter of the case list is the one from the interview, by default `Active`.                                          | always |

## WP: the page of a case

| Id       | Item                                                                                                                                                                                                     | Applies |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `WP-001` | Opening a case loads it and shows the web components the module's page function names.                                                                                                                   | always |
| `WP-002` | Removed: replaced by `WP-007`.                                                                                                                                                                           | removed |
| `WP-003` | The case page gets `getUserTasks`, and it returns the user tasks of the case.                                                                                                                            | always |
| `WP-004` | Removed: merged into `TF-008`, which holds for both pages.                                                                                                                                               | removed |
| `WP-005` | A case page that fails to load shows a note with a retry button.                                                                                                                                         | always |
| `WP-006` | Removed: replaced by `WP-008`.                                                                                                                                                                           | removed |
| `WP-007` | The case page has no header or footer of its own and no button "list in its own window"; the application header shows the case information and the case actions from the interview, and no task actions. | always |
| `WP-008` | The case page shows one web component or the same areas as the page of a user task, as the page function returns.                                                                                        | always |
| `WP-009` | The header of an open case offers its note.                                                                                                                                                              | `Q-NOTES` = yes |

## SRCH: search, filter, sort and paging

| Id         | Item                                                                                                                                    | Applies |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------|---------|
| `SRCH-001` | Both lists have a full-text search with an x button that clears it.                                                                     | always |
| `SRCH-002` | Suggestions are asked from three characters on, 300 milliseconds after the last key.                                                    | always |
| `SRCH-003` | At most 20 suggestions show, and a note shows instead when the server returns 21.                                                       | always |
| `SRCH-004` | Special characters typed by the user are escaped before they go to the server.                                                          | always |
| `SRCH-005` | A filter can be set and removed per column, and only one filter per path is sent.                                                       | always |
| `SRCH-006` | A button resets all searches and filters.                                                                                               | always |
| `SRCH-007` | A click on a sortable header sorts by it, a second turns the direction, a third goes back to the default.                               | always |
| `SRCH-008` | An `i18n` column sorts by `<path>.<language>`, a `person` column by `<path>.sort,<path>.id`.                                            | always |
| `SRCH-009` | Only columns with `sortable` offer sorting, and only columns with `filterable` offer a filter.                                          | always |
| `SRCH-010` | The list scrolls virtually and holds a window of pages: the visible area plus one page ahead in both directions; far pages are dropped. | always |
| `SRCH-011` | Every request of a list after the first sends the `initialTimestamp` of the first answer.                                               | always |
| `SRCH-012` | Every list request sets `pageSize` and the primary filter.                                                                              | always |
| `SRCH-013` | After scrolling a list of 20,000 cases from top to bottom, the JavaScript heap is below the limit from the interview.                   | always, with `Q-MEMORY-LIMIT` |
| `SRCH-014` | A taller list area holds more rows in its window than a shorter one.                                                                    | always |
| `SRCH-015` | Scrolling up and down shows no empty rows while pages load.                                                                             | always |

## LIVE: live updates

| Id         | Item                                                                                                                                                     | Applies |
|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `LIVE-001` | A task created by somebody else appears in the open list without a reload, highlighted as new.                                                           | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-002` | A task changed by somebody else is updated in place and highlighted as changed; a change by the current user only shows the new data.                    | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-003` | A task that ends while the user watches turns grey and stays in the list; a cancelled one also has its title struck through.                             | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-004` | A row the server no longer returns stays in place, marked as removed.                                                                                    | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-005` | Scrolling and loading more rows work correctly while ended and removed rows are still shown.                                                             | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-006` | The counts update after a change.                                                                                                                        | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-007` | The acknowledge button removes all highlights and the inactive rows.                                                                                     | `Q-LIVE-MODE` = `live` |
| `LIVE-008` | The highlight of a row goes away a few seconds after the mouse has left it; a row marked as removed stays until the user opens the list again.           | `Q-LIVE-MODE` = `auto-acknowledge` |
| `LIVE-009` | Nothing updates by itself, and a refresh button becomes active when changes were reported.                                                               | `Q-LIVE-MODE` = `refresh-button` |
| `LIVE-010` | A `RELOAD` event loads the list again with the same primary filter, filters and sorting.                                                                 | always |
| `LIVE-011` | A task whose follow-up date has come appears in `OpenTasksWithoutFollowUp` without a reload.                                                             | `Q-FOLLOW-UP` = yes |
| `LIVE-012` | When the server ends the stream, the user interface connects again within one second.                                                                    | always |
| `LIVE-013` | After a lost connection, the user interface connects again with a growing pause of at most 15 seconds.                                                   | always |
| `LIVE-014` | When connecting again answers 401, the user interface stops trying and sends the browser to the login.                                                   | always |
| `LIVE-015` | The selection of the user stays when the list updates and when rows leave the window.                                                                    | `Q-BULK-ACTIONS` not empty |
| `LIVE-016` | An update reads only the rows of the loaded window again, never the whole list.                                                                          | `Q-LIVE-MODE` ≠ `refresh-button` |
| `LIVE-017` | The types of the update events come from the generated client; the stream is opened with `EventSource` or a `fetch` stream, not with a generated method. | always |

## EXT: module parts and extension points

| Id        | Item                                                                                                                                                     | Applies |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `EXT-001` | Parts are loaded with the native federation orchestrator from `/wm/<workflowModuleId>/remoteEntry.json`.                                                 | always |
| `EXT-002` | Each pair of module and exposed name is loaded at most once per page.                                                                                    | always |
| `EXT-003` | Removed: replaced by `EXT-020`.                                                                                                                          | removed |
| `EXT-004` | Removed: replaced by `EXT-021`.                                                                                                                          | removed |
| `EXT-005` | Every web component of a module renders inside a shadow DOM, and the cockpit's styles do not change it, nor it the cockpit's.                            | always |
| `EXT-006` | A name of a web component that is not registered when the module's function returns is treated as a failed load.                                         | always |
| `EXT-007` | A `bc-request` from a part is answered with exactly one `bc-response` with the same `requestId`.                                                         | always |
| `EXT-008` | An unknown action is answered with the error code `UNKNOWN_ACTION`.                                                                                      | always |
| `EXT-009` | An action the server refuses is answered with `REFUSED` and the HTTP status.                                                                             | always |
| `EXT-010` | Every extension point outside the lists, the task page and the case page asks every module the user may see; those three ask only the module of the row. | always |
| `EXT-011` | A module that does not expose an extension point causes no error and no message to the user.                                                             | always |
| `EXT-012` | The extension points from the interview work with a blueprint module that fills them.                                                                    | `Q-EXTENSION-POINTS` |
| `EXT-013` | The theme reaches the module's shadow DOM as the CSS custom properties named in the types library.                                                       | `Q-THEME-TOKENS` = yes |
| `EXT-014` | When the language changes, every visible web component gets the new `language` property.                                                                 | `Q-LANGUAGE-SWITCH` = yes |
| `EXT-015` | A part that fails to load shows a note and a retry button.                                                                                               | always |
| `EXT-016` | `GET /workflow-module` is called once at the start; every part comes through the one central loader, and no component loads by itself.                   | always |
| `EXT-017` | A page of a module has the route `/<workflowModuleId>/<pageId>`, and a deep link to it works.                                                            | `Q-EXTENSION-POINTS` includes pages |
| `EXT-018` | A part of a module can open a modal dialog, and the promise resolves with what the dialog closed with.                                                   | `Q-EXTENSION-POINTS` |
| `EXT-019` | A module that registered after the page loaded is added to the registry the first time a row of it shows up.                                             | always |
| `EXT-020` | A row whose `uiUriType` is the native federation value of `Q-UI-URI-TYPE` loads its parts through native federation, whatever the form of its address.   | always |
| `EXT-021` | A row with an unknown `uiUriType` is shown, and the user is told it cannot be opened here.                                                               | always |
| `EXT-022` | The exposed names are `UserTaskList`, `WorkflowList`, `UserTaskForm`, `WorkflowPage` and those of the interview, without `./` in front.                  | always |
| `EXT-023` | `cockpit.showNotification` shows a toast in the look of its `level`; a sticky one stays until clicked away, any other goes away by itself.               | always |
| `EXT-024` | `page.subscribe` answers with a `subscriptionId`, and after `page.unsubscribe` the host delivers nothing more to it.                                     | always |

## SET: user settings

| Id        | Item                                                                                                                                                            | Applies |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `SET-001` | One global settings service reads and writes every user setting, and no other code calls the settings endpoints.                                                | always |
| `SET-002` | A setting the service has read once is answered from its cache, without a new request.                                                                          | always |
| `SET-003` | A setting made in one browser holds in another browser of the same user: the language, "don't ask again", the primary filter last chosen and the column widths. | always |
| `SET-004` | The user interface keeps no setting in `localStorage`, `sessionStorage` or a cookie of its own.                                                                 | always |

## I18N: language and texts

| Id         | Item                                                                                                                      | Applies |
|------------|---------------------------------------------------------------------------------------------------------------------------|---------|
| `I18N-001` | All texts of the user interface are in separate files, one per language.                                                  | always |
| `I18N-002` | The user interface starts in the default language from the interview.                                                     | always |
| `I18N-003` | The menu offers the languages from `additionalProperties` of `GET /app/info` below its entries, and the choice is kept.   | `Q-LANGUAGE-SWITCH` = yes |
| `I18N-004` | A missing translation falls back to the default language the same way in headers, cells and titles.                       | always |
| `I18N-005` | Numbers and dates are formatted for the chosen language.                                                                  | always |
| `I18N-006` | The build fails when a language file misses a key of the file of the default language, and it names the file and the key. | always |

## NOTI: notifications

| Id         | Item                                                                                               | Applies |
|------------|----------------------------------------------------------------------------------------------------|---------|
| `NOTI-001` | A menu entry leads to the notification settings.                                                   | `Q-NOTIFICATIONS` = yes |
| `NOTI-002` | The user chooses, for all cases and per kind of case, whether to be notified and on which channel. | `Q-NOTIFICATIONS` = yes |
| `NOTI-003` | The settings are stored with `PUT` and shown again after a reload.                                 | `Q-NOTIFICATIONS` = yes |

## DS: dev shell

| Id       | Item                                                                                                                             | Applies |
|----------|----------------------------------------------------------------------------------------------------------------------------------|---------|
| `DS-001` | There is one dev shell package for every framework from `Q-MODULE-FRAMEWORKS`.                                                   | always |
| `DS-002` | The dev shell imports the module's components directly, and the dev server swaps them while typing.                              | always |
| `DS-003` | The dev shell shows real data from the dev shell simulator.                                                                      | always |
| `DS-004` | The dev shell switches between parts, user tasks and cases.                                                                      | always |
| `DS-005` | The page of a user task and of a case look the same in the dev shell as in the cockpit, with the same header and areas.          | always |
| `DS-006` | A toast a part asks for with `showNotification` is shown, and a sticky one stays until it is clicked away.                       | always |
| `DS-007` | The developer can choose the language.                                                                                           | always |
| `DS-008` | A function the simulator does not answer tells the developer so instead of doing nothing silently.                               | always |
| `DS-009` | `getUserTasks` returns the real user tasks of the case.                                                                          | always |
| `DS-010` | The dev server forwards `/official-api` and `/dev-shell` to the simulator.                                                       | always |
| `DS-011` | The dev shell has no symbol view and no copied list row.                                                                         | always |
| `DS-012` | The dev shell offers the users of the simulator; one can be picked, switched and deselected, and picking sets a real JWT cookie. | always |
| `DS-013` | Every extension point from the interview can be tried in the dev shell, besides the page of a user task and of a case.           | `Q-EXTENSION-POINTS` |
| `DS-014` | `sendTo` and `receiveMessage` carry messages between the areas in the dev shell as in the cockpit.                               | always |
| `DS-015` | The drop-down of the dev shell asks for user tasks and for cases with the primary filter `All`, and offers no other.             | always |
