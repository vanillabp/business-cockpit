package io.vanillabp.cockpit.extension.springboot.brokenparts;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;

/**
 * The interface which makes Spring wrap the workflow service below in a JDK proxy. A proxy
 * carries the annotations of the interface and none of the implementation's, so a service
 * behind one is where a scan of bean TYPES stops finding anything.
 */
public interface ProxiedProvider {

  UserTaskDetails approve(
      PrefilledUserTaskDetails prefilled);

}
