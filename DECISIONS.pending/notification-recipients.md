### A notification goes to a user the directory knows and who used the cockpit recently, at one mailbox

A user gets a document in the collection `users` at the first login, and nothing deletes it. So the
document alone does not say whether the user may still read about the tasks. The user directory
says that, and the time of the last use says whether anybody still uses the account.

#### What the cockpit does now

- The poller plans a notification only for a user whom the user directory knows
  (`UserDirectoryService.getUser`) and who used the cockpit within
  `business-cockpit.notification.recipients-active-within`, 90 days by default. This holds for
  every kind of notification: a task which addresses nobody, a personal candidate, the assignee at
  the end of a task, and a group. A task which addresses nobody stays visible to every user, but
  only these users get a mail about it.
- The time of the last use is `lastLoggedIn` of the user document. Every request of a logged-in user
  refreshes it, at most once per window and node. A user who was away longer is notified again after
  the next login. A document without `lastLoggedIn` counts as not used.
- The poller asks MongoDB only for the users who were active within that time, and only for the
  notification configuration. It asks the directory once per such user and cycle, which the
  directory answers from its cache.
- The address a user sets for the mails has to be one mailbox, like `jane@example.com` or
  `Jane Doe <jane@example.com>`. A list of addresses, a group like
  `team: a@example.com, b@example.com;` and an address without a domain are refused with 400. A
  mail server delivers a group to each of its members. An address of that form which is stored
  already gets no mail. The cockpit logs a warning which names the user, and the delivery does not
  count as failed, so it is not repeated.

#### Why both checks

The directory check alone misses an account which the identity provider disabled and the directory
still returns, as LDAP does for a locked account. The time of the last use alone would keep mailing a
removed user for up to its length. Together a removed user gets no mail from the next cycle on, as
soon as the cache of the directory has noticed the change, and an account nobody uses stops after
the time set.

#### What stays as it was

- A self-set address is not confirmed by a mail to it. The user is logged in when setting it.
- A queued notification is sent with the task as it is at the time of sending.
- The setting has to be a positive duration. Any other value ends the start with a message which
  names the key.

