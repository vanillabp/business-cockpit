package io.vanillabp.cockpit.commons.mongo;

import com.mongodb.client.MongoClient;
import com.phactum.mongodb.changesets.ChangesetAutoConfiguration;
import com.phactum.mongodb.changesets.ChangesetProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Hands the MongoDB mode of the cockpit to the changeset library.
 * <p>
 * The library reads its own key {@code mongodb.changesets.mode}, and the cockpit has carried the
 * same information in {@code mongodb.mode} since long before the library existed. Where the
 * application publishes a {@link ChangesetProperties} bean, the library takes that one and builds
 * none of its own. So the cockpit's key stays the one which counts, no installation writes the
 * value twice, and {@code mongodb.changesets.mode} never turns up in a cockpit configuration.
 * <p>
 * It is not only convenience. {@link MongoDbProperties} binds the prefix {@code mongodb} with
 * {@code ignoreUnknownFields = false}, and such a binding refuses every key below its prefix which
 * none of its own fields takes. {@code mongodb.changesets.mode} is such a key, so an application
 * which wrote it would not start at all.
 * <p>
 * This is a class of its own rather than another bean of
 * {@link BusinessCockpitMongoDbAutoConfiguration}, because it has to be ordered before the
 * library's auto-configuration and that one may not be. Ordering the other one earlier moves it
 * ahead of the auto-configuration which registers the {@code MongoTemplate}, and its
 * {@code @ConditionalOnBean(MongoTemplate.class)} then finds nothing and drops the change-stream
 * container. Only the bean below needs the order, and nothing below has a condition on another
 * bean.
 */
// registered through the .imports file, so it has to be an @AutoConfiguration. Only then are
// @AutoConfigureBefore and @AutoConfigureAfter read
@AutoConfiguration
@AutoConfigureBefore(ChangesetAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
public class ChangesetPropertiesAutoConfiguration {

    /**
     * The two enums name the same two kinds of database, so the value is carried over by name. A
     * name which the library does not know ends the start, which is where a new mode of the cockpit
     * would be noticed.
     */
    @Bean
    public ChangesetProperties mongoDbChangesetProperties(
            final MongoDbProperties properties) {

        final var changesetProperties = new ChangesetProperties();
        changesetProperties.setMode(
                ChangesetProperties.MongoDbMode.valueOf(properties.getMode().name()));
        return changesetProperties;

    }

}
