package io.vanillabp.cockpit.extension.springboot.ownway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An application which reports its own way. One bean is enough, and the extension's own transport
 * is then not used. Nothing else is needed, and no property key has to say so.
 */
@Configuration
public class OwnTransportConfiguration {

  @Bean
  public OwnTransport ownTransport() {

    return new OwnTransport();

  }

}
