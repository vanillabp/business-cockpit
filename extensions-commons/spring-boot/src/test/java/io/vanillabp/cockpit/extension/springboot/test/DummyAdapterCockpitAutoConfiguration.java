package io.vanillabp.cockpit.extension.springboot.test;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Registers {@link DummyAdapterCockpitOverlayProperties} in every test application, the way the
 * auto-configuration of a real BPMS half registers its overlay class. The file below
 * <code>META-INF/spring</code> of the test resources names it.
 */
@AutoConfiguration
@EnableConfigurationProperties(DummyAdapterCockpitOverlayProperties.class)
public class DummyAdapterCockpitAutoConfiguration {
}
