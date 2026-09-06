package io.vanillabp.cockpit.tasklist;

public class UserTaskAlreadyCompletedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String userTaskId;

    public UserTaskAlreadyCompletedException(final String userTaskId) {
        super("User task '" + userTaskId + "' is already completed");
        this.userTaskId = userTaskId;
    }

    public String getUserTaskId() {
        return userTaskId;
    }

}
