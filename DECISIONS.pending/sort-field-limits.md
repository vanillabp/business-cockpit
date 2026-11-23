### The field `sort` of a list request names at most five paths in 500 characters

Decision 57 limits which paths a list may be sorted by and how many sort indexes a collection may
have. It did not limit the field `sort` itself. The rule of a path checks its characters, not its
length, and the server kept every new combination it saw in memory for as long as it ran. Now both
the field and what the server remembers have a limit.

#### What the cockpit does now

- `sort` holds at most 500 characters and names at most five paths. A longer one answers 400 before
  any of its paths is looked at. The answer does not repeat the field. The OpenAPI document of the
  GUI API says the same (`maxLength: 500`).
- `SortIndexes` remembers the combinations which have their index. They are no more than the
  indexes MongoDB takes per collection. Of the combinations left without an index, because the
  limit was reached or the index could not be created, it remembers the last 1,000 only.
- A combination is remembered only once it was dealt with. A failure to read the indexes leaves it
  for the next request.

#### What this changes in decision 57

Decision 57 says the server warns once per combination which is sorted without an index of its
own. Now it warns once while the combination is among the last 1,000 of them. A combination which
was forgotten and comes back is warned about again.
