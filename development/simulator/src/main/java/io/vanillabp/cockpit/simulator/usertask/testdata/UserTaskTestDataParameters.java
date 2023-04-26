package io.vanillabp.cockpit.simulator.usertask.testdata;

public class UserTaskTestDataParameters {

    private int noOfEvents;

    private int noOfUsers;
    
    private int noOfGroups;
    
    private int percentageUserCandidates;

    private int percentageGroupCandidates;

    private int percentageUserAssignments;

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
    
    public int getNoOfUsers() {
        return noOfUsers;
    }

    public void setNoOfUsers(int noOfUsers) {
        this.noOfUsers = noOfUsers;
    }

    public int getNoOfGroups() {
        return noOfGroups;
    }

    public void setNoOfGroups(int noOfGroups) {
        this.noOfGroups = noOfGroups;
    }

    public int getPercentageUserCandidates() {
        return percentageUserCandidates;
    }

    public void setPercentageUserCandidates(int percentageUserCandidates) {
        this.percentageUserCandidates = percentageUserCandidates;
    }

    public int getPercentageGroupCandidates() {
        return percentageGroupCandidates;
    }

    public void setPercentageGroupCandidates(int percentageGroupCandidates) {
        this.percentageGroupCandidates = percentageGroupCandidates;
    }

    public int getPercentageUserAssignments() {
        return percentageUserAssignments;
    }

    public void setPercentageUserAssignments(int percentageUserAssignments) {
        this.percentageUserAssignments = percentageUserAssignments;
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
