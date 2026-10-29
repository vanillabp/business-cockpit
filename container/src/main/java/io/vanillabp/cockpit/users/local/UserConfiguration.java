package io.vanillabp.cockpit.users.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;

/**
 * Gives the profile {@code local} a user directory without any setup: demo users loaded from the
 * simulator of the development tools. A bean of this class is left out where the application
 * defines one of its own, so a custom cockpit can bring its real user directory and keep the rest.
 * <p>
 * The property {@code dev-shell-simulator.users-uri} sets the address of the simulator's users.
 */
@Configuration
public class UserConfiguration {

  /**
   * Called by Spring, which creates this configuration class.
   */
  public UserConfiguration() {
  }

  @Value("${dev-shell-simulator.users-uri:http://localhost:8079/dev-shell/user}")
  private String devShellSimulatorUsersUri;

  /**
   * The directory of the demo users.
   *
   * @return The directory, loaded from the simulator
   */
  @Bean
  @ConditionalOnMissingBean(UserDetailsProvider.class)
  @Profile("local")
  public UserDetailsProvider bcUserDetailsProvider() {

    return new LocalUserDetailsProviderImpl(devShellSimulatorUsersUri);

  }

  /**
   * Shows the demo users in the GUI API.
   *
   * @param bcUserDetailsProvider The directory of the demo users
   * @return The mapper
   */
  @Bean
  @ConditionalOnMissingBean(PersonAndGroupApiMapper.class)
  @Profile("local")
  public PersonAndGroupApiMapper bcPersonAndGroupApiMapper(
      final UserDetailsProvider bcUserDetailsProvider) {

    return new LocalPersonAndGroupApiMapperImpl(bcUserDetailsProvider);

  }

  /**
   * Turns the ids a workflow module reports into stored persons and groups of the demo users.
   *
   * @param bcUserDetailsProvider The directory of the demo users
   * @return The mapper
   */
  @Bean
  @ConditionalOnMissingBean(PersonAndGroupMapper.class)
  @Profile("local")
  public PersonAndGroupMapper bcPersonAndGroupMapper(
      final UserDetailsProvider bcUserDetailsProvider) {

    return new LocalPersonAndGroupMapperImpl(bcUserDetailsProvider);

  }

}
