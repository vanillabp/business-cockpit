package io.vanillabp.cockpit.commons.mongo.changestreams;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import reactor.core.publisher.Flux;

@Component
@ConditionalOnBean(ReactiveMongoTemplate.class)
public class ReactiveChangeStreamUtils {

    private static final String COLLECTION_NAME_PROPERTY = "COLLECTION_NAME";

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    
    public <T> Flux<ChangeStreamEvent<T>> subscribe(
            final Class<T> entityClass,
            final OperationType... watchingOperationTypes) {
        
        // get collection name from entity class
        final String collectionName;
        try {
            collectionName = entityClass
                    .getField(COLLECTION_NAME_PROPERTY)
                    .get(null)
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not access public static propery '"
                    + entityClass.getName() + "." + COLLECTION_NAME_PROPERTY + "'", e);
        }
        
        final List<String> operationTypes;
        if ((watchingOperationTypes == null)
                || (watchingOperationTypes.length == 0)) {
            operationTypes = OperationType.ANY.getMongoTypes();
        } else {
            operationTypes = new LinkedList<String>();
            Arrays
                    .stream(watchingOperationTypes)
                    .forEach(type -> operationTypes.addAll(type.getMongoTypes()));
        }
        
        return mongoTemplate
                .changeStream(entityClass)
                .withOptions(builder -> builder
                        .fullDocumentLookup(FullDocument.DEFAULT)
                        .fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.OFF)
                        .filter(newAggregation(match(where("operationType").in(operationTypes)))))
                .watchCollection(collectionName)
                .resumeAt(Instant.now())
                .listen();
        
    }
    
}
