package io.vanillabp.cockpit.extension.quarkus.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.cockpit.extension.quarkus.BusinessCockpitProducer;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;

/**
 * What the Business Cockpit extension has to say at build time. It produces no VanillaBP build
 * item. Unlike an adapter, an extension announces itself by the beans it produces.
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
   * Ends the build if a key of one of the cockpit's sections is a key nobody reads. The message
   * names the key it was nearest to.
   * <p>
   * Quarkus refuses such a key by itself while the application starts, because no configuration
   * mapping declares it. That is all Quarkus can say. This check runs while the application is
   * built, over what the configuration files say, and it answers the question a developer really
   * has, which is which key was meant. It only sees the configuration of the build. An
   * environment variable of the container and a workflow module's own defaults file are read
   * when the application starts. They are left to Quarkus and its own message, which names the
   * key without guessing what it should have been.
   *
   * @param validation Where a defect of the application is collected, so that the build ends
   *          with it rather than with a stack trace of this step
   */
  @BuildStep
  void refuseAKeyNobodyReads(
      final BuildProducer<ValidationErrorBuildItem> validation) {

    try {
      ConfigurationKeyCheck
          .refuseKeysNobodyReads(ConfigProvider.getConfig().getPropertyNames());
    } catch (final IllegalStateException e) {
      validation.produce(new ValidationErrorBuildItem(e));
    }

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
