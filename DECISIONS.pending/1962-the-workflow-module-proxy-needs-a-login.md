### The proxy to the workflow modules needs the login of the cockpit

This changes decision 76. Its table names `/wm/**` among the open paths. That path is no longer
open. Mark decision 76 with an addendum which names this entry.

#### What was found

The cockpit forwards every request below `/wm/<workflow-module-id>/` to the workflow module. The
security chains of `container`, and the chain the wiki page "Security" shows, opened `/wm/**` to
everybody. The idea was that each workflow module checks the login cookie itself.

That leaves the protection to every single module. A module which checks nothing, or checks
wrongly, is open to everybody who can reach the cockpit. The files of its user interface are open
anyway. The maintainer decided on 2026-11-26 to protect the proxy, although the workflow module
checks the login once more.

#### The decision

- A request below `/wm/` needs the login of the cockpit, the same cookie the GUI API needs. A chain
  no longer opens `WebSecurityConfiguration.workflowModulesProxyRequestMatcher`. The constant stays,
  because a chain still needs it, see the next point.
- A request below `/wm/` without a login gets 401, also from a login by OpenID Connect, which sends
  a page to the provider. The user interface of the cockpit loads a module's manifest with `fetch`
  and its parts with `import()`. Neither can follow a redirect to the provider: the browser refuses
  the answer of another origin, and the user sees a broken module instead of a login. This is the
  rule the chains of `container` already have for the GUI API and the BPMS API. So the chain of
  `container` adds the matcher to `API_REQUEST_MATCHER`, which answers 401. A person who is not
  logged in opens a page of the cockpit first, and that page sends them to the login.
- The workflow module still checks the login itself, with the module `support`. The cockpit's check
  is a second wall, not a replacement. The groups of `getAccessibleToGroups` are still not checked
  at the proxy.
- A request below `/wm/` renews the login, like every other request with a valid cookie except the
  update stream.

#### What it costs

- An application which wrote its chains from the wiki page "Security" has to drop the matcher from
  its open paths, and should add it to the paths which get 401. Until it does, its proxy stays open
  as before. Nothing breaks.
- A script or a test which calls `/wm/` of a cockpit has to log in first, with the cookie or with
  the login of its chain.

