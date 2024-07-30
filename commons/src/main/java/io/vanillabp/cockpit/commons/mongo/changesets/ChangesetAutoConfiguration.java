package io.vanillabp.cockpit.commons.mongo.changesets;

import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties.Mode;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;

/**
 * This is a MongoDb initializer. It works based on annotated Spring beans.
 * 
 * @see DbChangeset
 * @see DbChangesetConfiguration
 */
@Configuration
@AutoConfigureBefore(MongoReactiveRepositoriesAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
public class ChangesetAutoConfiguration {

    private static Logger logger = LoggerFactory.getLogger(ChangesetAutoConfiguration.class);

    private static String SYSTEMPROPERTY_ROLLBACKALL = "initializer.rollback.all";

    private static String SYSTEMPROPERTY_ROLLBACK_UNKNOWN = "initializer.rollback.unknown";
    
    static class DbChangesetMethod {

        Object bean;
        
        Method reflectionMethod;
        
    }

    private ApplicationContext applicationContext;

    private ReactiveMongoTemplate mongoTemplate;
    
    private MongoDbProperties properties;
    
    public ChangesetAutoConfiguration(
            final ApplicationContext applicationContext, 
            final ReactiveMongoTemplate mongoTemplate,
            final MongoDbProperties properties) {
        
        this.applicationContext = applicationContext;
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;

    }

    @PostConstruct
    public void init() {
    
        logger.info("About to apply MongoDb changesets...");
        
        mongoTemplate.setWriteConcern(WriteConcern.JOURNALED);

        initChangesetsCollection();
        
        final var changesets = buildMapSortedByChangesetOrder();
        
        collectChangesetsByAnnotationsOnBeans(changesets);
        
        rollbackAllIfNecessary();
        
        final var unknownChangesets = removeAlreadyAppliedChangesets(changesets);
        
        applyNewChangesets(changesets);
        
        rollbackUnknownChangesets(unknownChangesets);
        
        synchronizeDbChangesToDisk();

        logger.info("Applying MongoDb changesets completed.");
        
    }

    private void initChangesetsCollection() {
        
        if (mongoTemplate
                .getCollectionNames()
                .toStream()
                .anyMatch(name -> name.equals(ChangesetInformation.COLLECTION_NAME))) {
            return;
        }
        
        mongoTemplate
                .createCollection(ChangesetInformation.COLLECTION_NAME)
                .block();

        // Azure Cosmos wants indexes for all fields ordered by
        if (properties.getMode() == Mode.AZURE_COSMOS_MONGO_4_2) {
            
            mongoTemplate
                    .indexOps(ChangesetInformation.COLLECTION_NAME)
                    .ensureIndex(new Index()
                            .on("order", Direction.ASC)
                            .named(ChangesetInformation.COLLECTION_NAME + "_order"))
                    .block();
            
        }
        
    }
    
    private void rollbackAllIfNecessary() {
        
        final String rollbackSysProp = System.getProperty(
                SYSTEMPROPERTY_ROLLBACKALL,
                Boolean.FALSE.toString());
        if (! rollbackSysProp.equals(Boolean.TRUE.toString())) {
            return;
        }
        
        knownChangesets()
                .forEach(changeset -> rollbackChangeset(changeset,
                        "Rolling back changeset '{}' due to system property " + SYSTEMPROPERTY_ROLLBACKALL));
        
        logger.info("Will exit due to system property {}", SYSTEMPROPERTY_ROLLBACKALL);
        System.exit(1);
        
    }
    
    private void synchronizeDbChangesToDisk() {
        
        //client.fsync(false);
        
    }
    
    private void rollbackUnknownChangesets(
            final List<ChangesetInformation> unknownChangesets) {

        final String rollbackSysProp = System.getProperty(
                SYSTEMPROPERTY_ROLLBACK_UNKNOWN,
                Boolean.FALSE.toString())
                .toLowerCase();
        if (!rollbackSysProp.equals(Boolean.TRUE.toString())) {
            return;
        }
        
        unknownChangesets
                .forEach(changeset -> rollbackChangeset(changeset,
                        "Rolling back unknown changeset '{}' of previous software version"));
        
    }

    private void rollbackChangeset(
            final ChangesetInformation changeset,
            final String info) {
        
        try {
            logger.info(info, changeset.getId());

            changeset.getRollbackScripts()
                    .stream()
                    .map(Document::parse)
                    .forEach(script -> mongoTemplate
                            .execute(db -> db.runCommand(script))
                            .blockFirst());

            mongoTemplate
                    .remove(changeset)
                    .block();
        } catch (Exception e) {
            logger.info(info + " failed!", changeset.getId(), e);
        }
        
    }

    private void applyNewChangesets(
            final Map<ChangesetInformation, DbChangesetMethod> changeSets) {
        
        changeSets
                .entrySet()
                .forEach(changeset -> applyNewChangeset(changeset.getKey(), changeset.getValue()));
        
    }

    @SuppressWarnings("unchecked")
    private void applyNewChangeset(
            final ChangesetInformation changeset,
            final DbChangesetMethod method) {

        logger.info("Applying new changeset '{}'", changeset.getId());
        
        // save to force optimistic locking exception if another cluster-node
        // initializes at the same time and fast a little bit faster
        final ChangesetInformation persistedChangeset = mongoTemplate
                .save(changeset)
                .block();

        persistedChangeset.setTimestamp(OffsetDateTime.now());
        
        try {
            final var reflectionMethod = method.reflectionMethod;
            final Object rollbackScript;
            if (reflectionMethod.getParameterCount() == 1) {
                rollbackScript = reflectionMethod.invoke(method.bean, mongoTemplate);
            } else {
                rollbackScript = reflectionMethod.invoke(method.bean);
            }
            final List<String> rollbackScripts;
            if (rollbackScript == null) {
                rollbackScripts = List.of();
            } else if (rollbackScript instanceof String) {
                rollbackScripts = List.of((String) rollbackScript);
            } else if (rollbackScript instanceof List) {
                rollbackScripts = List.copyOf((Collection<String>) rollbackScript);
            } else {
                rollbackScripts = List.of(rollbackScript.toString());
            }
            persistedChangeset.setRollbackScripts(rollbackScripts);
        } catch (Exception e) {
            mongoTemplate
                    .remove(persistedChangeset)
                    .block();
            
            throw new RuntimeException("Could not apply changeset '"
                    + changeset.getId()
                    + "'", e);
        }
        
        mongoTemplate
                .save(persistedChangeset)
                .block();

    }

    private void collectChangesetsByAnnotationsOnBeans(
            final Map<ChangesetInformation, DbChangesetMethod> changeSetMethods) {
        
        final var changesetBeans = applicationContext
                .getBeansWithAnnotation(ChangesetConfiguration.class);
        changesetBeans
                .entrySet()
                .forEach(beanEntry -> this.collectionChangesetsByMethodsOfAnnotatedBeans(
                        changeSetMethods, beanEntry.getValue()));
        
    }
    
    private void collectionChangesetsByMethodsOfAnnotatedBeans(
            final Map<ChangesetInformation, DbChangesetMethod> changeSetMethods,
            final Object bean) {
        
        final var config = bean
                .getClass()
                .getAnnotation(ChangesetConfiguration.class);
        
        Arrays
                .stream(bean.getClass().getMethods())
                .forEach(beanMethod -> collectionChangesetsByAnnotatedBeanMethods(
                        changeSetMethods,
                        config.author(),
                        bean,
                        beanMethod));
        
    }

    private void collectionChangesetsByAnnotatedBeanMethods(
            final Map<ChangesetInformation, DbChangesetMethod> changeSetMethods,
            final String defaultAuthor,
            final Object bean,
            final Method beanMethod) {
        
        final var changesetAnnotation = beanMethod.getAnnotation(Changeset.class);
        if (changesetAnnotation == null) {
            return;
        }
        
        final String author;
        if (changesetAnnotation.author().equals("")) {
            author = defaultAuthor;
        } else {
            author = changesetAnnotation.author();
        }
        final var information = new ChangesetInformation();
        information.setAuthor(author);
        final String changesetId = bean.getClass().getName() + "#" + beanMethod.getName();
        information.setId(changesetId);
        information.setOrder(changesetAnnotation.order());
        
        final var method = new DbChangesetMethod();
        method.bean = bean;
        method.reflectionMethod = beanMethod;
        
        final Class<?> returnType = method.reflectionMethod.getReturnType();
        final boolean isString = returnType.equals(String.class);
        final boolean isCollection = Collection.class.isAssignableFrom(returnType);
        if (!isString && !isCollection) {
            throw new RuntimeException(
                    "Result type of changeset method '"
                    + changesetId
                    + "' has to be either String, String[] or Collection<String>, but is '"
                    + returnType);
        }
                
        final var duplicates = changeSetMethods.keySet()
                .stream()
                .filter((key) -> key.getOrder() == information.getOrder())
                .collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            throw new RuntimeException(
                    "Got at least two changesets having the same order value '"
                    + information.getOrder()
                    + "': '"
                    + information.getId()
                    + "' and '"
                    + duplicates.iterator().next().getId()
                    + "'!");
        }

        changeSetMethods.put(information, method);
        
    }

    private List<ChangesetInformation> knownChangesets() {

        final var query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "order"));
        
        return mongoTemplate
                .find(query, ChangesetInformation.class)
                .toStream()
                .collect(Collectors.toList());
        
    }
    
    private List<ChangesetInformation> removeAlreadyAppliedChangesets(
            final Map<ChangesetInformation, DbChangesetMethod> changeSetMethods) {

        
        final var unkownChangesets = new LinkedList<ChangesetInformation>();
        
        knownChangesets()
                .forEach(alreadyAppliedChangeset -> {
                        if (changeSetMethods.remove(alreadyAppliedChangeset) == null) {
                            unkownChangesets.add(alreadyAppliedChangeset);
                        }
                });
        
        return unkownChangesets;
        
    }
        
    private Map<ChangesetInformation, DbChangesetMethod> buildMapSortedByChangesetOrder() {
        
        return new TreeMap<>(
                new Comparator<ChangesetInformation>() {
            @Override
            public int compare(
                    final ChangesetInformation o1,
                    final ChangesetInformation o2) {
                
                if (o1.equals(o2)) {
                    return 0;
                }
                if (o1.getOrder() != o2.getOrder()) {
                    return o1.getOrder() < o2.getOrder() ? -1 : 1;
                }
                if (o1.getId() == o2.getId()) {
                    return 0;
                }
                return o1.getId().compareTo(o2.getId());
                
            }
        });
        
    }
    
}
