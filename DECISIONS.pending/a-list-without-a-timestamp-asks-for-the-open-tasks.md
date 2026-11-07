# A list of user tasks without a timestamp asks for the open tasks, as the count does

This entry adds to decision 72. Decision 72 says that the count puts in no "now" when the client
sends no `initialTimestamp`, "unlike the list". Now the list does the same. Whoever merges this
entry adds a note to decision 72 which points here, the way decision 70 carries a note about
decision 79, and leaves the rest of decision 72 as it is.

Decided on 2026-11-07 for story 1533, on the default of its prompt.

## What was found

Without `initialTimestamp`, `POST /usertask` and `PUT /usertask` put in "now". The query then reads
"not ended, or ended after now", and MongoDB reads the index entries of every task the user may see,
the ended ones included. The count asks for "not ended" only and lets MongoDB narrow down with
`endedAt` inside each index of the visibility.

`CountingMeasurement` measured the first page of the list as well, on 2026-11-07, MongoDB 8.2, with
the data of decision 72 and the indexes of the visibility. For the main list of one user, mode
`OpenTasksWithFollowUp`, page size 10:

|      query of the list      |        without a timestamp         |      with a timestamp      |
|-----------------------------|------------------------------------|----------------------------|
| count, keys / docs examined | 456 / 904                          | 3339 / 6649                |
| find, keys / docs examined  | 456 / 904                          | 948 / 948                  |
| find, plan                  | `OR` of five `IXSCAN`, then `SORT` | `IXSCAN` of the sort index |

A list runs both, so a first page read 912 keys instead of 4287. Time was 1 ms against 3 to 4 ms.
The other open modes looked the same. `All` puts no condition of the timestamp into the query, so
it did not change. Neither did `ClosedTasksOnly` in this measurement. It now takes the tasks which
ended before the timestamp, which is one condition and no "or".

## The decision

`POST /usertask` and `PUT /usertask` pass a missing `initialTimestamp` on to the service as missing.
The service then asks for the tasks which have not ended, for the open modes, and for every ended
task, for `ClosedTasksOnly`. The answer still carries the time of the server in `serverTimestamp`,
as before. A client which sends it back with the next page or the update gets the same tasks.

The one difference is a task which ends in the instant between taking the time and reading the
tasks. The first page leaves it out, and a request with the timestamp shows it as open. That window
is a few milliseconds, and the count has the same window since decision 72.

## Where it lives

- `AbstractUserTaskListGuiApiController.getUserTasks` and `getUserTasksUpdate`, which pass the
  timestamp on unchanged and answer with "now".
- `UserTaskService.buildUserTasksCriteria`, which reads a missing timestamp.
- `AListWithoutATimestampTest`, which holds the controller to it.
- `WhatAListShowsForItsModeTest#theUpdateWithTheTimestampOfTheFirstPageKeepsATaskWhichEndedInBetween`,
  which holds the first page and its update together.
- `CountingMeasurement`, which measures the plans of the count and of the first page.

