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
3. [Planning: from the checklist to stories](#planning-from-the-checklist-to-stories)
4. [Working on one story](#working-on-one-story)
5. [Questions never block](#questions-never-block)
6. [Update jobs](#update-jobs)
7. [Templates](#templates)

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

## Planning: from the checklist to stories

Plan after the interview and before the first line of code.

1. Write `CHECKLIST.md`, as [Phase 2](../SKILL.md#phase-2-generate) of the skill says.
2. List the ids that the project has to prove. These are all items that are not marked "not chosen"
   or removed:

   ```bash
   awk -F'|' '/^\| `[A-Z0-9]+-[0-9]+`/ && !/not chosen/ && !/removed/ {gsub(/[ `]/, "", $2); print $2}' CHECKLIST.md > plan/ids.txt
   wc -l plan/ids.txt
   ```
3. Cut the work into stories. Follow [The order of building](../SKILL.md#the-order-of-building), from
   step 1 to step 10. Each step of it becomes one or more stories. Give every id of `plan/ids.txt`
   to exactly one story: the story that writes the test for it. Ids with the same prefix and the
   same screen usually belong together, for example `TL-001` to `TL-005` for the task list.
4. Keep each story small. A story has at most about eight checklist ids, changes at most about ten
   files, and works in one repository. A good size is one screen or one endpoint with its tests. If a
   story is bigger, split it.
5. Some stories prove no id, for example the skeleton of the project. That is fine. Their check is a
   build.
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
   git commit -m "S04 Task list with the primary filter (TL-001, TL-002, TL-005)"
   ```

   A story of the backend repository makes two commits with the same message: the code in the
   backend repository, and `plan/` in the UI repository.

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

**A newer reference.** Do the steps of
[Upgrading a generated user interface](../SKILL.md#upgrading-a-generated-user-interface). Then plan:

1. One story for the new interview questions, if there are any. Its check is a `grep` for the new
   question ids in `INTERVIEW.md`.
2. One story per group of new or changed ids, as in the planning of a new cockpit.
3. One story for the removed ids: mark them in `CHECKLIST.md` and delete or change their tests.
4. The same two last stories as for a new cockpit.

**A new wish of the customer.** Add the `CUST-` items to `CHECKLIST.md` first. Then plan stories for
them, as for a new cockpit.

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

| Id  | Title                                    | Status      | Checklist ids                        | Check command            |
|-----|------------------------------------------|-------------|--------------------------------------|--------------------------|
| S01 | Project skeleton and generated client    | done        | PKG-002, PKG-006, PKG-014            | `sh plan/checks/S01.sh`  |
| S02 | UI jar and its upload                    | in-progress | PKG-003, PKG-011                     | `sh plan/checks/S02.sh`  |
| S03 | Backend application with login           | open        | BE-001, BE-002, AUTH-001             | `sh plan/checks/S03.sh`  |
| S04 | Task list with the primary filter        | open        | TL-001, TL-002, TL-005               | `sh plan/checks/S04.sh`  |
```

The ids in this example only show the form. Take the ids from your `CHECKLIST.md`.

### A story file, plan/S04.md

```markdown
# S04 Task list with the primary filter

Repository: ui
Checklist ids: TL-001, TL-002, TL-005
Depends on: S03

## Goal

The task list shows the user tasks of the logged-in user. The primary filter shows its counts and
switches between open and closed tasks.

## Read first

- business-cockpit-creator, references/behaviour.md, section "The task lists"
- business-cockpit-creator, references/gui-api.md, section "The primary filter: which rows come at all"
- CHECKLIST.md, the lines of TL-001, TL-002 and TL-005

## Files

- packages/ui/src/tasks/TaskList.vue (new)
- packages/ui/src/tasks/PrimaryFilter.vue (new)
- e2e/task-list.spec.ts (new)

## Steps

- [ ] 1. Write the test "TL-001 the task list shows the open tasks of the user" in
      e2e/task-list.spec.ts. Run the check. It fails.
- [ ] 2. Write TaskList.vue until the test passes.
- [ ] 3. Write the tests for TL-002 and TL-005.
- [ ] 4. Write PrimaryFilter.vue until all three tests pass.
- [ ] 5. Run `sh plan/checks/S04.sh`. It passes.

## Notes

(Written while working: what is done, what was tried, what is left.)
```

Write the steps so concrete that a weaker model can do each of them without thinking about the
whole cockpit. One step is one file or one test, not "build the list".

### A check script, plan/checks/S04.sh

```sh
#!/bin/sh
set -e
npx playwright test --grep "TL-001|TL-002|TL-005"
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

A check of a story in the backend repository:

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
