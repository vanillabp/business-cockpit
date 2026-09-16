package io.vanillabp.cockpit.config;

import com.phactum.mongodb.changesets.ChangesetApplier;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroserviceProxyConfiguration {

    /**
     * @param changesetsHaveBeenApplied not read: the registry is filled in bulk from MongoDB while it
     *      is being built, so the changesets which create the collection it reads have to have run.
     *      Asking for the bean which runs them is what says so at compile time. It has to be
     *      {@link ChangesetApplier} and not the auto-configuration which declares it. A
     *      configuration class exists before its own beans do, so asking for it would compile,
     *      start and guarantee nothing.
     */
    @Bean
    public MicroserviceProxyRegistry microserviceProxyRegistry(
            final ChangesetApplier changesetsHaveBeenApplied) {

        return new MicroserviceProxyRegistry();

    }

}
