![](../../readme/vanillabp-headline.png)

# Dev shell simulator

The server behind the [dev shell](../dev-shell-react): it takes the reports a workflow module sends
and serves them back to the components the dev shell renders, and it supplies the users those
components are looked at as. So a workflow module's user interface can be built without a cockpit
and without a BPMS.

Running it, giving it users and pointing an application at it is in the wiki, under
[Developing UI components locally](https://github.com/vanillabp/business-cockpit/wiki/Developing-UI-components-locally#the-dev-shell-simulator).
This file is about the module.

It answers the parts of the cockpit's two interfaces the dev shell needs, which is why it is a
Spring Boot application rather than a mock inside the dev shell: what it answers has to be what the
cockpit answers, or a form works here and not there. It holds everything in H2, in memory unless a
file is configured.

Built as part of the reactor build described in the [root README](../../README.md#building-it), and
published as `io.vanillabp.businesscockpit:dev-shell-simulator`, with a runnable jar attached to a
release.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it
has benefited the community in the past.\
![Phactum](../../readme/phactum.png)

## License

Copyright 2025 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0
