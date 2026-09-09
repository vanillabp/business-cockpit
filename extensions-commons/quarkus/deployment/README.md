# Business Cockpit extension - Commons - Quarkus - Deployment

The build steps of the Quarkus extension: the runtime's producer becomes a bean, and the factory
building the per-aggregate `BusinessCockpitService` is named for the Jandex index, which a runtime
jar without an index of its own is not in. No VanillaBP build item is produced - an extension
announces itself by the beans it produces, unlike a BPMS adapter.

The tests of the Quarkus half live here rather than next to the runtime, because a Quarkus extension
can only be booted from its deployment module. They start an application carrying the extension,
VanillaBP's BPMS double and a BPMS half played by the test, report an event and read what arrived at
a cockpit server the test runs itself.

That server is asked over HTTP rather than through a static field. A test class of a
`QuarkusExtensionTest` runs in the application's own class loader, so a second copy of every test
class exists there with static fields of its own, and only the wire reaches both copies.
