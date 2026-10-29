package io.vanillabp.cockpit.commons.mongo;

import com.mongodb.client.MongoClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;

// registered through the .imports file, so it has to be an @AutoConfiguration. Only then are
// @AutoConfigureBefore and @AutoConfigureAfter read
/**
 * Sets up the MongoDB parts the cockpit needs: its MongoDB settings and the container which runs
 * the change streams. It runs before Spring Data builds the MongoDB repositories.
 */
@AutoConfiguration
@AutoConfigureBefore(DataMongoRepositoriesAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
public class BusinessCockpitMongoDbAutoConfiguration {

    /**
     * Creates the configuration. Spring Boot calls this constructor.
     */
    public BusinessCockpitMongoDbAutoConfiguration() {
    }

    /**
     * The MongoDB settings of the cockpit, bound from the keys below {@code mongodb}.
     *
     * @return The settings
     */
    @Bean
    public MongoDbProperties businessCockpitMongoDbProperties() {

        return new MongoDbProperties();

    }

    /**
     * Runs the change streams the cockpit subscribes to. The container owns the threads blocking on
     * the cursors, which is why a change-stream subscription is a registration here and not a
     * thread of its own.
     * <p>
     * Those threads are daemons. A change stream never ends by itself, and a thread parked on the
     * cursor of a database which is already gone would otherwise keep the JVM alive after the
     * shutdown.
     *
     * @param mongoTemplate The template the change streams are read with
     * @return The container
     */
    @Bean
    @ConditionalOnBean(MongoTemplate.class)
    @ConditionalOnMissingBean
    public MessageListenerContainer messageListenerContainer(
            final MongoTemplate mongoTemplate) {

        final var executor = new SimpleAsyncTaskExecutor("mongo-change-stream-");
        executor.setDaemon(true);

        return new DefaultMessageListenerContainer(mongoTemplate, executor);

    }

}
