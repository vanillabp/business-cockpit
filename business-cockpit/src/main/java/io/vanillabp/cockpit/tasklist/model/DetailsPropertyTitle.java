package io.vanillabp.cockpit.tasklist.model;

import java.util.Map;

public class DetailsPropertyTitle {

    private String path;

    private Map<String, String> title;

    private Boolean showAsColumn;

    private Boolean sortable;

    private Boolean filterable;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public Boolean getShowAsColumn() {
        return showAsColumn;
    }

    public void setShowAsColumn(Boolean showAsColumn) {
        this.showAsColumn = showAsColumn;
    }

    public Boolean getSortable() {
        return sortable;
    }

    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    public Boolean getFilterable() {
        return filterable;
    }

    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }
    
}
