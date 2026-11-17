package io.vanillabp.cockpit.support.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.support.security.BcUnauthorizedExceptionMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application finds the mappers of this module without a line of its own only because the jar
 * carries a Jandex index. Quarkus reads the classes of a dependency only if it has one, and Quarkus
 * REST then picks every method annotated with {@code @ServerExceptionMapper} out of it. Without the
 * index the mappers would be in the jar and nobody would call them, and no other test would notice,
 * because the test framework does not build its application out of this jar.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheMappersAreInTheJandexIndexTest {

  @Test
  public void everyMapperMethodIsInTheIndexOfTheJar() throws Exception {

    final var classes = Path
        .of(BcUserTaskExceptionMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    final var indexFile = classes.resolve("META-INF/jandex.idx");

    assertThat(indexFile)
        .as("the index the jandex-maven-plugin writes into the jar")
        .exists();

    try (InputStream in = Files.newInputStream(indexFile)) {
      final var index = new IndexReader(in).read();
      final var mapperMethods = index
          .getAnnotations(DotName.createSimple(ServerExceptionMapper.class))
          .stream()
          .map(annotation -> annotation.target().asMethod())
          .map(method -> method.declaringClass().name().toString()
              + "#"
              + method.name())
          .toList();

      assertThat(mapperMethods)
          .containsExactlyInAnyOrder(
              BcUserTaskExceptionMapper.class.getName()
                  + "#unknownTask",
              BcUserTaskExceptionMapper.class.getName()
                  + "#taskNotOpen",
              BcUserTaskExceptionMapper.class.getName()
                  + "#taskClaimedByAnotherUser",
              BcUnauthorizedExceptionMapper.class.getName()
                  + "#unauthorized");
    }

  }

}
