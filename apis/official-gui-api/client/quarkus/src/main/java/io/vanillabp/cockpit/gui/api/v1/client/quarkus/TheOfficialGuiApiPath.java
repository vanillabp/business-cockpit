package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import java.util.List;

import io.quarkus.arc.Unremovable;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialGuiApiPath;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.support.rest.restclient.RestClientApiPath;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Tells {@code RestClients} of the support module the path of the official GUI API, so a workflow
 * module sets only the address of the cockpit in {@code base-url}. The path is the server path of
 * the OpenAPI file, which the generator writes into {@link OfficialGuiApiPath}, so a client of a new
 * version of the API brings its new path along.
 */
@Singleton
public class TheOfficialGuiApiPath {

  /** Creates the producer. Quarkus creates it. */
  public TheOfficialGuiApiPath() {
  }

  /**
   * The path and the interfaces of the API. Only {@code RestClients} looks it up, so Quarkus must
   * not remove it as unused.
   *
   * @return the path of the official GUI API
   */
  @Produces
  @Singleton
  @Unremovable
  RestClientApiPath officialGuiApiPath() {
    return new RestClientApiPath(OfficialGuiApiPath.PATH, List.of(
        OfficialTasklistApi.class,
        OfficialWorkflowlistApi.class,
        OfficialWorkflowModulesApi.class,
        OfficialNotificationConfigApi.class,
        OfficialCurrentUserApi.class));
  }

}
