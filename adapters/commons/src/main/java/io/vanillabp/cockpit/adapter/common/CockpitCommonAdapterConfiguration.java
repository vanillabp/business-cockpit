package io.vanillabp.cockpit.adapter.common;

import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.AdapterConfigurationBase;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.rest.UserTaskRestMapperImpl;
import io.vanillabp.cockpit.adapter.common.usertask.rest.UserTaskRestPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.rest.WorkflowRestMapperImpl;
import io.vanillabp.cockpit.adapter.common.workflow.rest.WorkflowRestPublishing;
import io.vanillabp.cockpit.bpms.api.v1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.commons.rest.adapter.ClientsConfigurationBase;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.springboot.adapter.SpringDataUtil;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import io.vanillabp.springboot.modules.WorkflowModuleProperties;
import io.vanillabp.springboot.modules.WorkflowModulePropertiesConfiguration;
import no.api.freemarker.java8.Java8ObjectWrapper;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoConfigurationPackage(basePackageClasses = CockpitCommonAdapterConfiguration.class)
@AutoConfigureAfter(value = {WorkflowModulePropertiesConfiguration.class, CockpitCommonAdapterKafkaConfiguration.class})
@EnableConfigurationProperties({ VanillaBpCockpitProperties.class })
public class CockpitCommonAdapterConfiguration extends ClientsConfigurationBase {

    public static final String TEMPLATING_QUALIFIER = "BusinessCockpit";
    
    private static final Version FREEMARKER_VERSION = freemarker.template.Configuration.VERSION_2_3_31;
    
    @Value("${workerId}")
    private String workerId;
    
    @Autowired
    private List<WorkflowModuleProperties> workflowModulesProperties;
    
    @Autowired(required = false)
    private VanillaBpProperties vanillaBpProperties;
    
    @Autowired
    private VanillaBpCockpitProperties properties;

    private Map<Class<?>, AdapterAwareBusinessCockpitService<?>> connectableServices = new HashMap<>();
    
    @Autowired
    private List<AdapterConfigurationBase<?>> adapterConfigurations;

    @Bean
    @ConditionalOnMissingBean
    public UserTaskPublishing userTaskRestPublishing(
            @Qualifier("bpmsApiV1")
            final Optional<BpmsApi> bpmsApi) {

        return new UserTaskRestPublishing(
                workerId,
                bpmsApi,
                properties,
                new UserTaskRestMapperImpl());

    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowPublishing workflowRestPublishing(
            @Qualifier("bpmsApiV1")
            final Optional<BpmsApi> bpmsApi) {
        return new WorkflowRestPublishing(
                workerId,
                bpmsApi,
                properties,
                new WorkflowRestMapperImpl()
        );
    }

    private UiUriType findAndValidateUiUriTypeProperty(
            final String prefix,
            final String uiUriTypeProperty) {
        
        final var validUiUriTypes = Arrays
                .stream(UiUriType.values())
                .map(type -> type.name())
                .collect(Collectors.joining("', ", "'", "'"));
        if (!StringUtils.hasText(uiUriTypeProperty)) {
            return null;
        }
        
        try {
            return UiUriType.fromValue(uiUriTypeProperty);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Spring property '"
                    + prefix
                    + ".ui-uri-type' has an illegal value '"
                    + uiUriTypeProperty
                    + "'! Valid types are: "
                    + validUiUriTypes);
        }
        
    }
    
    @Bean
    @ConditionalOnProperty(
            prefix = VanillaBpProperties.PREFIX + ".cockpit.rest",
            name = "base-url")
    @Qualifier("bpmsApiV1")
    public BpmsApi bpmsApiClient() {
        
        final var config = properties.getCockpit().getRest();
        final var apiClient = new ApiClient().setBasePath(config.getBaseUrl());
        configureFeignBuilder(apiClient.getClass(), apiClient.getFeignBuilder(), config);

        return apiClient.buildClient(BpmsApi.class);
        
    }
    
    @Bean
    @Qualifier(TEMPLATING_QUALIFIER)
    public FreeMarkerConfigurationFactoryBean businessCockpitFreemarker() {

        if (!StringUtils.hasText(properties.getCockpit().getTemplateLoaderPath())) {
            throw new IllegalArgumentException(
                    "Template loader path is not set, used to build template based texts. Set property '"
                    + VanillaBpProperties.PREFIX
                    + ".cockpit.template-loader-path'");
        }
        
        final var result = new FreeMarkerConfigurationFactoryBean() {

            @Override
            protected freemarker.template.Configuration newConfiguration() throws IOException, TemplateException {
                return new freemarker.template.Configuration(FREEMARKER_VERSION);
            }
            
            protected void postProcessConfiguration(
                    final freemarker.template.Configuration config) throws IOException ,TemplateException {
                config.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
                config.setLocalizedLookup(true);
                config.setRecognizeStandardFileExtensions(true);
                final var objectWrapper = new Java8ObjectWrapper(FREEMARKER_VERSION);
                objectWrapper.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
                config.setObjectWrapper(objectWrapper);
            };

        };
        
        result.setTemplateLoaderPath(properties.getCockpit().getTemplateLoaderPath());

        return result;
        
    }
    
    @SuppressWarnings("unchecked")
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <DE> BusinessCockpitService<?> adapterAwareBusinessCockpitService(
            final SpringDataUtil springDataUtil,
            final InjectionPoint injectionPoint) {

        final ParameterizedType bcServiceGenericType;
        if (injectionPoint.getMethodParameter() != null) {
            bcServiceGenericType = 
                    (ParameterizedType) injectionPoint
                    .getMethodParameter()
                    .getGenericParameterType();
        } else if (injectionPoint.getField() != null) {
            bcServiceGenericType =
                    (ParameterizedType) injectionPoint
                    .getField()
                    .getGenericType();
        } else {
            throw new RuntimeException("Unsupported injection of BusinessCockpitService, only field-, constructor- and method-parameter-injection allowed!");
        }
        final Class<DE> workflowAggregateClass = (Class<DE>)
                bcServiceGenericType.getActualTypeArguments()[0];
        
        final var existingService = connectableServices.get(workflowAggregateClass);
        if (existingService != null) {
            return existingService;
        }

        final var workflowAggregateRepository = springDataUtil
                .getRepository(workflowAggregateClass);
        final var workflowAggregateIdClass = springDataUtil
                .getIdType(workflowAggregateClass);

        final var bcServicesByAdapter = adapterConfigurations
                .stream()
                .map(adapter -> Map.entry(
                        adapter.getAdapterId(),
                        adapter.newBusinessCockpitServiceImplementation(
                                springDataUtil,
                                workflowAggregateClass,
                                workflowAggregateIdClass,
                                workflowAggregateRepository)))
                .collect(Collectors
                        .toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue));

        @SuppressWarnings("rawtypes")
        final var result = new AdapterAwareBusinessCockpitService(
                vanillaBpProperties,
                bcServicesByAdapter,
                workflowAggregateIdClass,
                workflowAggregateClass);

        connectableServices.put(workflowAggregateClass, result);

        return result;
            
    }
    
}
