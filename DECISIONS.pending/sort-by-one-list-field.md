### A sort names at most one field which holds a list, and a write an index cannot take fails every time

Decision 57 creates an index for each new combination of sort paths. Some fields of the lists hold
a list themselves: `candidateUsers` and `candidateGroups` of a user task, `accessibleToUsers` and
`accessibleToGroups` of a workflow. MongoDB indexes at most one field which holds a list per
document in an index over several fields. Measured with MongoDB 8.2, the image the tests of this
repository use:

- On an empty collection MongoDB creates an index over `candidateUsers` and `candidateGroups`. After
  that it refuses every document which holds both fields with code 171 (`CannotIndexParallelArrays`),
  even when both lists are empty.
- On a collection which holds such a document, MongoDB refuses the index with the same code. The
  cockpit logs that and sorts without the index.
- An index over two paths into the same list, like `candidateUsers.sort,candidateUsers.id`, works.

So a sort names at most one of these fields, and a write MongoDB refuses with code 171 counts as
one which fails every time.

#### What the cockpit does now

- A sort which names two different fields which hold a list is answered with 400, before any index
  is built: `'sort' may name only one of the fields 'candidateGroups' and 'candidateUsers' at a time,
  because each of them holds a list` (for workflows `'accessibleToGroups' and 'accessibleToUsers'`).
  The text is written for a user, because the user interface shows it as it is. Several paths into
  the same field stay allowed, which is what a column of persons sends. Keys below `details.` cannot
  be known to hold a list, so for them the limit of five paths stays the only one.
- A write which MongoDB refuses with code 171 counts as one which fails every time
  (`OutcomeOfStoring`). REST answers 422 and Kafka skips the record, instead of repeating it
  without end. The reason says to drop the index whose name starts with `_sort_`. Such an index can
  still exist from before this change.

#### What stays as it was

Both lists add their default order behind the requested paths, so paging stays stable: user tasks
`dueDate`, `createdAt`, `_id`, workflows `createdAt`, `_id`. A field the request named is left out of
that addition. The index of a sort covers the requested paths and these fields, in this order.
