package io.vanillabp.cockpit.config;

import com.phactum.mongodb.changesets.ChangesetApplier;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroserviceProxyConfiguration {

    /**
     * @param changesetsHaveBeenApplied not read. The registry is filled from MongoDB while it is
     *      built, so the changesets which create the collection it reads must have run first.
     *      Asking for the bean which runs them makes this order visible at compile time. It has to
     *      be {@link ChangesetApplier}, not the auto-configuration which declares it. A
     *      configuration class exists before its own beans do. Asking for it would compile and
     *      start, but guarantee nothing.
     */
    @Bean
    public MicroserviceProxyRegistry microserviceProxyRegistry(
            final ChangesetApplier changesetsHaveBeenApplied) {

        return new MicroserviceProxyRegistry();

    }

}
