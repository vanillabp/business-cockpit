### An assign in the GUI API names a user the directory knows, and a report still decides who sees a task which addressed nobody

An assign now names a user the directory knows, and a later report that names somebody decides
who sees a task which addressed nobody. An assign through the GUI API makes a person a candidate of
a user task, and a candidate should be somebody who can see and work on the task.

#### What the cockpit does now

- An assign asks the user directory (`UserDirectoryService.getUser`) whether it knows the id. An id
  it does not know, or no id at all, answers 400. The mapper of the application is not asked,
  because both mappers the cockpit ships turn any id into a person. An unassign is not checked, so
  a person who left the directory can still be removed.
- A task which addressed nobody and got its only candidates from an assign in the cockpit is marked
  (`UserTask.addressedByTheCockpitOnly`). Such a task still takes who sees it from the first later
  report which names somebody, as a dangling task does. The candidates of the assign are dropped
  then. A report which names nobody leaves the assign as it is. Removing the last candidate again
  makes the task dangling and clears the mark.

Ordinary assign and takeover stay as decision 54 describes them.

#### No separate check for a task visible to nobody

A task is visible to nobody only when the people it names are people nobody is. With the check of
the directory an assign always adds a person the directory knows, and taking the last candidate away
makes the task dangling again, which shows it to everybody. So the cockpit has no second check after
the change.

#### What this adds to decision 54

Decision 54 says a dangling task takes who sees it from the first later report which names
somebody. This now holds as well for a task whose only candidates came from an assign in the
cockpit while it was dangling.
