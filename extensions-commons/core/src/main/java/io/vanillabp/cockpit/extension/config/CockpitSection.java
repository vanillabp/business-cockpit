package io.vanillabp.cockpit.extension.config;

import java.util.List;

/**
 * What one level of the cockpit's configuration says about a key which exists at several levels.
 * <p>
 * The platform walks the levels of a plug-in's configuration and asks each of them for a value
 * (see {@link io.vanillabp.integration.extension.spi.settings.SettingsResolution}). It does not
 * know what a section is, so this is what a section is here: the application, a workflow module, a
 * workflow and a user task each answer with what they say, and <code>null</code> where they say
 * nothing.
 * <p>
 * Only keys which MEAN the same thing at every level belong here. The template path does not: the
 * path is assembled from a segment per level rather than overridden by the most specific one, so a
 * workflow's segment is not a workflow module's segment said again.
 */
public interface CockpitSection {

  /**
   * @return The languages titles are reported in, or <code>null</code> where this level leaves it
   *         to the level above
   */
  default List<String> i18nLanguages() {

    return null;

  }

  /**
   * @return The language the BPMN names are written in, or <code>null</code> where this level
   *         leaves it to the level above
   */
  default String bpmnDescriptionLanguage() {

    return null;

  }

}
