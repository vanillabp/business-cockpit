package io.vanillabp.cockpit.adapter.common;

import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.AdapterConfigurationBase;
import io.vanillabp.cockpit.adapter.common.usertask.*;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowRestMapper;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowRestPublishing;
import io.vanillabp.cockpit.bpms.api.v1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.commons.rest.adapter.ClientsConfigurationBase;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
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
import javax.annotation.PostConstruct;

@AutoConfigurationPackage(basePackageClasses = CockpitCommonAdapterConfiguration.class)
@AutoConfigureAfter(WorkflowModulePropertiesConfiguration.class)
@EnableConfigurationProperties({ CockpitProperties.class, UserTasksWorkflowProperties.class })
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
    private CockpitProperties properties;
    
    @Autowired
    private UserTasksWorkflowProperties workflowsCockpitProperties;
    
    private Map<Class<?>, AdapterAwareBusinessCockpitService<?>> connectableServices = new HashMap<>();
    
    @Autowired
    private List<AdapterConfigurationBase<?>> adapterConfigurations;
    
    @PostConstruct
    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        
        final var uiUriTypeGlobal = findAndValidateUiUriTypeProperty(
                CockpitProperties.PREFIX, properties.getUiUriType());
        
        if (workflowModulesProperties.isEmpty()) {
            final var modulesAvailable = workflowModulesProperties
                    .stream()
                    .map(WorkflowModuleProperties::getWorkflowModuleId)
                    .collect(Collectors.joining("', '", "'", "'"));
            throw new RuntimeException("To use VanillaBP business cockpit you have to add a "
                    + "list of properties for each workflow-module: 'vanillabp.workflows.*' "
                    + "having sub-property 'workflow-module-id' in: "
                    + modulesAvailable);
        }
        
        workflowModulesProperties
                .forEach(workflowModuleProperties -> {
                    if (workflowsCockpitProperties
                            .getWorkflows()
                            .isEmpty()) {
                        final var modulesAvailable = workflowModulesProperties
                                .stream()
                                .map(WorkflowModuleProperties::getWorkflowModuleId)
                                .collect(Collectors.joining("', '", "'", "'"));
                        throw new RuntimeException("To use VanillaBP business cockpit you have to add a "
                                + "list of properties for each workflow-module: 'vanillabp.workflows.*' "
                                + "having sub-property 'workflow-module-id' in: "
                                + modulesAvailable);
                    }
                    workflowsCockpitProperties
                            .getWorkflows()
                            .stream()
                            .filter(workflowProperties -> workflowProperties.getBpmnProcessId() == null) // ignored for validation
                            .peek(workflowProperties -> {
                                if (!StringUtils.hasText(workflowProperties.getWorkflowModuleUri())) {
                                    throw new RuntimeException(
                                            "Property '"
                                            + VanillaBpProperties.PREFIX
                                            + "'.workflows[workflow-module-id = "
                                            + workflowModuleProperties.getWorkflowModuleId()
                                            + "'].workflow-module-uri' is not set!");
                                }
                            })
                            .peek(workflowProperties -> {
                                if (!StringUtils.hasText(workflowProperties.getTaskProviderApiPath())) {
                                    throw new RuntimeException(
                                            "Property '"
                                            + VanillaBpProperties.PREFIX
                                            + "'.workflows[workflow-module-id = "
                                            + workflowModuleProperties.getWorkflowModuleId()
                                            + "'].task-provider-api-path' is not set!");
                                }
                            })
                            .filter(workflowProperties -> workflowProperties.getWorkflowModuleId()
                                    .equals(workflowModuleProperties.getWorkflowModuleId()))
                            .findFirst()
                            .ifPresentOrElse(
                                    workflowProperties -> {
                                        if (!StringUtils.hasText(workflowProperties.getUiUriPath())) {
                                            throw new RuntimeException(
                                                    "Property '"
                                                    + VanillaBpProperties.PREFIX
                                                    + "'.workflows[workflow-module-id = "
                                                    + workflowModuleProperties.getWorkflowModuleId()
                                                    + "'].ui-uri-path' is not set!");
                                        }
                                        final var uiUriTypeModule = findAndValidateUiUriTypeProperty(
                                                VanillaBpProperties.PREFIX
                                                + ".workflows[workflow-module-id = '"
                                                + workflowModuleProperties.getWorkflowModuleId()
                                                + "']", workflowProperties.getUiUriType());
                                        workflowProperties
                                                .getUserTasks()
                                                .entrySet()
                                                .forEach(entry -> {
                                                    final var uiUriType = findAndValidateUiUriTypeProperty(
                                                            VanillaBpProperties.PREFIX
                                                            + ".workflows[workflow-module-id = " 
                                                            + workflowModuleProperties.getWorkflowModuleId()
                                                            + "]",
                                                            properties.getUiUriType());
                                                    if ((uiUriTypeGlobal == null)
                                                            && (uiUriTypeModule == null)
                                                            && (uiUriType == null)) {
                                                        throw new RuntimeException(
                                                                "Neither property '"
                                                                + CockpitProperties.PREFIX
                                                                + "'.ui-uri-type' nor '"
                                                                + VanillaBpProperties.PREFIX
                                                                + "'.workflows[workflow-module-id = "
                                                                + workflowModuleProperties.getWorkflowModuleId()
                                                                + "'].ui-uri-type' nor '"
                                                                + VanillaBpProperties.PREFIX
                                                                + "'.workflows[workflow-module-id = "
                                                                + workflowModuleProperties.getWorkflowModuleId()
                                                                + "'].user-tasks."
                                                                + entry.getKey()
                                                                + ".ui-uri-type' nor '"
                                                                + "' was set! One of them is mandatory.");
                                                    }
                                                });
                                    },
                                    () -> {
                                        throw new IllegalArgumentException(
                                                "No properties '"
                                                + VanillaBpProperties.PREFIX
                                                + ".workflows[workflow-module-id="
                                                + workflowModuleProperties.getWorkflowModuleId()
                                                + "]' found! This configuration is required for each workflow module.");
                                    });
                });
        
        
    }
    
    @Bean
    public UserTaskPublishing userTaskPublishing(
            @Qualifier("bpmsApiV1")
            final Optional<BpmsApi> bpmsApi,
            final UserTaskRestMapper userTaskRestMapper) {

        return new UserTaskRestPublishing(
                workerId,
                bpmsApi,
                properties,
                workflowsCockpitProperties,
                userTaskRestMapper);

    }

    @Bean
    public WorkflowPublishing workflowPublishing(
            @Qualifier("bpmsApiV1")
            final Optional<BpmsApi> bpmsApi,
            final WorkflowRestMapper workflowRestMapper) {
        return new WorkflowRestPublishing(
                workerId,
                bpmsApi,
                properties,
                workflowsCockpitProperties,
                workflowRestMapper);

    }

    @Bean
    public WorkflowRestMapper workflowRestMapper(){
        return new WorkflowRestMapper();
    }


    @Bean
    public UserTaskRestMapper userTaskRestMapper(){
        return new UserTaskRestMapper();
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
            prefix = CockpitProperties.PREFIX + ".client",
            name = "base-url")
    @Qualifier("bpmsApiV1")
    public BpmsApi bpmsApiClient(
            final CockpitProperties properties) {
        
        final var config = properties.getClient();
        final var apiClient = new ApiClient().setBasePath(config.getBaseUrl());
        configureFeignBuilder(apiClient.getClass(), apiClient.getFeignBuilder(), config);

        return apiClient.buildClient(BpmsApi.class);
        
    }
    
    @Bean
    @Qualifier("BpmsApi")
    public ApiVersionAware bpmsApiVersion() {
        
        return () -> "v1";
        
    }
    
    @Bean
    @Qualifier(TEMPLATING_QUALIFIER)
    public FreeMarkerConfigurationFactoryBean businessCockpitFreemarker(
            final CockpitProperties properties,
            final UserTasksWorkflowProperties usertasksProperties) {
        
        final var templatingActive = usertasksProperties
                .getWorkflows()
                .stream()
                .flatMap(ut -> ut.getUserTasks().values().stream())
                .map(UserTaskProperties::getTemplatesPath)
                .anyMatch(java.util.Objects::nonNull);
        if (!templatingActive) {
            return null;
        }
        
        usertasksProperties
                .getWorkflows()
                .stream()
                .peek(workflowProperties -> {
                    if (!StringUtils.hasText(workflowProperties.getTemplatesPath())) {
                        workflowProperties.setTemplatesPath("");
                    }
                })
                .flatMap(workflowProperties
                        -> workflowProperties.getUserTasks().values().stream())
                .forEach(userTaskProperties -> {
                    if (!StringUtils.hasText(userTaskProperties.getTemplatesPath())) {
                        userTaskProperties.setTemplatesPath("");
                    }
                });
        
        if (!StringUtils.hasText(properties.getTemplateLoaderPath())) {
            throw new IllegalArgumentException(
                    "Neither Spring property 'spring.freemarker.template-loader-path' nor '"
                    + VanillaBpProperties.PREFIX
                    + ".template-loader-path' is set but templating was activated by setting properties '"
                    + VanillaBpProperties.PREFIX
                    + ".workflows[*].user-tasks[*].template-path'!");
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
        
        result.setTemplateLoaderPath(properties.getTemplateLoaderPath());

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
        
        validateConfiguration();

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
