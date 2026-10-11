package io.vanillabp.cockpit.extension.springboot.brokenparts;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;

/**
 * The interface which makes Spring wrap the workflow service below in a JDK proxy. A proxy
 * carries the annotations of the interface but none of the implementation. So behind a proxy, a
 * scan of bean TYPES finds nothing.
 */
public interface ProxiedProvider {

  UserTaskDetails approveUpToTheThird(
      PrefilledUserTaskDetails prefilled);

  UserTaskDetails approveFromTheThird(
      PrefilledUserTaskDetails prefilled);

}
