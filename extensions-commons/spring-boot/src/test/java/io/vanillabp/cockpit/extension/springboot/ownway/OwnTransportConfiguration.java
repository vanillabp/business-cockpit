package io.vanillabp.cockpit.extension.springboot.ownway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An application which reports its own way. There is one bean, and the extension's own transport
 * is out of the picture. Nothing else is needed, and no property key says so.
 */
@Configuration
public class OwnTransportConfiguration {

  @Bean
  public OwnTransport ownTransport() {

    return new OwnTransport();

  }

}
