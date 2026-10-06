package io.vanillabp.cockpit.gui.api.v1;

import com.mongodb.client.MongoClient;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.tasklist.api.UserTaskStreamAudience;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.api.WorkflowStreamAudience;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.scheduling.TaskScheduler;

/**
 * What the tests of the update streams and the measurement of them share: a template which reads
 * and writes like the cockpit's, the streams wired to both audiences, and documents which carry
 * only the fields the visibility looks at.
 */
final class UpdateStreamFixtures {

    static final String DATABASE = "business-cockpit";

    private UpdateStreamFixtures() {
    }

    /**
     * A template with the converters of {@code MongoDbConfiguration}, so a query is mapped the way
     * the cockpit maps it.
     */
    static MongoTemplate cockpitLikeTemplate(
            final MongoClient mongoClient) {

        final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE);
        final var conversions = new MongoCustomConversions(List.of(
                new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter(),
                new BigDecimalReadConverter(),
                new BigDecimalWriteConverter()));
        final var mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return new MongoTemplate(factory, converter);

    }

    /**
     * The update streams as the cockpit wires them, with both audiences. The scheduler is the one
     * given, so a test decides whether anything scheduled ever runs.
     */
    static UpdateStreams updateStreams(
            final MongoTemplate mongoTemplate,
            final TaskScheduler taskScheduler,
            final int maxKnownIdsPerStream) {

        final var properties = new ApplicationProperties();
        // every call of consumeEvents delivers, so a test needs no clock
        properties.getGuiSse().setUpdateInterval(-1);
        properties.getGuiSse().setMaxItemsPerUpdate(Integer.MAX_VALUE);
        properties.getGuiSse().setMaxKnownIdsPerStream(maxKnownIdsPerStream);
        return new UpdateStreams(
                properties,
                taskScheduler,
                List.of(
                        new UserTaskStreamAudience(mongoTemplate),
                        new WorkflowStreamAudience(mongoTemplate)));

    }

    static UpdateEmitter streamOf(
            final UpdateStreams streams,
            final org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {

        return streams
                .openStreams()
                .stream()
                .filter(stream -> stream.getEmitter() == emitter)
                .findFirst()
                .orElseThrow();

    }

    static UpdateEmitter subscribe(
            final UpdateStreams streams,
            final UserDetails user) {

        return streamOf(streams, streams.subscribe(user, Optional.empty()));

    }

    /**
     * A user task which is assigned to somebody, or to nobody, and offered to these groups. The id
     * of a person or a group is stored as {@code _id}, the way Spring Data stores the {@code id}
     * of an embedded {@code Person} or {@code Group}.
     */
    static Document userTask(
            final String id,
            final String assignee,
            final Collection<String> candidateGroups) {

        final var document = new Document("_id", id)
                .append("candidateGroups", candidateGroups
                        .stream()
                        .map(group -> new Document("_id", group))
                        .toList())
                .append("dangling", (assignee == null) && candidateGroups.isEmpty());
        if (assignee != null) {
            document.append("assignee", new Document("_id", assignee));
        }
        return document;

    }

    /** A workflow addressed to these groups. */
    static Document workflow(
            final String id,
            final Collection<String> accessibleToGroups) {

        return new Document("_id", id)
                .append("accessibleToGroups", accessibleToGroups
                        .stream()
                        .map(group -> new Document("_id", group))
                        .toList())
                .append("dangling", accessibleToGroups.isEmpty());

    }

    static String userTasks() {
        return UserTask.COLLECTION_NAME;
    }

    static String workflows() {
        return Workflow.COLLECTION_NAME;
    }

    record Person(
            String id,
            List<String> groups) implements UserDetails {

        static Person of(
                final String id,
                final String... groups) {

            return new Person(id, List.of(groups));

        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<String> getAuthorities() {
            return groups;
        }

        @Override
        public String getEmail() {
            return id + "@example.com";
        }

        @Override
        public String getDisplay() {
            return id;
        }

        @Override
        public String getDisplayShort() {
            return id;
        }

    }

}
