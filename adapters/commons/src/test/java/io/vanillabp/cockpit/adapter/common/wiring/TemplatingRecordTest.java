package io.vanillabp.cockpit.adapter.common.wiring;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Version;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import no.api.freemarker.java8.Java8ObjectWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the scenario where a Java record is passed as a template context
 * to Freemarker configured like {@code CockpitCommonAdapterConfiguration#businessCockpitFreemarker()}.
 */
class TemplatingRecordTest {

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

    public record WorkflowDetails(
            String id,
            OffsetDateTime createdAt,
            String erpCustomerMatchCode,
            Integer erpOfferId,
            Integer erpOfferConfirmationId,
            Integer erpOperationId,
            String processPhase) implements Serializable {
    }

    private Configuration newCockpitFreemarkerConfiguration() {
        final var config = new Configuration(FREEMARKER_VERSION);
        config.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
        config.setLocalizedLookup(true);
        config.setRecognizeStandardFileExtensions(true);
        final var objectWrapper = new Java8ObjectWrapper(FREEMARKER_VERSION);
        objectWrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
        config.setObjectWrapper(objectWrapper);
        config.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        return config;
    }

    @Test
    void rendersRecordAccessorWithErpOperationId() throws Exception {

        final var config = newCockpitFreemarkerConfiguration();
        final var template = config.getTemplate("workflow-title.ftl");

        // Note: value kept < 1000 so the assertion is independent of the JVM's
        // default locale thousands-separator (Freemarker renders ${n} locale-aware).
        final var order = new WorkflowDetails(
                "id-1",
                OffsetDateTime.now(),
                "CUST",
                null,
                null,
                711,
                "PHASE_A");

        final var rendered = FreeMarkerTemplateUtils.processTemplateIntoString(
                template,
                Map.of("order", order));

        assertEquals("Kundenauftrag (Vorgang 711)", rendered);
    }

    @Test
    void rendersRecordAccessorWithErpOfferId() throws Exception {

        final var config = newCockpitFreemarkerConfiguration();
        final var template = config.getTemplate("workflow-title.ftl");

        final var order = new WorkflowDetails(
                "id-2",
                OffsetDateTime.now(),
                "CUST",
                42,
                null,
                null,
                "PHASE_A");

        final var rendered = FreeMarkerTemplateUtils.processTemplateIntoString(
                template,
                Map.of("order", order));

        assertEquals("Kundenauftrag (Angebot 42)", rendered);
    }

    @Test
    void rendersWithoutOrder() throws Exception {

        final var config = newCockpitFreemarkerConfiguration();
        final var template = config.getTemplate("workflow-title.ftl");

        final var rendered = FreeMarkerTemplateUtils.processTemplateIntoString(
                template,
                new java.util.HashMap<String, Object>());

        assertEquals("Kundenauftrag", rendered);
    }

}
