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

// registered through the .imports file, so it has to be an @AutoConfiguration - only then are
// @AutoConfigureBefore/After honoured
@AutoConfiguration
@AutoConfigureBefore(DataMongoRepositoriesAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
public class BusinessCockpitMongoDbAutoConfiguration {

    @Bean
    public MongoDbProperties businessCockpitMongoDbProperties() {

        return new MongoDbProperties();

    }

    /**
     * Runs the change streams the cockpit subscribes to. The container owns the threads blocking on
     * the cursors, which is why a change-stream subscription is a registration here and not a
     * thread of its own.
     * <p>
     * Those threads are daemons: a change stream never ends by itself, and a thread parked on the
     * cursor of a database that is already gone would otherwise keep the JVM alive after shutdown.
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
