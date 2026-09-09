# Business Cockpit extension - Commons - Core

Everything the Business Cockpit extension does without knowing a BPMS or a platform. What it is and
how an event travels through it is described once, in the
[parent's README](../README.md).

This page says where to put something new.

|   Package    |                                      What belongs there                                      |
|--------------|----------------------------------------------------------------------------------------------|
| `spi`        | What a BPMS half implements and calls. A published contract, changed deliberately            |
| `config`     | The property keys, the typed settings and the validation which reports every gap in one boot |
| `event`      | The two event classes and the registration of a workflow module                              |
| `handler`    | The contracts telling VanillaBP how to find and invoke the details providers                 |
| `templating` | The renderer of the titles, and the fallback to the names written in the BPMN                |
| `transport`  | The two ways to the cockpit server, and the mappers to what each of them sends               |
| `outbox`     | The three operations and the keys they are deduplicated by                                   |
| `service`    | The `BusinessCockpitService` a workflow service injects                                      |
| `wiring`     | The extension's place in VanillaBP's deployment pipeline                                     |

The one class holding the pieces together is `BusinessCockpitExtension`. It is also the
`BusinessCockpitEventPublisher` a BPMS half injects, so that there is one bean rather than two which
have to be told apart.

Three dependencies are optional on purpose: the protobuf messages and the Kafka client, which an
application reporting over REST never loads, and Freemarker, which an application reporting the
names written in its BPMN never loads. Choosing one of them without its dependency is reported while
the application starts, naming the artifact to add.
