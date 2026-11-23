### A user may not write the key `_class` into the user document, and the poller reads each user on its own

Spring Data writes the key `_class` into a stored document to say which Java type it holds, and it
reads the key back to create that type. The settings of a user, the notification configuration and
the recipient configurations are free JSON a user writes into the user document, so they must
not hold that key. The notification poller now reads each user on its own, so a document which
cannot be read concerns that one user only.

#### What the cockpit does now

- The key `_class` is refused with 400: as the name of a setting, at any depth inside the value of a
  setting, as a key of the recipient configuration, and as a key of the maps of the notification
  configuration.
- The poller reads each user with the one field it needs, the notification configuration, and turns
  each document into a user on its own. A document which cannot be read leaves out that one user,
  and the poller logs a warning which names the user.
- The poller sends what waits in the outbox even when planning the changes of a cycle failed.
- A check at startup looks for documents which still hold the key in one of these fields, and logs a
  warning which names each user and field. It changes nothing, so an operator decides how to remove
  the key. A changeset was not chosen, because removing a key at any depth of free JSON cannot be
  written as one update, and a changeset which guesses wrong changes data of users for good.

#### What this changes in decision 74

Decision 74 says a value of a setting is any JSON and the cockpit does not look into it. That is no
longer quite true: the cockpit looks into the value for the key `_class` and refuses it.
