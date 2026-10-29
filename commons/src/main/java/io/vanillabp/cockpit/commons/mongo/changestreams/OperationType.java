package io.vanillabp.cockpit.commons.mongo.changestreams;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The kinds of changes a change-stream subscription can ask for. Each kind stands for one or more
 * operation types as MongoDB names them.
 */
public enum OperationType {
    
    /**
     * Every change: insert, update, replace and delete.
     */
    ANY("insert", "update", "replace", "delete"),
    /**
     * A new document.
     */
    INSERT("insert"),
    /**
     * A changed document, either updated in parts or replaced as a whole.
     */
    UPDATE("update", "replace"),
    /**
     * A deleted document.
     */
    DELETE("delete");
    
    private List<String> mongoTypes;
    
    OperationType(final String... types) {
        
        mongoTypes = new LinkedList<>();
        Arrays
                .stream(types)
                .forEach(type -> mongoTypes.add(type));
                
    }
    
    /**
     * The operation types as MongoDB names them in a change event.
     *
     * @return The MongoDB names
     */
    public List<String> getMongoTypes() {
        return mongoTypes;
    }
    
    /**
     * Finds the kind of change for an operation type of a change event.
     *
     * @param type The operation type as MongoDB names it, like {@code replace}
     * @return The kind of change, never {@link #ANY}
     */
    public static OperationType byMongoType(
            final String type) {

        return List
                .of(INSERT, UPDATE, DELETE)
                .stream()
                .filter(operation -> operation.getMongoTypes().contains(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Unsupported change-stream operation-type '"
                        + type
                        + "'!"));

    }
    
}