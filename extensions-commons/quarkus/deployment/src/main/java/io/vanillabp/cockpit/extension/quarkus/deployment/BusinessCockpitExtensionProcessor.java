package io.vanillabp.cockpit.extension.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.vanillabp.cockpit.extension.quarkus.BusinessCockpitProducer;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;

/**
 * What the Business Cockpit extension has to say at build time. It produces no VanillaBP build
 * item: an extension announces itself by the beans it produces, unlike an adapter.
 */
class BusinessCockpitExtensionProcessor {

  private static final String FEATURE = "vanillabp-business-cockpit-extension";

  /**
   * @param featureProducer Where the feature is announced, so that a booting application lists
   *          the extension
   * @return The producer class, as a bean nothing may remove
   */
  @BuildStep
  AdditionalBeanBuildItem registerProducer(
      final BuildProducer<FeatureBuildItem> featureProducer) {

    featureProducer.produce(new FeatureBuildItem(FEATURE));
    return AdditionalBeanBuildItem
        .builder()
        .addBeanClass(BusinessCockpitProducer.class)
        .setUnremovable()
        .build();

  }

  /**
   * VanillaBP finds the factories building a per-aggregate service in the Jandex index, and a
   * runtime jar without an index of its own is not in there.
   *
   * @return The factory, named so that the index carries it
   */
  @BuildStep
  AdditionalIndexedClassesBuildItem indexTheFactory() {

    return new AdditionalIndexedClassesBuildItem(BusinessCockpitServiceFactory.class.getName());

  }

}
