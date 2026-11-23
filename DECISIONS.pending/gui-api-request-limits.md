### The GUI API reads a body of at most 1 MB, a note of at most 10,000 characters and at most 1,000 ids per request

Every request of the GUI API comes from a browser, and a browser sends small bodies: a list request,
a note, a setting, a list of ids. A note is stored in every entry a request names, and every list
answer carries it. So the cockpit ships limits, and an application can change the ones which are
settings.

#### Which limits

- A request body to `/gui/api/*` holds at most 1 MB. The key
  `business-cockpit.gui-api.max-request-body-size` changes it. A larger body answers 413 with a text
  which names the limit. A body which says its length is refused before it is read. A body sent in
  chunks is counted while it is read.
- The text of a note holds at most 10,000 characters. A longer text answers 400.
- A request which changes several entries at once, like the note of several cases or the claim of
  several user tasks, names at most 1,000 of them. A longer list answers 400.
- Jackson reads a string of at most 10,000,000 characters from any JSON body, the reports of the
  BPMS API included. The cockpit sets Spring Boot's key
  `spring.jackson.factory.constraints.read.max-string-length` to this value when the application does
  not set it. Jackson's own default is ten times as long.

The note and the list of ids are limited in the OpenAPI document of the GUI API (`maxLength`,
`maxItems`), so a generated client and the bean validation of the server know the same numbers. They
are no settings, because a user interface has to know them as well.

The size of a page, `pageSize` and `size`, stays without a limit, see decision 55.

#### Why the body limit is not a setting of the server

Neither Tomcat nor Spring limits the body of a JSON request by itself. A limit of the server would
hold for the BPMS API as well, and the reports of the workflow modules can carry more business data
than 1 MB. So the limit is a servlet filter for the paths of the GUI API only.

#### What this changes in decision 74

Decision 74 says the settings of a user have no limit of the cockpit, only the 16 MB of MongoDB.
That is no longer true: one setting holds at most what fits into a body of 1 MB, or whatever the
application sets. The 16 MB of the whole document still hold as well, and a write which would make
the document larger still answers 413.
