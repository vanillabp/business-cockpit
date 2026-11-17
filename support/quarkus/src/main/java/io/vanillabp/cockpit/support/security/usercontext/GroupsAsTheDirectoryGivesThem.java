package io.vanillabp.cockpit.support.security.usercontext;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Brings the {@link GroupResolver} an application gets if it has none of its own: it takes the
 * groups as the directory gives them.
 */
@ApplicationScoped
public class GroupsAsTheDirectoryGivesThem {

  /**
   * Creates the producer. Quarkus calls this constructor.
   */
  public GroupsAsTheDirectoryGivesThem() {
  }

  /**
   * The resolver which changes nothing. A bean of type {@link GroupResolver} of the application
   * replaces it.
   *
   * @return The resolver
   */
  @Produces
  @DefaultBean
  @ApplicationScoped
  public GroupResolver groupsAsTheyAre() {

    return groups -> groups;

  }

}
