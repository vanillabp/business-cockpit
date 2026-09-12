# OpenAPI generator fixes

Mustache templates which replace parts of what the OpenAPI generator produces for the interfaces
below [apis](../apis). They exist where the generator's own output was not what the cockpit needs,
the TypeScript fetch client above all.

It is a dependency of the generating modules rather than something published for use elsewhere.
Changing a template here changes generated code in several modules at once, so build them after
touching it.
