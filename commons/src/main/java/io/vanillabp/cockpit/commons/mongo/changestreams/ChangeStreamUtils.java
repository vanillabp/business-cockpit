package io.vanillabp.cockpit.commons.mongo.changestreams;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

@Component
@ConditionalOnBean(MongoTemplate.class)
public class ChangeStreamUtils {

    private static final String COLLECTION_NAME_PROPERTY = "COLLECTION_NAME";
    
    @Autowired
    private Logger logger;

    @Autowired
    private MessageListenerContainer messageListenerContainer;
    
    public <T> Subscription subscribe(
            final Class<T> entityClass,
            final MessageListener<ChangeStreamDocument<Document>, T> listener,
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
        
        // build MongoDb request for change stream
        final ChangeStreamRequest<T> requestForChangeEvents = ChangeStreamRequest
                .builder(catchExceptionsListener(listener, entityClass))
                .fullDocumentLookup(FullDocument.DEFAULT)
                .fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.OFF)
                .filter(newAggregation(match(where("operationType").in(operationTypes))))
                .maxAwaitTime(Duration.ofSeconds(15))
                .collection(collectionName)
                .build();
        
        // hook up to message listener backed by its own thread pool
        return messageListenerContainer
                .register(requestForChangeEvents, entityClass);     
        
    }
    
    private <T> MessageListener<ChangeStreamDocument<Document>, T> catchExceptionsListener(
            final MessageListener<ChangeStreamDocument<Document>, T> listener,
            final Class<T> entityClass) {

        return (message) -> {
            try {
                listener.onMessage(message);
            } catch (Throwable e) {
                logger.warn("Could error of change-stream listener for entity '{}'!",
                        entityClass.getName(),
                        e);
            }
        };
        
    }
    
    public void unsubscribe(
            final Subscription subscription) {
        
        messageListenerContainer
                .remove(subscription);
        
    }
    
}
