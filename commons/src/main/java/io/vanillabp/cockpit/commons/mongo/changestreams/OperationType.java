package io.vanillabp.cockpit.commons.mongo.changestreams;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum OperationType {
    
    ANY("insert", "update", "replace", "delete"),
    INSERT("insert"),
    UPDATE("update", "replace"),
    DELETE("delete");
    
    private List<String> mongoTypes;
    
    OperationType(final String... types) {
        
        mongoTypes = new LinkedList<>();
        Arrays
                .stream(types)
                .forEach(type -> mongoTypes.add(type));
                
    }
    
    public List<String> getMongoTypes() {
        return mongoTypes;
    }
    
    public static OperationType byMongoType(
            final String type) {
        
        return List
                .of(INSERT, UPDATE, DELETE)
                .stream()
                .filter(operation -> operation.getMongoTypes().contains(type))
                .findFirst()
                .orElse(null);

    }
    
}