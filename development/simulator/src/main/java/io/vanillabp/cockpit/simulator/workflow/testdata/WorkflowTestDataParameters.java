package io.vanillabp.cockpit.simulator.workflow.testdata;

public class WorkflowTestDataParameters {

    private int noOfEvents;

    private int percentageUpdates;

    private int noOfConcurrentRequest;
    
    private String languages;

    private int throttling;

    public int getNoOfEvents() {
        return noOfEvents;
    }

    public void setNoOfEvents(int noOfEvents) {
        this.noOfEvents = noOfEvents;
    }

    public int getPercentageUpdates() {
        return percentageUpdates;
    }

    public void setPercentageUpdates(int percentageUpdates) {
        this.percentageUpdates = percentageUpdates;
    }

    public int getNoOfConcurrentRequest() {
        return noOfConcurrentRequest;
    }

    public void setNoOfConcurrentRequest(int noOfConcurrentRequest) {
        this.noOfConcurrentRequest = noOfConcurrentRequest;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public int getThrottling() {
        return throttling;
    }

    public void setThrottling(int throttling) {
        this.throttling = throttling;
    }
}
