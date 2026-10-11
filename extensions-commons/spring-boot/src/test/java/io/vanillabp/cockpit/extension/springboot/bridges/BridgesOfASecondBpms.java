package io.vanillabp.cockpit.extension.springboot.bridges;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;

/**
 * The second shape in which a BPMS half may be registered: one bean holds the bridges of every
 * adapter id it serves. A half which builds one bridge per configured adapter id cannot know at
 * build time how many there are, so Quarkus needs this shape. What one platform accepts, the
 * other has to accept too.
 */
@Configuration
public class BridgesOfASecondBpms {

  /** The adapter the bridges of this list serve. */
  public static final String ADAPTER_ID = "second";

  @Bean
  public List<BusinessCockpitBpmsBridge> bridgesOfTheSecondBpms() {

    return List.of(new SecondBpmsBridge(ADAPTER_ID));

  }

}
