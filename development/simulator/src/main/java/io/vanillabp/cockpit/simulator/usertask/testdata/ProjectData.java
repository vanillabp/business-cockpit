package io.vanillabp.cockpit.simulator.usertask.testdata;

/**
 * Random project data which the generator puts into the details of a user task, under the key
 * {@code project}.
 */
public class ProjectData {

    private String projectPk;

    private String name;

    /**
     * Creates empty project data.
     */
    public ProjectData() {
    }

    /**
     * The primary key of the project, a random string of 12 characters.
     *
     * @return The primary key
     */
    public String getProjectPk() {
        return projectPk;
    }

    /**
     * Sets the primary key of the project.
     *
     * @param projectPk The primary key
     */
    public void setProjectPk(String projectPk) {
        this.projectPk = projectPk;
    }

    /**
     * The name of the project, a few random words.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the project.
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }
}
