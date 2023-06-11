![](./readme/vanillabp-headline.png)

*Vanilla BP* is an aspect orientated service provider interface (SPI) for workflow systems as a Java developer would expect it to be. **This is an extension of the SPI for user task applications.**

The SPI can be used by user task application vendors to collect details about a user task which may be used in the user task application. By using this SPI the business code is fully decoupled from the user task application.
  
## Content

1. [How it looks like](#how-it-looks-like)

## How it looks like

This is a section of a taxi ride workflow and should give you an idea of how the Vanilla BP SPI for user task applications is used in your business code:

```java
@Service
@WorkflowService(workflowAggregateClass = Ride.class)
public class TaxiRide {

    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final Ride ride,
            final PrefilledUserTaskDetails userTaskDetails) {
            
        userTaskDetails.setComment("comment-template");
            
    }

}
```

## Usage

### Using an adapter

Vendors of user task applications can provide an adapter which uses this SPI to retrieve details about the user tasks currently shown in the user task application. Those adapters will react to BPMN user tasks created, changed, completed or cancelled by the underlying Business Process Management System (BPMS). An adapter will provide defaults for some of those details but they may be modified or enriched by the business software.

### Wire up a user task

To modify or enrich user task details the workflow's service class has to provide a custom method for each individual user task. The return-value's class has to implement the interface `io.vanillabp.spi.cockpit.usertask.UserTaskDetails`.

```java
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment() {
        var details = new MyUserTaskDetails();
        ...
        return details;
    }
```

All non-null attributes provided by `io.vanillabp.spi.cockpit.usertask.UserTaskDetails` returned by the method will override default values provided by the adapter implementation.

### Process-specific workflow-aggregate

Typically, user task details are based on attributes of the [workflow-aggregate](https://github.com/vanillabp/spi-for-java/blob/main/README.md#process-specific-workflow-aggregate). One may define a parameter to get the current aggregate passed to the method:

```java
    @UserTaskDetailsProvider
    public UserTaskDetails retrievePayment(
            final Ride ride) {
        var details = new MyUserTaskDetails();
        details.setComment(ride.toString());
        return details;
    }
```

### Using templates for details

