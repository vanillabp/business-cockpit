package io.vanillabp.cockpit.config;

import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetAutoConfiguration;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroserviceProxyConfiguration {

    /**
     * @param changesetsHaveBeenApplied not read: the registry is filled in bulk from MongoDB while it
     *      is being built, so the changesets which create the collection it reads have to have run.
     *      Asking for the bean which runs them is what says so at compile time.
     */
    @Bean
    public MicroserviceProxyRegistry microserviceProxyRegistry(
            final ChangesetAutoConfiguration changesetsHaveBeenApplied) {

        return new MicroserviceProxyRegistry();

    }

}
