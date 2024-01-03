package io.vanillabp.cockpit.adapter.common.usertask.events;


public enum UserTaskUiUriType {

    EXTERNAL("EXTERNAL"),

    WEBPACK_MF_REACT("WEBPACK_MF_REACT");

    private final String value;

    UserTaskUiUriType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static UserTaskUiUriType fromValue(String value) {
        for (UserTaskUiUriType b : UserTaskUiUriType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}