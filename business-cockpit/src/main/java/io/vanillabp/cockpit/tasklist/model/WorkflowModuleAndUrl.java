package io.vanillabp.cockpit.tasklist.model;

/**
 * A workflow module and the URL it is reached at.
 */
public interface WorkflowModuleAndUrl {

    /**
     * The id of the workflow module.
     *
     * @return The id
     */
    String getId();
    
    /**
     * The URL the workflow module is reached at.
     *
     * @return The URL
     */
    String getUrl();
    
}
