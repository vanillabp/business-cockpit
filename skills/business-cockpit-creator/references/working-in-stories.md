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
4. [Planning: from the checklist to stories](#planning-from-the-checklist-to-stories)
5. [Working on one story](#working-on-one-story)
6. [Questions never block](#questions-never-block)
7. [Update jobs](#update-jobs)
8. [Templates](#templates)

## The first step of every start: resume

Do this before anything else, every time you start, also when the user only says "go on". Run the
commands in the folder of the UI repository. If you do not know that folder, ask the user for it.

1. Look which files are there:

   ```bash
   ls INTERVIEW.md CHECKLIST.md plan/INDEX.md plan/QUESTIONS.md
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
   - The check passes: the work is done and only the end of the story is missing. Do steps 6 to 9
     of [Working on one story](#working-on-one-story).
   - The check fails: open the story file `plan/<id>.md`. Read its section `Notes` and the steps
     that have no tick yet. Go on with the first step without a tick.
5. If no story is in progress, but `git status` shows changes: these changes belong to no story.
   Do not throw them away. Run `git stash push -u -m "changes found when resuming"`, write a
   question about them into `plan/QUESTIONS.md`, and go on.
6. If no story is in progress, take the first line with the status `open`:

   ```bash
   grep -nE -m 1 '\| *open *\|' plan/INDEX.md
   ```

   Then work on it as [Working on one story](#working-on-one-story) says.

7. If there is no open story, the job is done. Tell the user, and name the questions in
   `plan/QUESTIONS.md` that still wait for an answer:

   ```bash
   grep -n '^Answer: open' plan/QUESTIONS.md
   ```

If two stories are in progress, take the first one and write a note about the second one into its
story file.

Tell the user in one or two sentences where you go on, for example: "S07 is in progress, its check
fails, I go on with step 3 of plan/S07.md."

## The files of the plan

All files of the plan are in the folder `plan/` of the UI repository, next to `INTERVIEW.md` and
`CHECKLIST.md`. They are committed like the code.

|         File          |                            What it holds                             |
|-----------------------|----------------------------------------------------------------------|
| `plan/INDEX.md`       | one line per story: id, title, status, checklist ids, check command  |
| `plan/<id>.md`        | one file per story: goal, what to read, files, steps, notes          |
| `plan/checks/<id>.sh` | the check command of a story, as a small script that fails or passes |
| `plan/QUESTIONS.md`   | the questions that came up, each with the default you took           |

The status of a story is one of three words: `open`, `in-progress` or `done`. `plan/INDEX.md` is the
only place that holds the status. Git holds the history: every story ends with one commit that
names its id.

The check command is a script and not a line in the table, because a test filter such as
`TL-001|TL-002` has a `|`, and a `|` breaks a Markdown table.

## Stories are business slices

A story is one thing a user of the cockpit can do. You build it through all layers at once: the
backend, the user interface, the simulated workflow modules and the end-to-end tests. A story is
done when a user can do this thing in the cockpit, and the tests of its checklist ids prove it.

Never build layer by layer. A plan such as "first the whole backend, then all screens, then all
tests" runs for a long time before a user can do anything, and nobody can say whether the cockpit
is right.

Cut the stories from the inside out:

1. **The core first.** The first story lets a user do the most basic thing, on the simplest path:
   log in and see the user tasks that wait for them. No filter, no sorting, no search, no live
   update.
2. **One more thing per story.** Each next story adds one thing a user can do or one case that
   differs from the simple path: open a task, look up a case, see the closed tasks, sort, search,
   work with very many tasks, see a new task without a reload.
3. **Cross-cutting things last.** Dev shells, demo data, the nginx image, extra views and the types
   jar come after the user can do their work.

The technical base comes only as far as the first story needs it. For the first story that is the
npm workspaces with the generated client, an empty shell, the backend application with its login,
and one simulated workflow module. The module kit, the module host, the theme and the update
stream come in the first story that needs them. Put the base into the first story, or into one
story right before it when it gets too big. Never spread it over more stories.

[The order of building](../SKILL.md#the-order-of-building) still says what builds on what. Use it
to find out what a story needs from the layers below. Do not use it as the order of the stories.

The checklist ids are not the cut. They are what a story fulfils. Each story names the ids that its
business slice proves, and every id belongs to exactly one story.

### A worked example: a cockpit for a loan approval module

The organisation has one workflow module, loan approval. Its user task is "Assess risk".

| Story |                          Business slice                           |
|-------|-------------------------------------------------------------------|
| S01   | A clerk logs in and sees the loan approval tasks waiting for them |
| S02   | The clerk opens a task and works on it in the form of the module  |
| S03   | The clerk looks up the state of a loan request in the case list   |
| S04   | The clerk switches to the closed tasks and sees the counts        |
| S05   | The clerk sorts and searches the task list                        |
| S06   | A clerk with 30,000 tasks still scrolls the list smoothly         |
| S07   | A new task shows up without a reload                              |
| S08   | A module developer works on a form in the dev shell               |
| S09   | Every checklist item has a passing test                           |
| S10   | Run against a running cockpit                                     |

S01 brings the skeleton, the backend with its login and a list without the columns of the modules.
It goes through both repositories. S02 brings the module host and the page of a user task. Each
story writes the end-to-end tests of its own checklist ids.

## Planning: from the checklist to stories

Plan after the interview and before the first line of code.

1. Write `CHECKLIST.md`, as [Phase 2](../SKILL.md#phase-2-generate) of the skill says.
2. List the ids that the project has to prove. These are all items that are not marked "not chosen"
   or removed:

   ```bash
   awk -F'|' '/^\| `[A-Z0-9]+-[0-9]+`/ && !/not chosen/ && !/removed/ {gsub(/[ `]/, "", $2); print $2}' CHECKLIST.md > plan/ids.txt
   wc -l plan/ids.txt
   ```
3. Cut the work into business slices, as
   [Stories are business slices](#stories-are-business-slices) says. Write down one title per story:
   what a user can do when it is done. Start with the simplest path, then one more thing per story,
   then the cross-cutting things.
4. Give every id of `plan/ids.txt` to exactly one story: the first story whose business slice needs
   it. That story writes the test for it. An id that no user notices, such as most `PKG-` ids, goes
   to the first story that needs the part it describes.
5. Keep each story small. A story has at most about eight checklist ids and changes at most about
   fifteen files, in one or both repositories. If a story is bigger, split it along the business,
   not along the layers. For example, "see the open tasks" and "see the closed tasks" are two
   stories, not "the backend of the list" and "the screen of the list".
6. The last two stories are always the same:
   - "Every checklist item has a passing test": runs the whole end-to-end suite and checks that
     every id of `plan/ids.txt` names a test, see the template of the check below.
   - "Run against a running cockpit": the steps of
     [How you know it is done](../SKILL.md#how-you-know-it-is-done).
7. Write `plan/INDEX.md`, one story file per line, and one check script per story. Use the
   [Templates](#templates).
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

A story with no id is rare, for example the base when it gets a story of its own. Its check is a
build.

Number the stories `S01`, `S02` and so on, in the order you will work on them. A story id is a name
in the plan only. It never goes into the code, the tests or the texts of the cockpit, see
[code-that-reads-like-prose.md](code-that-reads-like-prose.md#nothing-points-at-the-plan).

## Working on one story

These are the steps for one story. Do them in this order, and do not skip one.

1. In `plan/INDEX.md`, change the status of the story from `open` to `in-progress`. Do not commit
   this change on its own.
2. Read the story file `plan/<id>.md`. Then read the sections it lists under `Read first`, and only
   those. Do not read whole reference pages, so your context stays small.
3. Work through the steps of the story file one by one. After each step, put a tick on it
   (`- [x]`) and save the file. If you stop in the middle of a step, write into `Notes` what is done
   and what comes next.
4. When something comes up that you have to decide and cannot find in the skill, do what
   [Questions never block](#questions-never-block) says.
5. Run the check:

   ```bash
   sh plan/checks/<id>.sh
   ```

   If it fails, fix the code and run it again. Never change a test or a check only to make it pass.

6. Read your own change once against [code-that-reads-like-prose.md](code-that-reads-like-prose.md)
   and run the checks there.

7. In `plan/INDEX.md`, change the status from `in-progress` to `done`.

8. Commit the story, in one commit, with the story id and the checklist ids in the message:

   ```bash
   git add -A
   git commit -m "S04 The clerk switches to the closed tasks and sees the counts (TL-001, TL-002, TL-003)"
   ```

   A story that changes the backend repository as well makes two commits with the same message:
   one in the backend repository, and one in the UI repository with the user interface and
   `plan/`.

9. Take the next story: go back to step 6 of
   [The first step of every start](#the-first-step-of-every-start-resume).

After each story you may stop. Stop also when the user asks you to, or when your context is nearly
full. The files hold everything the next agent needs.

## Questions never block

When you do not know something, you do not wait for an answer. You write the question down with a
default, take the default and go on.

1. Pick the default. Take the default the skill names, for example in
   [interview.md](interview.md). If the skill names none, take the choice that is easiest to change
   later and that commits the customer to nothing.
2. Add an entry to `plan/QUESTIONS.md`, see the [template](#an-entry-of-planquestionsmd). Give it the
   next free number, `ASK-001`, `ASK-002` and so on.
3. Write one sentence into `Notes` of the story file: which question, which default.
4. Go on with the default.

When the user answers later, write the answer into the entry. If it differs from the default, add a
new story at the end of `plan/INDEX.md` that changes the default into the answer. Its title names
the question, for example "Apply the answer to ASK-003".

When the answer changes an interview answer, write it into `INTERVIEW.md` as well, as
[interview.md](interview.md#going-on-later) says.

## Update jobs

An update job works in the same way. Only the planning reads something else.

Where something changes for the user, cut business slices. Where only the technique changes, cut a
technical story. A plain raise of a version, for example of the cockpit library or the web
framework, is one technical story. Its check is the build and the whole end-to-end suite.

**A newer reference.** Do the steps of
[Upgrading a generated user interface](../SKILL.md#upgrading-a-generated-user-interface). Then plan:

1. One story for the new interview questions, if there are any. Its check is a `grep` for the new
   question ids in `INTERVIEW.md`.
2. One business slice for each thing a user can newly do or does differently, with the new or
   changed ids it proves. New or changed ids that no user notices go into one technical story.
3. One story for the removed ids: mark them in `CHECKLIST.md` and delete or change their tests.
4. The same two last stories as for a new cockpit.

**A new wish of the customer.** Add the `CUST-` items to `CHECKLIST.md` first. Then cut business
slices for them, as for a new cockpit.

**A finished plan.** When `plan/INDEX.md` has no open story and a new job starts, move the old plan
away first. Then write the new one:

```bash
mkdir -p plan/archive/2026-11-19
git mv plan/INDEX.md plan/QUESTIONS.md plan/S*.md plan/checks plan/ids.txt plan/archive/2026-11-19/
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

| Id  | Title                                                             | Status      | Checklist ids                                       | Check command           |
|-----|-------------------------------------------------------------------|-------------|-----------------------------------------------------|-------------------------|
| S01 | A clerk logs in and sees the loan approval tasks waiting for them | done        | PKG-002, PKG-003, BE-001, AUTH-001, NAV-001, TL-008 | `sh plan/checks/S01.sh` |
| S02 | The clerk opens a task and works on it in the form of the module  | done        | TF-001, TF-004                                      | `sh plan/checks/S02.sh` |
| S03 | The clerk looks up the state of a loan request in the case list   | in-progress | NAV-003, WL-001, WL-002, WP-001                     | `sh plan/checks/S03.sh` |
| S04 | The clerk switches to the closed tasks and sees the counts        | open        | TL-001, TL-002, TL-003                              | `sh plan/checks/S04.sh` |
```

Each title says what a user can do when the story is done. The ids in this example only show the
form. Take the ids from your `CHECKLIST.md`.

### A story file, plan/S04.md

```markdown
# S04 The clerk switches to the closed tasks and sees the counts

Repositories: ui
Checklist ids: TL-001, TL-002, TL-003
Depends on: S01

## Goal

Above the task list, the clerk sees the primary filter with the number of open and of closed
tasks. One click switches the list to the closed tasks, another one back.

## Done when

The end-to-end tests of TL-001, TL-002 and TL-003 pass against the simulator. The tests of S01
still pass.

## Read first

- business-cockpit-creator, references/behaviour.md, section "The task lists"
- business-cockpit-creator, references/gui-api.md, section "The primary filter: which rows come at all"
- CHECKLIST.md, the lines of TL-001, TL-002 and TL-003

## Files

- packages/ui/src/tasks/PrimaryFilter.vue (new)
- packages/ui/src/tasks/TaskList.vue (change)
- e2e/simulator/ (change: some tasks get the state closed)
- e2e/task-list.spec.ts (change)

## Steps

- [ ] 1. Write the test "TL-001 the primary filter shows the open tasks first" in
      e2e/task-list.spec.ts. Run the check. It fails.
- [ ] 2. Give some of the tasks of the simulator the state closed.
- [ ] 3. Write PrimaryFilter.vue and put it above the list in TaskList.vue, until the test passes.
- [ ] 4. Write the tests for TL-002 and TL-003.
- [ ] 5. Change PrimaryFilter.vue until all three tests pass.
- [ ] 6. Run `sh plan/checks/S04.sh`. It passes.

## Notes

(Written while working: what is done, what was tried, what is left.)
```

Write the steps so concrete that a weaker model can do each of them without thinking about the
whole cockpit. One step is one file or one test, not "build the list".

### A check script, plan/checks/S04.sh

```sh
#!/bin/sh
set -e
npx playwright test --grep "TL-001|TL-002|TL-003"
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
cd ../<backend repository>
test -f application/src/test/java/<package path>/LoginIT.java
mvn -B verify
```

With Gradle (`Q-BUILD-TOOL`), the last line is `./gradlew build`.

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

### An entry of plan/QUESTIONS.md

```markdown
## ASK-003 Does the case list show closed cases by default?

Story: S09
Date: 2026-11-19
Default taken: no, the case list starts with the primary filter `InProgress`.
Why this default: it is the default of Q-WORKFLOW-FILTERS in the interview, and it is one setting
to change later.
Answer: open
```

`Answer: open` stays until the user answers. Then it holds the answer, for example
`Answer: yes, show the closed cases as well (2026-11-21)`.
