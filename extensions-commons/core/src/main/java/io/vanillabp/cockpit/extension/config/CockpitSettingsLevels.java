package io.vanillabp.cockpit.extension.config;

import io.vanillabp.integration.extension.spi.settings.SettingsLevel;

/**
 * The levels of the cockpit's configuration, as the platform walks the levels of a plug-in.
 * <p>
 * The cockpit binds its own tree, below <code>vanillabp.cockpit</code> and the <code>cockpit</code>
 * sections of the levels under it, because that is where version 1 configured it and an application
 * moving to version 2 swaps a dependency rather than its configuration. In which order the levels
 * are asked is not the cockpit's business.
 * {@link io.vanillabp.integration.extension.spi.settings.SettingsResolution} knows that for every
 * plug-in, and this class only says which section belongs to which level.
 * <p>
 * The adapter positions answer nothing on purpose. A plug-in may be told something per configured
 * adapter, and the cockpit will want that the day a value has to differ between two clusters of
 * one BPMS type. Until then nothing binds those keys, and a position nothing binds is not a
 * position an application can write.
 */
public final class CockpitSettingsLevels {

  private CockpitSettingsLevels() {
  }

  /**
   * The levels above and below one workflow module, from the application down.
   *
   * @param module The workflow module whose settings are asked about
   * @return The application level, which is where the walk starts
   */
  public static SettingsLevel<CockpitSection> of(
      final WorkflowModuleConfiguration module) {

    return new Application(module);

  }

  /**
   * The application. It says nothing about the keys which exist at several levels. What the
   * whole application configures, namely the transport, the templates and the two switches, has
   * one place and is read there instead of being resolved per workflow.
   */
  private record Application(WorkflowModuleConfiguration module) implements SettingsLevel<CockpitSection> {

    @Override
    public CockpitSection settings() {

      return null;

    }

    @Override
    public CockpitSection settingsOfAdapter(
        final String adapterId) {

      return null;

    }

    @Override
    public SettingsLevel<CockpitSection> levelBelow(
        final String workflowModuleId) {

      return module.workflowModuleId().equals(workflowModuleId)
          ? new Module(module)
          : null;

    }

  }

  /** One workflow module. */
  private record Module(WorkflowModuleConfiguration module) implements SettingsLevel<CockpitSection> {

    @Override
    public CockpitSection settings() {

      return module;

    }

    @Override
    public CockpitSection settingsOfAdapter(
        final String adapterId) {

      return null;

    }

    @Override
    public SettingsLevel<CockpitSection> levelBelow(
        final String bpmnProcessId) {

      final var workflow = module.workflows().get(bpmnProcessId);
      return workflow == null ? null : new Workflow(workflow);

    }

  }

  /**
   * One workflow. The walk ends here, because a user task says nothing which is resolved this
   * way. It carries a segment of the template path, and a path is assembled from the levels
   * instead of being overridden by the most specific one.
   */
  private record Workflow(WorkflowConfiguration workflow) implements SettingsLevel<CockpitSection> {

    @Override
    public CockpitSection settings() {

      return workflow;

    }

    @Override
    public CockpitSection settingsOfAdapter(
        final String adapterId) {

      return null;

    }

    @Override
    public SettingsLevel<CockpitSection> levelBelow(
        final String taskDefinition) {

      return null;

    }

  }

}
