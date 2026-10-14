# The interview

*Part of the description in [`../SKILL.md`](../SKILL.md).*

Ask these questions before you generate anything. Ask every one of them. Where a default is given,
propose it, and let the user decide. Write every answer into `INTERVIEW.md` in the generated
project, with the question id.

Each question has an id, for example `Q-FOLLOW-UP`. The checklist
[reference-1.0.0.md](reference-1.0.0.md) names these ids, so you can see which items depend on
which answer. Ids are never reused.

1. [The stack](#the-stack)
1. [The look](#the-look)
1. [The screens](#the-screens)
1. [The lists](#the-lists)
1. [A user task](#a-user-task)
1. [Live updates](#live-updates)
1. [Language](#language)
1. [Extension points](#extension-points)
1. [Backend and outputs](#backend-and-outputs)

## The stack

| Id                    | Question                                                                                                                                  | Default the agent proposes |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-FRAMEWORK`         | Which UI framework does the cockpit use, and which version?                                                                               | none, the user has to say |
| `Q-DESIGN-LIBRARY`    | Which component or design library, and which version? For example PrimeNG, PrimeVue, MUI, Tailwind.                                       | none, the user has to say |
| `Q-MODULE-FRAMEWORKS` | Which frameworks, and which versions, do the workflow modules use for their forms and pages? This decides which dev shells are generated. | the same as `Q-FRAMEWORK` |
| `Q-BUNDLER`           | Which build tool builds the cockpit? It does not matter for the federation, because the host only needs the runtime orchestrator.         | the default of the framework |

## The look

| Id               | Question                                                                                                                                                            | Default the agent proposes |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-THEME`        | Colours, fonts, logo, spacing. Also the colours of a row that is new, changed, ended or removed while the user watches. They also go into the dev shell base pages. | the defaults of the design library |
| `Q-THEME-TOKENS` | Shall the theme be handed to module parts as CSS custom properties, so a form can look like the cockpit inside its shadow DOM?                                      | yes |
| `Q-WORDING`      | What does the user interface call a business case? "Case" or the organisation's own word. "Workflow" or "process" are rarely the right words for users.             | "case" |

## The screens

| Id                 | Question                                                                                                                                                                          | Default the agent proposes |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-SCREENS`        | Which screens exist beyond the minimum? The minimum is `/tasks` (all user tasks the user may see), `/my-tasks` (those where the user is assignee) and one workflow list.          | the minimum plus a dashboard |
| `Q-DASHBOARD`      | What does the dashboard show, if there is one? For example one pie chart of the open user tasks by task type and one of the open cases by kind. A click opens the list, narrowed. | two pie charts, and two buttons on the left of the header for the two lists |
| `Q-SUB-LISTS`      | Are there sub-lists, narrowed to a role or to some task types? For example one card per role on the start page, with the number of visible and of claimed tasks of that role.     | none |
| `Q-NOTIFICATIONS`  | Is there a screen where the user sets up notifications?                                                                                                                           | yes |
| `Q-ROUTES`         | Which paths do the screens have, and are they translated?                                                                                                                         | English paths, not translated |
| `Q-VISIBLE-GROUPS` | Do some groups of users see other screens or menu entries? Which groups?                                                                                                          | everybody sees the same |

## The lists

| Id                   | Question                                                                                                                                                                                         | Default the agent proposes |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-PRIMARY-FILTERS`  | Which primary filters does the task list offer, and which is the default? Possible: `OpenTasks`, `OpenTasksWithoutFollowUp`, `OpenTasksWithFollowUp`, `OpenTaskOnlyFollowUp`, `ClosedTasksOnly`. | all five, default `OpenTasksWithoutFollowUp` |
| `Q-WORKFLOW-FILTERS` | Which primary filters does the workflow list offer, and which is the default? Possible: `All`, `Active`, `Inactive`.                                                                             | all three, default `Active` |
| `Q-FOLLOW-UP`        | Is there a follow-up feature (put a task aside until a date)?                                                                                                                                    | yes |
| `Q-CLOSED-TASKS`     | Are closed user tasks shown? They document with which input a task was completed.                                                                                                                | yes |
| `Q-TASK-COLUMNS`     | Which fixed columns does the task list have, and in which order? For example id, case kind, title, assignee, candidate users, created, due, follow-up, ended, notes.                             | id, title, assignee, candidate users, created, due, follow-up, ended |
| `Q-WORKFLOW-COLUMNS` | Which fixed columns does the workflow list have, and in which order?                                                                                                                             | id, title, created, ended |
| `Q-MODULE-COLUMNS`   | Do the workflow modules add columns of their own (dynamic columns)? Some organisations want fixed columns only.                                                                                  | yes |
| `Q-BULK-ACTIONS`     | Which actions are offered for several selected tasks? Claim, propose to a person (candidate user), mark as read or unread, follow-up, notes.                                                     | claim, propose, read and unread, follow-up |
| `Q-NOTES`            | Can user tasks carry notes, edited in the list and in the task header? The GUI API has no endpoint for notes yet, so this needs a backend extension.                                             | no |
| `Q-CLAIM`            | Is claiming informal, or hard (the form cannot be edited until the user claims the task)?                                                                                                        | hard |
| `Q-PAGE-SIZE`        | How many rows does one block of the list load?                                                                                                                                                   | 30 |

## A user task

| Id                   | Question                                                                                                                                                                     | Default the agent proposes |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-TASK-BAR`         | Is the bar of a user task a header or a footer?                                                                                                                              | header |
| `Q-TASK-BAR-INFO`    | Which information does the bar show? For example task kind, case kind, case number.                                                                                          | task title, case title, business id |
| `Q-TASK-BAR-ACTIONS` | Which actions does the bar offer? For example claim, follow-up, note, mark as unread, open as own window.                                                                    | claim, follow-up, mark as unread, open as own window |
| `Q-OWN-WINDOW`       | Is there "open as own window"? It hides the application header and opens the list in a new tab, so a form being filled in stays open.                                        | yes |
| `Q-INFO-COLUMN`      | Does the cockpit reserve a column on the right of the form for basic information at the top and the completing actions at the bottom? Or is the whole area left to the form? | the whole area is left to the form, see [behaviour.md](behaviour.md#opening-and-working-on-a-user-task) |

## Live updates

| Id            | Question                                                                                                                                                                                                                                                                                                                                                                                            | Default the agent proposes |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-LIVE-MODE` | How do lists react to changes? `live`: rows update and are highlighted until the user acknowledges them with a button. `auto-acknowledge`: like `live`, but the highlight of a row goes away a few seconds after the mouse leaves it; this is an experiment to be validated. `refresh-button`: nothing updates by itself, a refresh button in the header becomes active when changes were reported. | `live` |

## Language

| Id                   | Question                                                    | Default the agent proposes |
|----------------------|-------------------------------------------------------------|----------------------------|
| `Q-LANGUAGE-DEFAULT` | Which language is the default?                              | English |
| `Q-LANGUAGES`        | Which other languages are offered?                          | none |
| `Q-LANGUAGE-SWITCH`  | Can the user switch the language? Where is the choice kept? | no |

## Extension points

| Id                   | Question                                                                                                                                                                                                                                                   | Default the agent proposes |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-EXTENSION-POINTS` | Which extension points exist beyond the four minimum parts (user task form, workflow page, columns of the task list, columns of the workflow list)? For example buttons in the header delivered by a module, menu entries and pages delivered by a module. | header buttons |
| `Q-CELL-RENDERING`   | Shall workflow modules draw cells of a list themselves? This is an open question and not generated yet, see [module-federation.md](module-federation.md#cells-drawn-by-a-workflow-module).                                                                 | no |
| `Q-UI-URI-TYPE`      | Which value of `uiUriType` do the workflow modules report for parts loaded through native federation?                                                                                                                                                      | `NATIVE_MF` |

## Backend and outputs

| Id                   | Question                                                                                                                                                           | Default the agent proposes |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `Q-BACKEND`          | Which backend serves the user interface? The runnable `container` jar, or a Spring Boot application of the organisation's own with the `business-cockpit` library. | an application of the organisation's own |
| `Q-LOGIN`            | How does a user log in? For example a login form of the backend, or OAuth2 through `/login`. Where does the user interface send a browser that has no login?       | `/login` |
| `Q-COCKPIT-VERSION`  | Which version of the Business Cockpit does it run against? The GUI API description is taken from that version.                                                     | the latest release |
| `Q-UI-JAR`           | Maven coordinates of the UI jar.                                                                                                                                   | `<organisation group>:business-cockpit-ui:<version>` |
| `Q-RUNNABLE-JAR`     | Shall a runnable cockpit jar with the user interface inside be built as well? Which coordinates?                                                                   | yes, if `Q-BACKEND` is the `container` jar |
| `Q-MAVEN-REPOSITORY` | Where are the jars published?                                                                                                                                      | GitHub Packages of the organisation |
| `Q-NPM-SCOPE`        | The npm scope and names of the published libraries (types library, dev shells), and the npm registry.                                                              | `@<organisation>/bc-types`, `@<organisation>/bc-dev-shell-<framework>` |
| `Q-E2E-TOOL`         | Which tool runs the end-to-end tests?                                                                                                                              | Playwright |
