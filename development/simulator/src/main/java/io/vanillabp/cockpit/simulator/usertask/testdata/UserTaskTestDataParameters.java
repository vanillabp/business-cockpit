package io.vanillabp.cockpit.simulator.usertask.testdata;

/**
 * The parameters of the form which generates test user tasks.
 */
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

    /**
     * Creates empty parameters. Spring fills them from the form.
     */
    public UserTaskTestDataParameters() {
    }
    
    /**
     * The number of events to send, new tasks and updates together.
     *
     * @return The number
     */
    public int getNoOfEvents() {
        return noOfEvents;
    }
    
    /**
     * Sets the number of events to send.
     *
     * @param noOfEvents The number
     */
    public void setNoOfEvents(int noOfEvents) {
        this.noOfEvents = noOfEvents;
    }
    
    /**
     * The number of random users to assign tasks to.
     *
     * @return The number
     */
    public int getNoOfUsers() {
        return noOfUsers;
    }

    /**
     * Sets the number of random users.
     *
     * @param noOfUsers The number
     */
    public void setNoOfUsers(int noOfUsers) {
        this.noOfUsers = noOfUsers;
    }

    /**
     * The number of random groups to offer tasks to.
     *
     * @return The number
     */
    public int getNoOfGroups() {
        return noOfGroups;
    }

    /**
     * Sets the number of random groups.
     *
     * @param noOfGroups The number
     */
    public void setNoOfGroups(int noOfGroups) {
        this.noOfGroups = noOfGroups;
    }

    /**
     * How many of the assigned tasks also name the assignee as a candidate user, in percent.
     *
     * @return The percentage
     */
    public int getPercentageUserCandidates() {
        return percentageUserCandidates;
    }

    /**
     * Sets how many assigned tasks also name the assignee as a candidate user.
     *
     * @param percentageUserCandidates The percentage
     */
    public void setPercentageUserCandidates(int percentageUserCandidates) {
        this.percentageUserCandidates = percentageUserCandidates;
    }

    /**
     * How many tasks are offered to one or two candidate groups, in percent.
     *
     * @return The percentage
     */
    public int getPercentageGroupCandidates() {
        return percentageGroupCandidates;
    }

    /**
     * Sets how many tasks are offered to candidate groups.
     *
     * @param percentageGroupCandidates The percentage
     */
    public void setPercentageGroupCandidates(int percentageGroupCandidates) {
        this.percentageGroupCandidates = percentageGroupCandidates;
    }

    /**
     * How many tasks are assigned to a user, in percent.
     *
     * @return The percentage
     */
    public int getPercentageUserAssignments() {
        return percentageUserAssignments;
    }

    /**
     * Sets how many tasks are assigned to a user.
     *
     * @param percentageUserAssignments The percentage
     */
    public void setPercentageUserAssignments(int percentageUserAssignments) {
        this.percentageUserAssignments = percentageUserAssignments;
    }

    /**
     * How many of the events are updates of tasks created before, in percent.
     *
     * @return The percentage
     */
    public int getPercentageUpdates() {
        return percentageUpdates;
    }

    /**
     * Sets how many of the events are updates.
     *
     * @param percentageUpdates The percentage
     */
    public void setPercentageUpdates(int percentageUpdates) {
        this.percentageUpdates = percentageUpdates;
    }

    /**
     * The number of threads which send events at the same time.
     *
     * @return The number
     */
    public int getNoOfConcurrentRequest() {
        return noOfConcurrentRequest;
    }

    /**
     * Sets the number of threads which send events.
     *
     * @param noOfConcurrentRequest The number
     */
    public void setNoOfConcurrentRequest(int noOfConcurrentRequest) {
        this.noOfConcurrentRequest = noOfConcurrentRequest;
    }

    /**
     * The languages of the task titles, separated by commas, e.g. {@code en,de}.
     *
     * @return The languages
     */
    public String getLanguages() {
        return languages;
    }
    
    /**
     * Sets the languages of the task titles.
     *
     * @param languages The languages
     */
    public void setLanguages(String languages) {
        this.languages = languages;
    }
    
    /**
     * The pause after each event, in milliseconds.
     *
     * @return The pause
     */
    public int getThrottling() {
        return throttling;
    }
    
    /**
     * Sets the pause after each event.
     *
     * @param throttling The pause
     */
    public void setThrottling(int throttling) {
        this.throttling = throttling;
    }
    
}
