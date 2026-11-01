package io.vanillabp.cockpit.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The library brings no security chain, no users and no user details provider. Those are the
 * application's, and the module {@code container} is the example. An example login in the library
 * would be active in every application built on it, and nothing would say so. So this test reads
 * every class of the library and fails on a bean method which produces one of them.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheLibraryBringsNoLoginTest {

  private static final String CLASSES_OF_THE_LIBRARY = "classpath*:io/vanillabp/cockpit/**/*.class";

  private static final List<Class<?>> WHAT_ONLY_THE_APPLICATION_BRINGS = List.of(
      SecurityFilterChain.class, UserDetailsService.class, UserDetailsProvider.class);

  @Test
  void noBeanOfTheLibraryIsAChainOrAUserStore() throws Exception {

    final var classes = classesOfTheLibrary();
    assertThat(classes)
        .as("classes of the library found on the classpath; far fewer means the scan is broken")
        .hasSizeGreaterThan(100);

    final var offending = new ArrayList<String>();
    for (final var type : classes) {
      for (final var method : type.getDeclaredMethods()) {
        if (method.getAnnotation(Bean.class) == null) {
          continue;
        }
        WHAT_ONLY_THE_APPLICATION_BRINGS
            .stream()
            .filter(forbidden -> forbidden.isAssignableFrom(method.getReturnType()))
            .forEach(forbidden -> offending.add(type.getName()
                + "."
                + method.getName()
                + " returns a "
                + forbidden.getSimpleName()));
      }
    }

    assertThat(offending)
        .as("bean methods of the library which belong to the application, see the module "
            + "'container' for where they live instead")
        .isEmpty();

  }

  /**
   * The update stream connects again by itself. It must not keep a login alive which nobody
   * uses, so every chain hands this matcher to the renewal filter.
   */
  @Test
  void theUpdateStreamDoesNotRenewTheLogin() {

    assertThat(WebSecurityConfiguration.updatesRequestMatcher
        .matches(new MockHttpServletRequest("GET", "/gui/api/v1/updates")))
        .isTrue();
    assertThat(WebSecurityConfiguration.updatesRequestMatcher
        .matches(new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user")))
        .isFalse();

  }

  /**
   * Reads the classes from the library's own output folder only. Test classes and other jars below
   * {@code io.vanillabp.cockpit} are not the library.
   */
  private List<Class<?>> classesOfTheLibrary() throws IOException, ClassNotFoundException {

    final var resolver = new PathMatchingResourcePatternResolver();
    final var metadata = new CachingMetadataReaderFactory(resolver);
    final var library = WebSecurityConfiguration.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toString();
    final var result = new ArrayList<Class<?>>();
    for (final var resource : resolver.getResources(CLASSES_OF_THE_LIBRARY)) {
      if (!resource.getURL().toString().startsWith(library)) {
        continue;
      }
      final var className = metadata.getMetadataReader(resource).getClassMetadata().getClassName();
      result.add(Class.forName(className, false, getClass().getClassLoader()));
    }
    return result;

  }

}
