package io.vanillabp.cockpit.commons.mongo.changestreams;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties.Mode;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnBean(ReactiveMongoTemplate.class)
@DependsOn("changesetAutoConfiguration")
public class ReactiveChangeStreamUtils {

    private static final String COLLECTION_NAME_PROPERTY = "COLLECTION_NAME";

    @Autowired
    private MongoDbProperties properties;
    
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    public <T> Flux<ChangeStreamEvent<T>> subscribe(
            final Class<T> entityClass,
            final OperationType... watchingOperationTypes) {
        
        return subscribe(entityClass, false, watchingOperationTypes);
        
    }

    public <T> Flux<ChangeStreamEvent<T>> subscribe(
            final Class<T> entityClass,
            final boolean fullDocument,
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
        
        final var aggregations = new LinkedList<AggregationOperation>();
        if (properties.getMode() == Mode.AZURE_COSMOS_MONGO_4_2) {
            if (operationTypes.containsAll(OperationType.DELETE.getMongoTypes())) {
                operationTypes.removeAll(OperationType.DELETE.getMongoTypes());
            }
            aggregations.add(match(where("operationType").in(operationTypes)));
            aggregations.add(project("_id", "ns", "documentKey", "fullDocument"));
        } else {
            aggregations.add(match(where("operationType").in(operationTypes)));
        }
        
        return mongoTemplate
                .changeStream(entityClass)
                .withOptions(builder -> builder
                        .fullDocumentLookup(
                                fullDocument || (properties.getMode() == Mode.AZURE_COSMOS_MONGO_4_2)
                                        ? FullDocument.UPDATE_LOOKUP // required for Cosmos
                                        : FullDocument.DEFAULT)
                        .fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.OFF)
                        .filter(newAggregation(aggregations.toArray(AggregationOperation[]::new))))
                .watchCollection(collectionName)
                .resumeAt(Instant.now())
                .listen();
        
    }
    
}
