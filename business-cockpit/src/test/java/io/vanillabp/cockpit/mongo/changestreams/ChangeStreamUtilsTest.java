package io.vanillabp.cockpit.mongo.changestreams;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A change stream is opened for the collection an entity class names in its field
 * {@code COLLECTION_NAME}.
 */
@ExtendWith(SuppressOutputExtension.class)
class ChangeStreamUtilsTest {

  @Test
  void aClassWithoutACollectionNameIsRefusedWithThePropertyItLacks() {

    assertThatThrownBy(() -> new ChangeStreamUtils().subscribe(String.class, false, message -> {
    }))
        .hasMessage("Could not access public static property 'java.lang.String.COLLECTION_NAME'");

  }

}
