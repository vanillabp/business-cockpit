package io.vanillabp.cockpit.adapter.common.usertask;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;

public class DetailsProperties implements DetailCharacteristics {

    private boolean sortable;
    
    private boolean filterable;

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }
    
}
