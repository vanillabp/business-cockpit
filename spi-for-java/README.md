![](../readme/vanillabp-headline.png)

# SPI for Java

The annotations and interfaces a workflow module's business code is written with to say what its
user tasks and its business cases are about. It is an extension of
[VanillaBP's own SPI](https://github.com/vanillabp/spi-for-java) for user task applications, and it
is published as `io.vanillabp.businesscockpit:spi-for-java`.

How these are used is in the wiki, under
[Reporting workflows and user tasks](https://github.com/vanillabp/business-cockpit/wiki/Reporting-workflows-and-user-tasks)
and [Templates](https://github.com/vanillabp/business-cockpit/wiki/Templates). This file is about
the module.

**Contents:**

1. [What is here](#what-is-here)
1. [What a change to it costs](#what-a-change-to-it-costs)
1. [Building it](#building-it)

## What is here

Everything lives below `io.vanillabp.spi.cockpit`, and the module depends on nothing but the JDK, so
that a workflow module can compile against it without inheriting anything.

| Package | What it holds |
|---|---|
| `usertask` | `@UserTaskDetailsProvider` and its repeatable container, the details a method returns, the prefilled details it is handed, and the notification settings |
| `workflow` | `@WorkflowDetailsProvider` and the same pair of details interfaces for a business case |
| `details` | `@DetailsEvent`, the parameter saying which lifecycle event caused the call |
| `workflowmodules` | `WorkflowModuleDetailsProvider`, the bean saying which groups may see a workflow module |
| (root) | `BusinessCockpitService`, which a workflow service injects to report a change of its own |

The `version` attribute of `@UserTaskDetailsProvider` is reserved and refused at startup. Version 1
documented it and never read it, so applications wrote it believing it worked; refusing it is the
only answer which tells them. Its Javadoc carries the reasoning.

## What a change to it costs

This module is compiled against by every application using the cockpit, and it is read by
`extensions-commons` and, through it, by the three BPMS halves in their own repositories. An
addition is cheap, a rename is not, and a change to what a details provider may declare as a
parameter has to be followed in `BusinessCockpitHandlers` of `extensions-commons`.

## Building it

Nothing special; it is part of the reactor build described in the
[root README](../README.md#building-it).

```sh
mvn -pl spi-for-java -am install
```
