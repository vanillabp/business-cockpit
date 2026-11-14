package io.vanillabp.cockpit.extension.springboot.test;

import java.util.List;

import io.vanillabp.cockpit.extension.spi.AdapterCockpitKey;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys;

/**
 * What a BPMS half of the test adapter type announces: one key, read at the adapter only. The
 * service loader finds it through the file below <code>META-INF/services</code> of the test
 * resources, the way it finds the class of a real BPMS half.
 */
public class DummyAdapterCockpitKeys implements AdapterCockpitKeys {

  /** The key the tests write at the wrong level. */
  public static final String KEY = "check-interval";

  @Override
  public String adapterType() {

    return "dummy";

  }

  @Override
  public List<AdapterCockpitKey> keys() {

    return List
        .of(AdapterCockpitKey.atTheAdapterOnly(KEY, "PT1M", "The test adapter runs one check for each adapter id."));

  }

}
