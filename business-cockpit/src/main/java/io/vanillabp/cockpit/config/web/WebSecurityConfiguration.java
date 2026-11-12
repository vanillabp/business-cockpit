package io.vanillabp.cockpit.config.web;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.util.matcher.RequestMatcher;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;

/**
 * The parts of the login which belong to the library: the paths every security chain leaves open,
 * and the JWT (JSON Web Token) cookie a login is carried in.
 * <p>
 * The library brings no security chain and no users. How a person or a BPMS adapter logs in is
 * the application's choice, so the application writes the chains. The module {@code container}
 * is the reference for them, and the wiki page "Security" says what every chain has to keep.
 * The startup check says on every start which chain is missing.
 */
@Configuration
public class WebSecurityConfiguration {

  /**
   * The information about the application, like its title and version. The browser needs it
   * before anybody logs in, so it is open to everybody.
   */
  public static final RequestMatcher appInfoRequestMatcher = pathPattern(
      "/gui/api/v1/app/info");

  /**
   * The user who is logged in. It is open to everybody, so the browser can find out whether
   * somebody is logged in at all. It matches exactly this path and nothing below it, because the
   * settings of the user, at {@code /gui/api/v1/app/current-user/settings/...}, need a login.
   */
  public static final RequestMatcher currentUserRequestMatcher = pathPattern(
      "/gui/api/v1/app/current-user");

  /**
   * The update stream of a browser tab. It connects again by itself, so it is no sign of somebody
   * using the cockpit and does not renew the login.
   */
  public static final RequestMatcher updatesRequestMatcher = pathPattern(
      "/gui/api/v1/updates");

  /**
   * The files the user interface is built of. They are open to everybody.
   */
  public static final RequestMatcher assetsRequestMatcher = pathPattern(
      "/assets/**");

  /**
   * Further static files of the user interface. They are open to everybody.
   */
  public static final RequestMatcher staticRequestMatcher = pathPattern(
      "/static/**");

  /**
   * The requests the cockpit forwards to the user interfaces of the workflow modules. They are
   * open to everybody, so the cockpit itself does not ask for a login here.
   */
  public static final RequestMatcher workflowModulesProxyRequestMatcher = pathPattern(
      "/wm/**");

  @Autowired
  private ApplicationProperties properties;

  /**
   * Creates the configuration. Spring creates it because of {@code @Configuration}.
   */
  public WebSecurityConfiguration() {
  }

  /**
   * Turns a login token into the authentication of a user and back. The groups of the user are
   * completed by the group hierarchy. An application can bring its own mapper instead.
   *
   * @param groupHierarchyService Resolves the groups a group belongs to
   * @return The mapper
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtMapper<? extends JwtAuthenticationToken> jwtMapper(
      final GroupHierarchyService groupHierarchyService) {

    return new JwtAuthenticationTokenMapper(
        properties.getJwt(), groupHierarchyService::resolveGroups);

  }

  /**
   * Stores the authentication of a login in the JWT cookie, so later requests find it there.
   *
   * @param jwtMapper Turns the authentication into a token
   * @return The repository
   */
  @Bean
  public JwtSecurityContextRepository jwtSecurityContextRepository(
      final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

    return new JwtSecurityContextRepository(
        properties.getJwt(), jwtMapper);

  }

}
