package io.vanillabp.cockpit.adapter.common.usertask;

import java.util.TreeSet;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.springboot.adapter.VanillaBpProperties;

@ConfigurationProperties(prefix = VanillaBpProperties.PREFIX, ignoreUnknownFields = true)
public class UserTasksWorkflowProperties {

    private TreeSet<UserTasksProperties> workflows = new TreeSet<>();

    public TreeSet<UserTasksProperties> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(TreeSet<UserTasksProperties> workflows) {
        this.workflows = workflows;
    }

}
