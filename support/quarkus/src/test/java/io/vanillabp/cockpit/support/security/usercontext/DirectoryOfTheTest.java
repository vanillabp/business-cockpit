package io.vanillabp.cockpit.support.security.usercontext;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The user directory of a test application. A test injects it and changes it while it runs.
 */
@ApplicationScoped
public class DirectoryOfTheTest extends ChangeableUserDirectory {

}
