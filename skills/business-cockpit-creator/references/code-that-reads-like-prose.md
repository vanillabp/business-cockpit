# Code that reads like prose

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The people who keep a generated cockpit running did not write it. They come to one file to fix one
bug, and they do not know the rest. So the code has to explain itself where they land. These rules
hold for everything you generate: TypeScript, the code of the frameworks, Java, the tests, the
scripts, the comments, the log messages and the `README.md` files.

The rules are the ones the VanillaBP blueprints follow, see
[Writing style](https://github.com/vanillabp-blueprints/blueprints/blob/main/CONTRIBUTING.md#writing-style)
and [How we write](https://github.com/vanillabp-blueprints/blueprints/blob/main/AGENTS.md#how-we-write)
in the blueprints repository.

**Contents:**

1. [Names read like small sentences](#names-read-like-small-sentences)
2. [A name says what a thing stands for](#a-name-says-what-a-thing-stands-for)
3. [A comment says why](#a-comment-says-why)
4. [Nothing points at the plan](#nothing-points-at-the-plan)
5. [English for second-language readers](#english-for-second-language-readers)
6. [Nothing a program reads is renamed](#nothing-a-program-reads-is-renamed)
7. [The checks before each commit](#the-checks-before-each-commit)

## Names read like small sentences

A function name says what the function does. A call then reads like a short sentence about the
cockpit, and the body of a function, with its comments, reads like a short text. `loadNextPage`,
`claimTask` and `showsOnlyMatchingRows` are names. `handle`, `process` and `doUpdate` are not.

A test says which behaviour it proves. An end-to-end test starts with the checklist id it proves,
then says the behaviour, for example `TL-003 each primary filter shows its count`.

## A name says what a thing stands for

Name a variable, a field or a parameter after the role it plays, not after its type. A type name
inside a name is the sign to look for, and so are filler words such as `data`, `result`, `value`,
`list`, `obj` and `tmp`.

The generated GUI API client follows the names of the OpenAPI file. Name the instance after what it
does for the page:

```typescript
// not like this
const userTasksApi = new UserTasksApi(configuration);
const result = await userTasksApi.getUserTasks({ ... });

// like this
const userTasks = new UserTasksApi(configuration);
const openTasksOfUser = await userTasks.getUserTasks({ ... });
```

The same holds for Java in the backend repository. A bean is named after its role in the class:

```java
@Autowired
private UserDirectory people;

public CurrentUser currentUser(final Authentication login) {
  return people.findByLogin(login.getName());
}
```

If a method name would repeat the field, drop that part: `people.findByLogin(...)`, not
`userDirectory.findUserInUserDirectory(...)`.

The one exception: two variables of the same type that stand for different things. Even there,
name the roles, for example `taskBeforeChange` and `taskAfterChange`.

## A comment says why

A comment that repeats what the line says is noise. Leave it out. A comment that explains why the
obvious way is not the one taken is the reason comments exist.

Before you write a comment, try a better name. A name that carries the meaning is better than a
comment next to a bad name.

A comment must be complete where it stands. "Same reason as above" and "see the other module" are
not complete, because code moves and the other place changes.

```typescript
// not like this
// set the timestamp
query.timestamp = firstPageTimestamp;

// like this
// Every page asks with the timestamp of the first page. Otherwise a task that is created while the
// user scrolls moves all later rows down by one, and the user sees one row twice.
query.timestamp = firstPageTimestamp;
```

## Nothing points at the plan

The code, the tests, the comments and the texts of the cockpit never name a story id such as
`S04`, a question id such as `ASK-003`, a ticket, an issue, a pull request, a chat or a person.
Those record a moment in time. A later change can make them wrong, and nothing in the build
notices. Where the reason matters, write the reason itself in your own words.

The checklist ids such as `TL-003` and the interview ids such as `Q-HOSTING` are allowed. They
are stable, they are never reused, and the project keeps them in `CHECKLIST.md` and
`INTERVIEW.md`.

A commit message may name the story id. A commit is itself a record of a moment in time.

## English for second-language readers

Most people who read the code read English as a second language. Write so that nobody has to read a
sentence twice.

- Short main sentences, one thought each. One subordinate clause is enough.
- Active voice: "The loader asks the registry", not "The registry is asked".
- The common word instead of the rare one: `use`, not `leverage`; `about`, not `regarding`; `so`,
  not `consequently`; `before`, not `prior to`.
- Not three or more nouns in a row, such as `module part load state handler`.
- Write an abbreviation out the first time, and say what a technical term means the first time it
  turns up.
- No em dashes, no lists whose items start with a bold label, no phrases about how important
  something is.

## Nothing a program reads is renamed

These rules are for the names you choose and the sentences you write. Nothing a program reads is
renamed for the sake of language: the paths and fields of the GUI API, the export names of the
module kit, the names of npm packages and Maven artifacts, configuration keys, environment
variables, file names that other code loads, and the checklist ids.

## The checks before each commit

Run these on the change of the story, after `git add -A`. They leave out the folder `plan/`,
because the plan names its stories on purpose.

```bash
git diff --cached -- . ':(exclude)plan' | grep -niE '^\+.*\b(story|prompt|issue|ticket|jira)s? ?#?[0-9]+'
git diff --cached -- . ':(exclude)plan' | grep -nE '^\+.*\b(S[0-9]{2}|ASK-[0-9]{3})\b'
git diff --cached -- . ':(exclude)plan' | grep -nE '^\+' | grep -oE '[^.!?]{160,}'
git diff --cached -- . ':(exclude)plan' | grep -niE '^\+.*\b(leverage|utilize|regarding|consequently|possesses|facilitate|prior to|aforementioned|thereby|whilst|in order to)\b'
git diff --cached -- . ':(exclude)plan' | grep -nE '^\+.*\b(const|let|var|private|final) [A-Za-z<>]* ?[a-z][A-Za-z]*(Api|Service|Repository|Data|Result|Value|List|Obj|Tmp)\b'
```

Each line that a command prints is a place to read again. Fix the place, or leave it when the rule
does not apply there, for example a quoted error message. The commands do not find everything, so
also read each new name and each new comment once.
