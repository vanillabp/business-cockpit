package io.vanillabp.cockpit.users.model;

/**
 * A group of users, as it is stored in a user task or a workflow. Besides the id it holds the text
 * a search looks at and the text a list sorts by.
 */
public class Group {
    private String id;
    private String fulltext;
    private String sort;

    /**
     * Creates an empty group. The application's mapper fills it.
     */
    public Group() {

    }

    /**
     * The id of the group.
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the group.
     *
     * @param id The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The text a search for the group looks at, usually the name.
     *
     * @return The text
     */
    public String getFulltext() {
        return fulltext;
    }

    /**
     * Sets the text a search for the group looks at.
     *
     * @param fulltext The text
     */
    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    /**
     * The text a list sorts by when it is sorted by this group.
     *
     * @return The text
     */
    public String getSort() {
        return sort;
    }

    /**
     * Sets the text a list sorts by.
     *
     * @param sort The text
     */
    public void setSort(String sort) {
        this.sort = sort;
    }
}
