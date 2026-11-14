package io.vanillabp.cockpit.extension.spi;

import java.util.List;

/**
 * The keys one BPMS half of the Business Cockpit reads below the adapter ids of its BPMS, at
 * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.&lt;key&gt;</code> and at the same
 * position of the levels below.
 * <p>
 * The neutral half checks every key an application writes at such a position. It refuses a key
 * which no BPMS half of the adapter's type announced, and a key written at a level where its half
 * does not read it. So a typo or a key at the wrong level ends the startup with a message, on
 * both platforms. Without the announcement, the neutral half cannot tell a key of a BPMS half from
 * a typo.
 * <p>
 * A BPMS half announces its keys through the Java service loader: a file
 * <code>META-INF/services/io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys</code> in its
 * jar names the implementing class. The service loader works the same on both platforms, and
 * Quarkus can read it while it builds the application, before any bean exists. So the class
 * needs a public constructor without parameters, and it must not depend on a bean.
 * <p>
 * A half which reads no key of its own needs no implementation. Then every key at the place of
 * its adapters is refused.
 */
public interface AdapterCockpitKeys {

  /**
   * The type of the BPMS adapter whose adapter ids carry these keys.
   *
   * @return The adapter type, for example <code>camunda8</code>
   */
  String adapterType();

  /**
   * The keys the half reads below an adapter id of {@link #adapterType()}.
   *
   * @return The keys, never <code>null</code>
   */
  List<AdapterCockpitKey> keys();

}
