package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.vanillabp.cockpit.config.web.WebSecurityConfiguration;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The user signed in, {@code GET /gui/api/v1/app/current-user}, belongs to the official GUI API. A
 * workflow module asks it with the generated client of that API, with the user's cookie, for
 * example to tell whether another user claimed a task. The cockpit answers it with the controller
 * which answered it before, and every security chain keeps the path open, so the user interface
 * can ask before anybody signed in.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheCurrentUserIsPartOfTheOfficialApiTest {

  private UserContext userContext;

  private PersonAndGroupApiMapper personAndGroupMapper;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {

    userContext = mock(UserContext.class);
    personAndGroupMapper = mock(PersonAndGroupApiMapper.class);
    final var controller = new LoginApiController();
    ReflectionTestUtils.setField(controller, "userContext", userContext);
    ReflectionTestUtils.setField(controller, "personAndGroupMapper", personAndGroupMapper);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();

  }

  @Test
  void theControllerImplementsTheInterfaceOfTheOfficialApi() {

    assertThat(OfficialCurrentUserApi.class).isAssignableFrom(LoginApiController.class);

  }

  @Test
  void theUserSignedInComesWithAllGroups() throws Exception {

    final var user = mock(UserDetails.class);
    when(user.getId()).thenReturn("john");
    when(user.getAuthorities()).thenReturn(List.of("CLERK"));
    when(userContext.getUserLoggedInDetails()).thenReturn(user);
    when(personAndGroupMapper.toApiPerson("john"))
        .thenReturn(new Person().id("john").display("Doe, John").email("john@example.com"));
    when(personAndGroupMapper.authorityToApiGroup("CLERK")).thenReturn(new Group().id("CLERK").display("Clerk"));

    mvc
        .perform(get("/gui/api/v1/app/current-user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("john"))
        .andExpect(jsonPath("$.display").value("Doe, John"))
        .andExpect(jsonPath("$.email").value("john@example.com"))
        .andExpect(jsonPath("$.groups[0].id").value("CLERK"));

  }

  @Test
  void withoutALoginTheAnswerHasNoBody() throws Exception {

    mvc
        .perform(get("/gui/api/v1/app/current-user"))
        .andExpect(status().isOk())
        .andExpect(content().string(""));

  }

  @Test
  void everySecurityChainKeepsThePathOpen() {

    assertThat(WebSecurityConfiguration.currentUserRequestMatcher
        .matches(new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user")))
        .isTrue();

  }

}
