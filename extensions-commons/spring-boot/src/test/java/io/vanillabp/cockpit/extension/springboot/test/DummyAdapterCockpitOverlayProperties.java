package io.vanillabp.cockpit.extension.springboot.test;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;

/**
 * Binds the key of {@link DummyAdapterCockpitKeys}, the way a real BPMS half binds its keys in an
 * overlay class:
 *
 * <pre>
 * vanillabp.adapters.&lt;adapter-id&gt;.cockpit.check-interval
 * </pre>
 *
 * On Spring Boot the platform ends the start for a key below <code>vanillabp</code> which no
 * {@code @ConfigurationProperties} class binds. Declaring the key in {@link DummyAdapterCockpitKeys}
 * is not enough for that, so without this class the test application could not start with the
 * key at its level.
 */
@ConfigurationProperties(MigrationAdapterProperties.PREFIX)
public class DummyAdapterCockpitOverlayProperties {

  /** The sections of the adapter ids, by adapter id. */
  private Map<String, Adapter> adapters = Map.of();

  public Map<String, Adapter> getAdapters() {

    return adapters;

  }

  public void setAdapters(
      final Map<String, Adapter> adapters) {

    this.adapters = adapters;

  }

  /** The section of one adapter id, as far as the test half reads it. */
  public static class Adapter {

    private Cockpit cockpit;

    public Cockpit getCockpit() {

      return cockpit;

    }

    public void setCockpit(
        final Cockpit cockpit) {

      this.cockpit = cockpit;

    }

  }

  /** <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit</code>. */
  public static class Cockpit {

    /** The key {@link DummyAdapterCockpitKeys#KEY}, as written. */
    private String checkInterval;

    public String getCheckInterval() {

      return checkInterval;

    }

    public void setCheckInterval(
        final String checkInterval) {

      this.checkInterval = checkInterval;

    }

  }

}
