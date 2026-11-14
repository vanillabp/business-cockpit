package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.TheClassOfABean;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A message at startup names the class the developer wrote, and not the proxy a platform
 * generated around it. The proxies below are named the way Quarkus and Spring name theirs.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AProxyIsNamedByTheClassBehindItTest {

  static class TeamHierarchy {
  }

  static class TeamHierarchy_ClientProxy extends TeamHierarchy {
  }

  static class TeamHierarchy_Subclass extends TeamHierarchy {
  }

  static class TeamHierarchy$$SpringCGLIB$$0 extends TeamHierarchy {
  }

  @Test
  @DisplayName("A client proxy, an intercepting subclass and a CGLIB class name the bean's class")
  public void aProxyNamesTheClassBehindIt() {

    final var expected = TeamHierarchy.class.getName();
    assertEquals(expected, TheClassOfABean.nameOf(new TeamHierarchy_ClientProxy()));
    assertEquals(expected, TheClassOfABean.nameOf(new TeamHierarchy_Subclass()));
    assertEquals(expected, TheClassOfABean.nameOf(new TeamHierarchy$$SpringCGLIB$$0()));

  }

  @Test
  @DisplayName("A bean which is no proxy is named by its own class")
  public void aBeanNamesItsOwnClass() {

    assertEquals(TeamHierarchy.class.getName(), TheClassOfABean.nameOf(new TeamHierarchy()));

  }

}
