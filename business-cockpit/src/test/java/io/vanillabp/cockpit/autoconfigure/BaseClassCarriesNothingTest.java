package io.vanillabp.cockpit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.BusinessCockpitApplication;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.lang.annotation.Annotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An application which still extends {@code BusinessCockpitApplication} has to behave exactly like
 * one which does not, and this is what makes that so: the class carries nothing.
 *
 * <p>Spring reads the annotations of a superclass while parsing the subclass, which is how extending
 * the class used to wire the cockpit. Anything left on it would therefore be wired twice, once from
 * the subclass and once from the auto-configuration, and the second one would be the one nobody sees
 * in their own source. An empty class cannot do that.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressWarnings("removal")
class BaseClassCarriesNothingTest {

    @Test
    void theBaseClassCarriesNothingButItsDeprecation() {

        assertThat(BusinessCockpitApplication.class.getAnnotations())
                .extracting(Annotation::annotationType)
                .containsExactly(Deprecated.class);

    }

    /**
     * Synthetic members are left out because the coverage agent adds one of its own to every class it
     * instruments, and that one says nothing about what the class declares.
     */
    @Test
    void theBaseClassHasNoMembersToInherit() {

        assertThat(BusinessCockpitApplication.class.getDeclaredFields())
                .filteredOn(field -> !field.isSynthetic())
                .isEmpty();
        assertThat(BusinessCockpitApplication.class.getDeclaredMethods())
                .filteredOn(method -> !method.isSynthetic())
                .isEmpty();
        assertThat(BusinessCockpitApplication.class.getSuperclass()).isEqualTo(Object.class);

    }

    /**
     * What an application extending it gets: nothing, and therefore no second copy of anything the
     * auto-configurations register.
     */
    @Test
    void extendingItAddsNothing() {

        class StillExtendsTheBaseClass extends BusinessCockpitApplication {
        }

        assertThat(StillExtendsTheBaseClass.class.getAnnotations()).isEmpty();
        assertThat(StillExtendsTheBaseClass.class.getSuperclass().getAnnotations())
                .extracting(Annotation::annotationType)
                .containsExactly(Deprecated.class);

    }

}
