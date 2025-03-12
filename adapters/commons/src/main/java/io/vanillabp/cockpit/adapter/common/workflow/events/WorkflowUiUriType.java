package io.vanillabp.cockpit.adapter.common.workflow.events;



public enum WorkflowUiUriType {

    EXTERNAL("EXTERNAL"),

    WEBPACK_MF_REACT("WEBPACK_MF_REACT"),

    NF_NG("NF_NG");

    private final String value;

    WorkflowUiUriType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static WorkflowUiUriType fromValue(String value) {
        for (WorkflowUiUriType b : WorkflowUiUriType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}