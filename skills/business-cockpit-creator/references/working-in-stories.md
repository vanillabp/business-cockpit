# Working in stories

*Part of the description in [`../SKILL.md`](../SKILL.md).*

A cockpit is too big for one session. The context of an agent fills up, a session ends, a weaker
model takes over. So you never keep the plan in the chat. You keep it in the UI repository, in
small stories, and you write down after every step what is done. Any agent can then stop at any
time, and the next one goes on from the files alone.

This holds for every job of this skill that you cannot finish with one prompt: a new cockpit, an
upgrade to a newer reference, a new wish of the customer, a fix that touches more than a few files.
If you expect more than one commit, or more than five changed files, make a plan.

**Contents:**

1. [The first step of every start: resume](#the-first-step-of-every-start-resume)
2. [The files of the plan](#the-files-of-the-plan)
3. [Stories are business slices](#stories-are-business-slices)
4. [The fixed list of slices](#the-fixed-list-of-slices)
5. [Planning: from the checklist to stories](#planning-from-the-checklist-to-stories)
6. [Working on one story](#working-on-one-story)
7. [Driving sub-agents](#driving-sub-agents)
8. [Questions never block](#questions-never-block)
9. [Update jobs](#update-jobs)
10. [Templates](#templates)

## The first step of every start: resume

Do this before anything else, every time you start, also when the user only says "go on". Run the
commands in the folder of the UI repository. If you do not know that folder, ask the user for it.

1. Look which files are there:

   ```bash
   ls INTERVIEW.md CHECKLIST.md plan/INDEX.md plan/QUESTIONS-DRAFT.md plan/QUESTIONS.md
   ```

   - No `INTERVIEW.md`: start the interview, see [interview.md](interview.md).
   - `INTERVIEW.md`, but no `plan/INDEX.md`: go on with the interview, see
     [interview.md](interview.md#going-on-later). When every question that applies has an answer,
     write the plan, see [Planning](#planning-from-the-checklist-to-stories).
   - `plan/INDEX.md` is there: go on with step 2.
2. Look for changes nobody committed, in the UI repository and, when the skill created it, in the
   backend repository:

   ```bash
   git status --short
   git -C ../<backend repository> status --short
   git log --oneline -5
   ```
3. Look for a story that is in progress:

   ```bash
   grep -nE '\| *in-progress *\|' plan/INDEX.md
   ```
4. If there is one, run its check command from the column `Check command`:
   - The check passes: the work is done and only the end of the story is missing. Do steps 6 to 10
     of [Working on one story](#working-on-one-story).
   - The check fails: open the story file `plan/<id>.md`. Read its section `Notes` and the steps
     that have no tick yet. Go on with the first step without a tick.
5. If no story is in progress, but `git status` shows changes: these changes belong to no story.
   Do not throw them away. Run `git stash push -u -m "changes found when resuming"`, write a
   question about them into `plan/QUESTIONS-DRAFT.md`, and go on.
6. If no story is in progress, take the first line with the status `open`:

   ```bash
   grep -nE -m 1 '\| *open *\|' plan/INDEX.md
   ```

   Then work on it as [Working on one story](#working-on-one-story) says.

7. If there is no open story, the job is done. If `plan/QUESTIONS-DRAFT.md` is still there, move
   its entries into `plan/QUESTIONS.md` first, see
   [At the end of the plan](#at-the-end-of-the-plan). Then tell the user, and name the questions in
   `plan/QUESTIONS.md` that still wait for an answer:

   ```bash
   grep -n '^Answer: open' plan/QUESTIONS-DRAFT.md plan/QUESTIONS.md
   ```

If two stories are in progress, take the first one and write a note about the second one into its
story file.

Tell the user in one or two sentences where you go on, for example: "S07 is in progress, its check
fails, I go on with step 3 of plan/S07.md."

## The files of the plan

All files of the plan are in the folder `plan/` of the UI repository, next to `INTERVIEW.md` and
`CHECKLIST.md`. They are committed like the code.

|           File            |                                         What it holds                                          |
|---------------------------|------------------------------------------------------------------------------------------------|
| `plan/INDEX.md`           | one line per story: id, title, status, checklist ids, check command                            |
| `plan/ids.txt`            | the checklist ids the project has to prove                                                     |
| `plan/slices.txt`         | the fixed list of slices, copied from this page                                                |
| `plan/stories.txt`        | the stories as the script cut them from the two files above                                    |
| `plan/<id>.md`            | one file per story: goal, what to read, files, steps, notes                                    |
| `plan/checks/<id>.sh`     | the check command of a story, as a small script that fails or passes                           |
| `plan/QUESTIONS-DRAFT.md` | the questions that came up while the plan runs, each with the default you took                 |
| `plan/QUESTIONS.md`       | the questions for the user: those that block, and at the end of the plan the rest of the draft |

The status of a story is one of three words: `open`, `in-progress` or `done`. `plan/INDEX.md` is the
only place that holds the status. Git holds the history: every story ends with one commit that
names its id.

The check command is a script and not a line in the table, because a test filter such as
`TL-001|TL-002` has a `|`, and a `|` breaks a Markdown table.

## Stories are business slices

A story is one step of one thing a user of the cockpit can do. You build it through all layers at
once: the backend, the user interface, the simulated workflow modules and the end-to-end tests. A
story is done when the tests of its checklist ids pass.

Never build layer by layer. A plan such as "first the whole backend, then all screens, then all
tests" runs for a long time before a user can do anything, and nobody can say whether the cockpit
is right.

The things a user can do are fixed. They are the slices of
[The fixed list of slices](#the-fixed-list-of-slices). Each slice comes from one section of the
checklist, or from a part of it, and the list orders them from the inside out:

1. The base first: the UI repository, the backend and the login. Each brings only what the slices
   after it need.
2. Then the core: the frame, the task list with its status line, and the actions on a task. A
   clerk logs in and sees the tasks waiting for them.
3. Then one more thing per slice: search, very many tasks, the parts of the modules, the page of a
   task, the case list, the page of a case, live updates.
4. Cross-cutting things last: extension points, language, the rights, notifications, the dev
   shells, the conformance test of the VanillaBP kit and the delivery.

A slice is often too big for one story. So each slice becomes one story or several, each with at
most six checklist ids. The plan takes the ids in the order the list gives them, so the same job
with the same interview always gives the same stories, whichever agent writes the plan. Plans can
then be compared, and no model has to decide how to cut.

The technical base comes only as far as the slices need it. The first slices build the npm
workspaces with the generated client, the backend application and the login. The module kit, the
module host, the theme and the update stream come with the slices that need them.

[The order of building](../SKILL.md#the-order-of-building) still says what builds on what. Use it
to find out what a story needs from the layers below. The list of slices already follows it.

The checklist ids are what a story proves. Each story names its ids, and every id belongs to exactly
one story.

### A worked example: a cockpit for a loan approval module

The organisation has one workflow module, loan approval. Its user task is "Assess risk". Most items
of the checklist apply, so the plan has about 80 stories. The numbers below assume that every
item applies, so the real plan has a few stories less. It starts like this:

| Story |                               Business slice                                |
|-------|-----------------------------------------------------------------------------|
| S01   | The UI repository builds and tests itself (1 of 3)                          |
| S02   | The UI repository builds and tests itself (2 of 3)                          |
| S03   | The UI repository builds and tests itself (3 of 3)                          |
| S04   | The backend starts and serves the user interface (1 of 3)                   |
| ...   | ...                                                                         |
| S07   | A user logs in and out (1 of 4)                                             |
| ...   | ...                                                                         |
| S15   | A clerk sees the tasks waiting for them (1 of 3)                            |
| ...   | ...                                                                         |
| S18   | A clerk switches between open and closed tasks and sees the counts (1 of 2) |

S01 to S03 bring the npm workspaces and the generated client. S04 to S06 bring the backend, which
serves the empty shell. The login comes next, then the frame and the settings, and S15 is the first
story in which a clerk sees a task. Each story writes the end-to-end tests of its own checklist ids.

## The fixed list of slices

Copy this list into `plan/slices.txt`, exactly as it stands. One line is one slice: its key, the
title of its stories, and its checklist ids in the order of building. Do not add an id, move one or
change the order. An id that does not apply to the project is left out when the stories are cut,
see [Planning](#planning-from-the-checklist-to-stories). The ids of the customer's own items,
`CUST-001` and on, go into the last slice, `customer`.

```text
project|The UI repository builds and tests itself|PKG-044, PKG-035, PKG-045, PKG-019, PKG-002, PKG-062, PKG-055, PKG-047, PKG-046, PKG-066, PKG-069, PKG-067, PKG-014, PKG-008, PKG-052, PKG-063, PKG-064, PKG-054, PKG-070, PKG-071
backend|The backend starts and serves the user interface|BE-001, BE-035, PKG-068, BE-024, BE-025, BE-002, BE-003, BE-014, BE-028, BE-012, BE-006, PKG-003, PKG-004, PKG-005, PKG-059, BE-031
login|A user logs in and out|BE-005, BE-010, BE-011, BE-007, BE-008, BE-013, BE-036, BE-017, BE-026, BE-027, BE-009, AUTH-002, AUTH-003, AUTH-004, AUTH-005, AUTH-006, AUTH-008, AUTH-001, AUTH-009, AUTH-010, AUTH-007, BE-032, BE-033, BE-034
frame|The user sees the header and the menu|NAV-006, NAV-037, NAV-009, NAV-010, NAV-011, NAV-013, NAV-029, NAV-031, NAV-032, NAV-034, I18N-001, I18N-002, I18N-004, I18N-005, I18N-006
settings|The settings of a user are kept on the server|SET-001, SET-002, SET-004, SET-005, SET-006
task-list|A clerk sees the tasks waiting for them|BE-004, NAV-001, NAV-002, TL-008, TL-012, TL-013, TL-015, TL-016, TL-018, TL-024, TL-032, COL-015, COL-017, COL-018, COL-019, COL-021, BE-020, TL-033
status-line|A clerk switches between open and closed tasks and sees the counts|TL-001, TL-002, TL-003, TL-005, TL-006, TL-009, TL-010, TL-021, TL-026, TL-028
row-looks|A clerk sees which tasks are unread, ended or suspended|TL-007, TL-023, TL-027
actions|A clerk claims, proposes, marks and puts aside a task in the list|TL-029, TL-011, TL-030, TL-014, TL-017, TL-025, ACT-001, ACT-003, ACT-004, ACT-005, ACT-006, ACT-007, ACT-008, ACT-009, ACT-010, ACT-012, ACT-013, ACT-014, ACT-015, ACT-016, ACT-017
search|A clerk searches, filters and sorts a list|SRCH-001, SRCH-002, SRCH-003, SRCH-004, SRCH-005, SRCH-006, SRCH-007, SRCH-008, SRCH-009, SRCH-026, SRCH-027, SRCH-033, SRCH-034
paging|A clerk scrolls through very many tasks|I18N-007, SRCH-010, SRCH-011, SRCH-012, SRCH-013, SRCH-014, SRCH-015, SRCH-016, SRCH-017, SRCH-018, SRCH-019, SRCH-020, SRCH-021, SRCH-022, SRCH-023, SRCH-024, SRCH-025, SRCH-028, SRCH-029, SRCH-030, SRCH-031, SRCH-032
tools|A clerk acts on several selected tasks at once|TL-019, TL-031, ACT-018
module-kit|A workflow module builds its parts against the module kit|PKG-006, PKG-009, PKG-017, PKG-020, PKG-033, PKG-039, PKG-040, PKG-041, PKG-042, PKG-043, PKG-048, PKG-049, PKG-050, PKG-051, PKG-053, PKG-056, PKG-065, PKG-057
module-loading|The cockpit loads the parts of the modules|EXT-005, EXT-007, EXT-008, EXT-009, EXT-010, EXT-011, EXT-016, EXT-019, EXT-021, EXT-026, EXT-027, EXT-029, EXT-031, EXT-033, EXT-034, EXT-035, EXT-036, EXT-037, EXT-041, EXT-043, EXT-044, EXT-045, EXT-046, EXT-047, EXT-048, EXT-049, EXT-050, EXT-051, EXT-052, EXT-053, EXT-054, EXT-055, EXT-056, EXT-057, EXT-059, EXT-061
module-columns|The lists show the columns of the modules|COL-001, COL-003, COL-004, COL-005, COL-006, COL-007, COL-008, COL-009, COL-010, COL-011, COL-012, COL-013, COL-014, COL-016, COL-020, COL-022, COL-023
task-page|A clerk opens a task and works on it in the form of the module|TL-022, ACT-019, TF-001, TF-004, TF-005, TF-006, TF-008, TF-016, TF-019, TF-020, TF-022, TF-023, TF-025, TF-026, TF-027, TF-029, TF-039, TF-040, TF-041, TF-042
task-tab|A clerk works on a task in a tab of its own and goes back to the list|TF-007, TF-017, TF-018, TF-024, TF-045
barrier|A clerk claims a task before working on it, and a suspended task waits|TF-030, TF-031, TF-035, TF-036, TF-038, TF-043, TF-044
case-list|A clerk looks up a case in the case list|NAV-003, WL-001, WL-002, WL-003, WL-004, WL-005, WL-006, WL-007, WL-008, WL-009, WL-010, WL-011, WL-012, WL-013, WL-014
case-page|A clerk opens the page of a case|NAV-004, TF-013, WP-001, WP-003, WP-007, WP-008, WP-009, WP-010, WP-011, WP-012, WP-014, WP-015
live|A clerk sees changes without a reload|TF-011, TF-037, LIVE-001, LIVE-002, LIVE-003, LIVE-004, LIVE-005, LIVE-006, LIVE-007, LIVE-008, LIVE-009, LIVE-010, LIVE-011, LIVE-012, LIVE-013, LIVE-014, LIVE-015, LIVE-016, LIVE-017, LIVE-018, LIVE-019, LIVE-020, LIVE-021, LIVE-022, LIVE-023, LIVE-024, LIVE-025
extension-points|A module adds pages, dialogs, buttons and charts of its own|BE-030, NAV-005, NAV-033, NAV-007, NAV-008, NAV-017, NAV-018, EXT-012, EXT-013, EXT-014, EXT-017, EXT-018, EXT-023, EXT-060, EXT-024, EXT-039, EXT-042
language|A clerk works in their language, in every browser|NAV-014, NAV-015, NAV-016, NAV-028, SET-003, I18N-003, I18N-008
rights-pictures|A clerk sees their rights and the pictures of persons|PKG-058, BE-021, BE-022, BE-023, BE-029, NAV-019, NAV-020, NAV-021, NAV-022, NAV-023, NAV-024, NAV-025, NAV-026, NAV-027, NAV-030, NAV-035, NAV-036
notifications|A clerk sets up notifications and gets mails|BE-015, BE-016, TF-014, NOTI-001, NOTI-002, NOTI-003
dev-shell|A module developer works on a form in the dev shell|PKG-034, DS-001, DS-002, DS-003, DS-028, DS-004, DS-005, DS-006, DS-027, DS-007, DS-008, DS-009, DS-010, DS-011, DS-012, DS-013, DS-014, DS-015, DS-016, DS-017, DS-018, DS-019, DS-020, DS-021, DS-022, DS-023, DS-024, DS-025, DS-026
conformance|A module built against the VanillaBP kit loads in the cockpit|EXT-040
delivery|The cockpit is released and deployed|PKG-011, PKG-012, PKG-013, PKG-024, PKG-025, PKG-032, PKG-036, PKG-037, PKG-038, PKG-060, PKG-061, BE-018, BE-019, EXT-058
customer|The wishes of the customer|CUST-*
```

Every id of [reference-1.0.0.md](reference-1.0.0.md) is in exactly one line. A later version of the
reference brings its own list, with a line for each new id.

The slice `conformance` has one id, but it is a large story: it builds the conformance modules of
the VanillaBP kit into `dist/conformance` and runs their test, see
[A module built against the kit loads in your cockpit](../SKILL.md#a-module-built-against-the-kit-loads-in-your-cockpit).
Give it a time budget of its own, and never fold it into another story. It comes before the
delivery, because `EXT-058` runs the same test once more against a running cockpit.

The slice `actions` starts with the toolbar of `TL-029` and the selection of rows. Every action it
builds is a tool of this toolbar and acts on the selected rows, also when only one row is selected.
Mark as read or unread, follow-up and propose never become buttons or menus in a row. Only claim
and give back, removing a candidate and the note sit in the cells of a row, see
[behaviour.md](behaviour.md#the-tools-and-the-look-of-a-list). A row tool built early would have
to go again in a later story.

## Planning: from the checklist to stories

Plan after the interview and before the first line of code.

1. Write `CHECKLIST.md`, as [Phase 2](../SKILL.md#phase-2-generate) of the skill says.
2. List the ids that the project has to prove. These are all items whose last column, "Applies",
   does not say "not chosen" and does not start with "removed". Read only that column: the text of
   an item may hold these words too, for example a row "marked as removed" of the live updates.

   ```bash
   awk -F'|' '/^\| `[A-Z0-9]+-[0-9]+`/ {
       applies = $(NF-1); gsub(/^ +| +$/, "", applies)
       if (applies ~ /not chosen/ || applies ~ /^removed/) next
       id = $2; gsub(/[ `]/, "", id); print id
     }' CHECKLIST.md > plan/ids.txt
   wc -l plan/ids.txt
   ```
3. Copy [the fixed list of slices](#the-fixed-list-of-slices) into `plan/slices.txt`.
4. Cut the stories. The script goes through the slices in their order. Of each slice it keeps the
   ids of `plan/ids.txt`, in the order of the slice, and puts six of them into each story. A slice
   with seven to twelve ids gives two stories, and a slice without an id that applies gives none.
   The title of a story is the title of its slice, with its part when the slice gives more than one
   story, for example "A user logs in and out (2 of 4)". The script prints the lines of the table of
   `plan/INDEX.md`:

   ```bash
   awk -F'|' '
     NR == FNR { applies[$1] = 1; order[++count] = $1; next }
     {
       n = 0
       if ($3 == "CUST-*") {
         for (i = 1; i <= count; i++) if (order[i] ~ /^CUST-/) ids[++n] = order[i]
       } else {
         split($3, list, /, */)
         for (i = 1; i in list; i++) if (list[i] in applies) ids[++n] = list[i]
       }
       parts = int((n + 5) / 6)
       for (p = 1; p <= parts; p++) {
         line = ""
         for (i = (p - 1) * 6 + 1; i <= p * 6 && i <= n; i++) line = line (line == "" ? "" : ", ") ids[i]
         title = $2 (parts > 1 ? " (" p " of " parts ")" : "")
         printf "| S%02d | %s | open | %s | `sh plan/checks/S%02d.sh` |\n", ++story, title, line, story
       }
     }' plan/ids.txt plan/slices.txt > plan/stories.txt
   ```

   Do not change the cut by hand. Two agents with the same `CHECKLIST.md` get the same stories.

5. Add the last two stories after the printed ones. They are always the same:

   - "Every checklist item has a passing test": runs the whole end-to-end suite and checks that
     every id of `plan/ids.txt` names a test, see the template of the check below.
   - "Run against a running cockpit": the steps of
     [How you know it is done](../SKILL.md#how-you-know-it-is-done). It ends with
     [At the end of the plan](#at-the-end-of-the-plan): the questions left in the draft move to
     `plan/QUESTIONS.md`.
6. Write `plan/INDEX.md` with the lines of `plan/stories.txt` and the last two stories, one story
   file per line, and one check script per story. Use the [Templates](#templates).
7. Keep each story small. A story has at most six checklist ids, and it changes at most about
   eight files, in one or both repositories. When the list `Files` of a story file has more than
   eight files, split the story in two: the first half of its ids stays, and the rest goes into a
   new story with the same id and the letter `b`, for example `S17b`, in the line right after it.
   Split along the ids, never along the layers. "The backend of the list" and "the screen of the
   list" are no stories.
8. Prove that every id has its story. This prints nothing when the plan is complete:

   ```bash
   for id in $(cat plan/ids.txt); do grep -q "$id" plan/INDEX.md || echo "no story for $id"; done
   ```
9. Commit the plan:

   ```bash
   git add CHECKLIST.md plan/
   git commit -m "Plan the cockpit in stories"
   ```

A check must fail before the story is done and pass after it. A check that passes from the start
proves nothing. So a check names the tests of its ids, or the files the story creates. Playwright
fails when `--grep` finds no test, so a check over test titles fails as long as the test is
missing. With another tool from `Q-E2E-TOOL`, use its filter by test title in the same way.

A story with no id is rare. The last two stories are such stories, and so is a technical story of
an update job. Its check is a build.

Number the stories `S01`, `S02` and so on, in the order you will work on them. The script does that.
A story id is a name in the plan only. It never goes into the code, the tests or the texts of the
cockpit, see [code-that-reads-like-prose.md](code-that-reads-like-prose.md#nothing-points-at-the-plan).

## Working on one story

These are the steps for one story. Do them in this order, and do not skip one.

1. In `plan/INDEX.md`, change the status of the story from `open` to `in-progress`. Do not commit
   this change on its own. In the story file, write the time into the line `Started:`.
2. Read the story file `plan/<id>.md`. Then read the sections it lists under `Read first`, and the
   lines of its checklist ids in `CHECKLIST.md`. Do not read whole reference pages, so your context
   stays small. A rule the story needs may still stand in another section, or in another file. So
   search the whole skill for the terms the story builds, for example the names of its controls,
   its endpoints and its checklist ids, and read the paragraphs the search finds:

   ```bash
   grep -rn -i -E 'follow-up|followUpDate|TL-009' <folder of this skill>
   ```
3. Work through the steps of the story file one by one. After each step, put a tick on it
   (`- [x]`) and save the file at once. The ticks are how others see your progress. If you stop in
   the middle of a step, write into `Notes` what is done and what comes next. The line `Time budget:` of the story file, 20 minutes unless it says
   otherwise, is a checkpoint. When the time is up and the check does not pass yet, write into
   `Notes` which step you are on and what holds you up, and tell the agent that started you, or the
   user. Then go on, unless they tell you otherwise.
4. When something comes up that you have to decide and cannot find in the skill, do what
   [Questions never block](#questions-never-block) says.
5. Run the check:

   ```bash
   sh plan/checks/<id>.sh
   ```

   If it fails, fix the code and run it again. Never change a test or a check only to make it pass.

6. Read your own change once against [code-that-reads-like-prose.md](code-that-reads-like-prose.md)
   and run the checks there.

7. Read `plan/QUESTIONS-DRAFT.md`. Delete each entry this story made obsolete, for example because
   the story built what the question asked about, or because the skill answered it in a section
   the story read. For each deleted entry, write one line into `Notes` of the story file: its id
   and why it went, for example `ASK-007 deleted: the toolbar of this story answers it.`

8. In `plan/INDEX.md`, change the status from `in-progress` to `done`.

9. Commit the story, in one commit, with the story id and the checklist ids in the message:

   ```bash
   git add -A
   git commit -m "S18 A clerk switches between open and closed tasks and sees the counts (1 of 2) (TL-001, TL-002, TL-003, TL-005, TL-006, TL-009)"
   ```

   A story that changes the backend repository as well makes two commits with the same message:
   one in the backend repository, and one in the UI repository with the user interface and
   `plan/`.

10. Take the next story: go back to step 6 of
    [The first step of every start](#the-first-step-of-every-start-resume).

After each story you may stop. Stop also when the user asks you to, or when your context is nearly
full. The files hold everything the next agent needs.

## Driving sub-agents

Often one agent, the main agent, holds the plan and starts a sub-agent for each story. This section
is for the main agent. It steers and does not build: it writes no code, puts no tick on a step and
changes no test and no check. It reads `plan/INDEX.md`, the story files and `git log`, and it talks
to the sub-agent. A main agent that builds on its own fills its context with code, and then nobody
watches the sub-agent any more.

### The time budget

Every story file has the line `Time budget: 20 minutes`. Twenty minutes is the default for every
story. For a story that is clearly larger, for example the first story of a slice that brings a new
layer, the main agent may raise the budget in the story file before the story starts. It writes the
reason into `Notes`. The budget is a checkpoint, not a limit. When it is used up, the main agent
does not stop the sub-agent. It looks at the ticks and the `Notes` and decides, as [At the
checkpoint](#at-the-checkpoint) says.

### Starting a story

1. Change the status of the story to `in-progress` and write the time into the line `Started:` of
   the story file.
2. Start one sub-agent. Tell it the folder of the UI repository and the story id, and that it works
   as [Working on one story](#working-on-one-story) says: a tick after each step, saved at once, and
   a stop at the end of the story.
3. Run one sub-agent at a time. The stories build on each other.

### Watching the progress

The ticks in the story file are the progress. Count them about every five minutes, and remember the
count:

```bash
grep -c '^- \[x\]' plan/S18.md
```

- There is a new tick since the last count: the sub-agent moves on. Look again later.
- There is no new tick since the last count: ask the sub-agent for its state and for what holds it
  up. A short answer is enough: the step it is on, the obstacle, and what it tries next.
- The sub-agent is stuck when one of these holds: there is still no new tick at the next count, or
  its answer shows that it goes round in circles, for example the same
  failing test with the same fix again.

### At the checkpoint

When the time budget is used up, the main agent decides. It does not stop the sub-agent for this
alone.

- The ticks still grow, and the steps left are few: let the sub-agent go on. Write a line into
  `Notes` with the time and a new checkpoint, for example ten minutes later.
- The ticks still grow, but many steps are left: let the sub-agent finish the step it is on. Then
  ask yourself whether the story is cut too big. If it is, split the rest as a story of its own.
- The ticks stopped, or the sub-agent goes round in circles: it is stuck. Go on as the next section
  says.

### When a sub-agent is stuck

1. Stop the sub-agent.
2. Write a note into `Notes` of the story file: the time, the step, the obstacle as the sub-agent
   named it, and what it tried.
3. Then do one of two things:
   - Split the story when it is too big for one go: the obstacle is a piece of work of its own, or
     the steps without a tick need more files than the story file names. The first ids stay in the
     story with the steps that are done. The other ids go into a new story with the same id and the
     letter `b`, for example `S18b`, in the line of `plan/INDEX.md` right after it, with its own
     story file and check.
   - Start a new sub-agent on the same story with a hint when the obstacle is clear: the note, and
     one sentence about the way around, for example the section of the skill that answers it.
4. A story that gets stuck again after a hint is split. A story with one id that gets stuck again
   goes back to `open`, with a note. Write the obstacle into `plan/QUESTIONS-DRAFT.md`, as
   [Questions never block](#questions-never-block) says, and go on with the next story.

### After a story

Before the next story, check the end of this one: the status in `plan/INDEX.md` is `done`, the last
commit names the story id, and the check passes:

```bash
sh plan/checks/S18.sh
```

Running a check is steering, not building. If it fails, the story goes back to a sub-agent.

## Questions never block

When you do not know something, you do not wait for an answer. You write the question down with a
default, take the default and go on.

First make sure that the skill really has no answer. Most questions of earlier runs were answered
in a section the story did not list, or in another file. Search the whole skill, `SKILL.md` and
every page of `references/`, for the key words of the question, in more than one form:

```bash
grep -rn -i -E 'ended|endedAt|bulk' <folder of this skill>
```

Read every paragraph the search finds. Then look the question up in the public sources that
[where-to-look.md](where-to-look.md) lists: the API files, the README of the VanillaBP kit, the
blueprints and the wiki. Write a question only when neither the skill nor these sources answer it.

What may become a question:

- Only a decision the sources leave open, and one that needs knowledge of the project. A question
  with one sensible answer that needs no project knowledge is no question. Take that answer, and
  write it into `Notes` of the story file.
- Never an answer of the interview again. An answer in `INTERVIEW.md` is final, whatever value it
  holds. `com.acme` or `@acme` given in the interview is the answer, not a placeholder to replace.
  Do not ask it again in `plan/QUESTIONS.md`, in the hand-over or at the end of the work.
- A project decision the interview should have asked, but did not, is still a question. Name the
  interview question it belongs to, so a later version of the skill can ask it there.

Every entry names the sources you checked, in its line `Checked:`.

`plan/QUESTIONS-DRAFT.md` and `plan/QUESTIONS.md` hold questions only, things you need the user to
decide. A choice you made on purpose, for example another way than the skill shows, with your
reason, is no question. Write it into `Notes` of the story file, and add it to neither file.

A new question goes into `plan/QUESTIONS-DRAFT.md` first. Many questions of a run are answered by
a later story, and the draft keeps them from reaching the user. Step 7 of
[Working on one story](#working-on-one-story) reads the draft after every story and deletes what
the story made obsolete. Only a question that blocks the next story, or that the user must answer
before the work can go on, goes into `plan/QUESTIONS.md` at once. Tell the user about it.

1. Pick the default. Take the default the skill names, for example in
   [interview.md](interview.md). If the skill names none, take the choice that is easiest to change
   later and that commits the customer to nothing.
2. Add an entry to `plan/QUESTIONS-DRAFT.md`, or to `plan/QUESTIONS.md` for a question that blocks,
   see the [template](#a-question-entry). Give it the next free number over both files, `ASK-001`,
   `ASK-002` and so on. The number stays the same when the entry moves.
3. Write one sentence into `Notes` of the story file: which question, which default.
4. Go on with the default.

When the user answers later, write the answer into the entry. If it differs from the default, add a
new story at the end of `plan/INDEX.md` that changes the default into the answer. Its title names
the question, for example "Apply the answer to ASK-003".

When the answer changes an interview answer, write it into `INTERVIEW.md` as well, as
[interview.md](interview.md#going-on-later) says.

### At the end of the plan

After the last story, the story "Run against a running cockpit", move the entries still in
`plan/QUESTIONS-DRAFT.md` into `plan/QUESTIONS.md`. Group them by topic: one heading `## <topic>`
per topic, for example `## Live updates`, and the entries below it with `###`. Each entry keeps its
id, its default taken and its line `Answer: open`. Then delete the draft and commit both files:

```bash
git rm plan/QUESTIONS-DRAFT.md
git add plan/QUESTIONS.md
git commit -m "Move the open questions of the plan to the user"
```

## Update jobs

An update job works in the same way. Only the planning reads something else.

Where only the technique changes, cut a technical story. A plain raise of a version, for example of
the cockpit library or the web framework, is one technical story. Its check is the build and the
whole end-to-end suite.

### A newer reference

Do the steps of
[Upgrading a generated user interface](../SKILL.md#upgrading-a-generated-user-interface). Then plan:

1. One story for the new interview questions, if there are any. Its check is a `grep` for the new
   question ids in `INTERVIEW.md`.
2. Copy the list of slices of the new reference into `plan/slices.txt`. Write the new and the
   changed ids that apply into `plan/ids.txt`, and only those. Then cut the stories with the script
   of [Planning](#planning-from-the-checklist-to-stories), step 4. So the new ids get their stories
   by the same fixed rule.
3. One story for the removed ids: mark them in `CHECKLIST.md` and delete or change their tests.
4. The same two last stories as for a new cockpit.

### A new wish of the customer

Add the `CUST-` items to `CHECKLIST.md` first. Then write their ids into `plan/ids.txt` and cut them
with the script of [Planning](#planning-from-the-checklist-to-stories), step 4. They land in the
slice `customer`, six ids to a story.

### A finished plan

When `plan/INDEX.md` has no open story and a new job starts, move the old plan
away first. If `plan/QUESTIONS-DRAFT.md` is still there, do
[At the end of the plan](#at-the-end-of-the-plan) before. Then write the new one:

```bash
mkdir -p plan/archive/2026-11-19
git mv plan/INDEX.md plan/QUESTIONS.md plan/S*.md plan/checks plan/ids.txt plan/slices.txt plan/stories.txt \
  plan/archive/2026-11-19/
git commit -m "Archive the finished plan"
```

Use the date of the day. The archive keeps the open questions readable. The ids of the new plan
start at `S01` again.

## Templates

### plan/INDEX.md

```markdown
# Plan

Job: generate the Business Cockpit of <organisation>
Reference: 1.0.0
Started: 2026-11-19

| Id  | Title | Status | Checklist ids | Check command |
|-----|-------|--------|---------------|---------------|
| S01 | The UI repository builds and tests itself (1 of 3) | done | PKG-044, PKG-035, PKG-045, PKG-019, PKG-002, PKG-062 | `sh plan/checks/S01.sh` |
| S02 | The UI repository builds and tests itself (2 of 3) | done | PKG-055, PKG-047, PKG-046, PKG-066, PKG-069, PKG-067 | `sh plan/checks/S02.sh` |
| S17 | A clerk sees the tasks waiting for them (3 of 3) | done | COL-017, COL-018, COL-019, COL-021, BE-020, TL-033 | `sh plan/checks/S17.sh` |
| S18 | A clerk switches between open and closed tasks and sees the counts (1 of 2) | in-progress | TL-001, TL-002, TL-003, TL-005, TL-006, TL-009 | `sh plan/checks/S18.sh` |
| S19 | A clerk switches between open and closed tasks and sees the counts (2 of 2) | open | TL-010, TL-021, TL-026, TL-028 | `sh plan/checks/S19.sh` |
| S20 | A clerk sees which tasks are unread, ended or suspended | open | TL-007, TL-023, TL-027 | `sh plan/checks/S20.sh` |
```

The lines come from the script of [Planning](#planning-from-the-checklist-to-stories), step 4. Each
title is the title of its slice and says what a user can do when the slice is done. The example
shows a few lines only, and its ids assume that every item applies. Your plan takes its ids from
your `CHECKLIST.md`.

### A story file, plan/S18.md

```markdown
# S18 A clerk switches between open and closed tasks and sees the counts (1 of 2)

Repositories: ui
Checklist ids: TL-001, TL-002, TL-003, TL-005, TL-006, TL-009
Depends on: S17
Time budget: 20 minutes
Started:

## Goal

In the status line below the task list, the clerk sees the primary filters with the number of open
tasks and the legend of the looks of a row. One click switches the list to the closed tasks, another
one back, and the list opens the next time with the primary filter chosen last. The follow-up column
shows only for the primary filters with follow-up.

## Done when

The end-to-end tests of TL-001, TL-002, TL-003, TL-005, TL-006 and TL-009 pass against the
simulator. The tests of S17 still pass.

## Read first

- business-cockpit-creator, references/behaviour.md, section "The status line"
- business-cockpit-creator, references/behaviour.md, section "Counts"
- business-cockpit-creator, references/behaviour.md, section "Fixed columns"
- business-cockpit-creator, references/gui-api.md, section "The primary filter: which rows come at all"
- CHECKLIST.md, the lines of TL-001, TL-002, TL-003, TL-005, TL-006 and TL-009

## Files

- packages/ui/src/tasks/PrimaryFilter.vue (new)
- packages/ui/src/tasks/StatusLine.vue (new)
- packages/ui/src/tasks/Legend.vue (new)
- packages/ui/src/tasks/TaskList.vue (change)
- e2e/simulator/ (change: some tasks get the state closed, some a follow-up date)
- e2e/task-list.spec.ts (change)

## Steps

- [ ] 1. Write the test "TL-001 the status line offers the primary filters" in
      e2e/task-list.spec.ts. Run the check. It fails.
- [ ] 2. Give some of the tasks of the simulator the state closed, and some a follow-up date.
- [ ] 3. Write PrimaryFilter.vue and StatusLine.vue, and put the status line below the rows in
      TaskList.vue, until the test passes.
- [ ] 4. Write the tests for TL-002 and TL-003.
- [ ] 5. Change PrimaryFilter.vue until the three tests pass.
- [ ] 6. Write the test for TL-005, and keep the primary filter with the settings service until it
      passes.
- [ ] 7. Write the test for TL-006, and write Legend.vue on the right of the status line until it
      passes.
- [ ] 8. Write the test for TL-009, and show the follow-up column only for its primary filters.
- [ ] 9. Run `sh plan/checks/S18.sh`. It passes.

## Notes

(Written while working: what is done, what was tried, what is left.)
```

Write the steps so concrete that a weaker model can do each of them without thinking about the
whole cockpit. One step is one file or one test, not "build the list".

### A check script, plan/checks/S18.sh

```sh
#!/bin/sh
set -e
npx playwright test --grep "TL-001|TL-002|TL-003|TL-005|TL-006|TL-009"
```

A check of a story without ids builds and tests what the story created:

```sh
#!/bin/sh
set -e
test -f packages/bc-types/package.json
test ! -f pom.xml
npm ci
npm run build --workspaces
```

A check of a story that changes the backend repository as well:

```sh
#!/bin/sh
set -e
ui="$(pwd)"
backend="$(cd ../<backend repository> && pwd)"
# start the local registry only if it does not run yet, and stop only what this script started
if ! curl -sf http://localhost:4873/-/ping >/dev/null; then
  (cd "$backend" && docker compose up -d verdaccio)
  trap '(cd "$backend" && docker compose stop verdaccio)' EXIT
  timeout 60 sh -c 'until curl -sf http://localhost:4873/-/ping >/dev/null; do sleep 2; done'
fi
npm ci
npm run publish:snapshot
cd "$backend"
test -f application/src/test/java/<package path>/LoginIT.java
mvn -B verify
```

With Gradle (`Q-BUILD-TOOL`), the last line is `./gradlew build`. The types build reads the types
library from the registry only, see
[project.md](project.md#where-the-types-build-finds-the-types-library). So the script makes sure
the registry runs and has a snapshot of this state of the UI repository, every time, before the
backend builds. A precondition that an earlier story set up once would be gone after a restart of
the machine or `docker compose down -v`. The script starts in the folder of the UI repository. It
asks the local registry first. When it answers, somebody runs it already, and the script leaves it
alone. When it does not answer, the script starts it from the backend repository, waits until it
answers, at most a minute, and stops it again when the check ends, also when the check fails. So a
registry the developer runs stays up, and one the check started does not stay behind. Stopping
keeps its storage volume, so the published snapshots are still there the next time.
Then `npm run publish:snapshot` builds the UI repository and publishes it. With `Q-HOSTING`
`embedded` it also installs the UI jar of the snapshot into the local Maven repository, where the
module `application` finds it, see
[project.md](project.md#a-snapshot-of-the-ui-jar-goes-into-the-local-maven-repository). Every run
publishes a new snapshot counter, so the script can run any number of times.

With a registry of its own (`Q-NPM-SCOPE`), the script leaves out the block that asks for and
starts the local registry, and the token of the registry has to be in the environment. The slice `backend` puts `BE-035`,
the local registry in the `docker-compose.yaml`, right before `PKG-068`, the types build. So the
first story that builds the backend brings the registry its check needs.

The check of the story "Every checklist item has a passing test":

```sh
#!/bin/sh
set -e
for id in $(cat plan/ids.txt); do
  grep -rq "$id" e2e/ || { echo "no test names $id"; exit 1; }
done
npx playwright test
```

Ids that only a build or the backend proves, such as most `PKG-` and `BE-` ids, are named in a
comment of their check script instead of in an end-to-end test. Then add `plan/checks/` to the
`grep -r` of this loop.

### A question entry

An entry of `plan/QUESTIONS-DRAFT.md` and of `plan/QUESTIONS.md` looks the same:

```markdown
## ASK-003 Does the case list show closed cases by default?

Story: S09
Date: 2026-11-19
Default taken: no, the case list starts with the primary filter `InProgress`.
Why this default: it is the default of Q-WORKFLOW-FILTERS in the interview, and it is one setting
to change later.
Checked: behaviour.md and interview.md of the skill; the GUI API file; the wiki page Security.
Answer: open
```

Every entry has the line `Answer:`. `Answer: open` stays until the user answers. Then it holds the
answer, for example `Answer: yes, show the closed cases as well (2026-11-21)`. An entry without the
line is not found by the search for open questions in
[the first step of every start](#the-first-step-of-every-start-resume).
