package io.vanillabp.cockpit.extension.springboot.bridges;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;

/**
 * The second shape a BPMS half may be registered in: one bean holding the bridges of every
 * adapter id it serves. A half which builds one bridge per configured adapter id cannot say at
 * build time how many that is, which is why Quarkus needs this shape - and what is accepted on
 * one platform has to be accepted on the other.
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
