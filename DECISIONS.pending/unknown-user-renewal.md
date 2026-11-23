### Addendum to decision 91: a login is not renewed once the directory does not know its user any more

This entry adds to decision 91. It also changes point 3 of decision 87 and point 6 of decision 90
for the renewal of a login. The rest of these entries stays as it is.

#### What was found

Decision 91 said that a locked account ends earlier than a change of its groups, because the login
is not refreshed any more once the identity provider refuses it. That was not true. The cockpit
renews its own login token. The renewal copies the claims of the old token and signs them again,
and it asks neither the identity provider nor the directory. So a user who was removed from the
directory, or disabled in Keycloak, kept the login for up to `max-login-duration`, seven days by
default. Only the groups went, once the cache of the directory forgot the user. Decisions 87 and 90
said the same thing: a user the directory does not know keeps the login.

#### The decision

1. A login is not renewed once the directory does not know its user any more, although it knew the
   user before. The login then ends when its token expires, at most
   `business-cockpit.jwt.cookie.expires-duration` later, twelve hours by default.
2. The token remembers whether the directory knew the user. The login and every renewal which finds
   the user in the directory write the claim `known_to_directory` with the value `true`. A token of
   an older version gets the claim at its first renewal.
3. `business-cockpit.jwt.cookie.unknown-user-renewal` chooses the behaviour:
   - `refuse-if-known-before`, the default: as in point 1. A user the directory never knew keeps
     the login and is renewed as before. This is the case of an application whose directory does
     not know the users by the id of the login, for example because it is not set up yet. Such an
     application keeps working, and the log warns about every unknown user, as decision 87 says.
   - `refuse`: no login of a user the directory does not know is renewed, whether it knew the user
     before or not.
   - `allow`: the behaviour before this entry. Every login is renewed up to `max-login-duration`.
4. The renewal asks no system of its own. It uses the answer of `getUser` which the request asked
   for anyway. So how fast a removed user is noticed depends on the cache of the directory. With the
   directories the cockpit brings and their defaults, the directory notices it at most 14 hours
   later, or at the clearing at 2 a.m. if that comes first. With the twelve hours of the last token,
   a removed user is logged out at most 26 hours after the change, usually on the next day. For a
   user who must go at once, operations empty the cache with the actuator endpoint
   `userdirectorycache`. Then the login ends at most `expires-duration` later.
5. A disabled user in Keycloak counts as unknown, as decision 90 says. For LDAP a user counts as
   unknown only when its entry is gone, or is no longer found below `user-search-base`. The LDAP
   directory reads no attribute which marks a locked account.
6. There is no list of single tokens which are not valid any more. A token which was handed out
   stays valid until it expires, at the cockpit and at every workflow module which checks it with
   the same key. Only a new key ends all logins at once.

#### What follows

- Only the cockpit renews tokens, so only the cockpit reads the key. A workflow module checks the
  token and never renews it. On Quarkus the key is refused with the other keys which say how the
  cookie is written.
- An application which builds its own `JwtMapper` gets the behaviour if its authentication keeps
  the user the directory gave, as `JwtAuthenticationTokenMapper` does. If it does not, every user
  counts as unknown, and the default renews as before.
- The wiki page Security says how fast a removed user is logged out.

