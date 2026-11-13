package io.vanillabp.cockpit.users.directory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;

/**
 * Lets operations empty the cache of the directory at once, for example after a group was taken
 * away which has to count before the next night. It is an actuator endpoint, so it lives on the
 * management side of the application: it is reachable only if operations exposes it with
 * {@code management.endpoints.web.exposure.include}, and it is secured like every other actuator
 * endpoint of the application.
 * <p>
 * Over HTTP it is {@code DELETE /actuator/userdirectorycache}. The answer is {@code 204 No Content}.
 */
@Endpoint(id = UserDirectoryCacheEndpoint.ID)
public class UserDirectoryCacheEndpoint {

  /**
   * The id of the endpoint, and so the last part of its path.
   */
  public static final String ID = "userdirectorycache";

  private static final Logger log = LoggerFactory.getLogger(UserDirectoryCacheEndpoint.class);

  private final CachingUserDirectoryService directory;

  UserDirectoryCacheEndpoint(
      final CachingUserDirectoryService directory) {

    this.directory = directory;

  }

  /**
   * Empties the cache. Each user is read from the directory again at its next request.
   */
  @DeleteOperation
  public void clear() {

    final var forgotten = directory.clearCache();
    log.info("Emptied the cache of the user directory on request of the actuator endpoint '{}', {} users "
        + "forgotten. Each of them is read from the directory again at the next request.", ID, forgotten);

  }

}
