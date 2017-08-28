package com.bazaarvoice.dropwizard.spring;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

/**
 * A bundle which load Spring Application context to automatically initialize Dropwizard {@link Environment}
 * including health checks, resources, providers, tasks and managed.
 */
@SuppressWarnings ({"WeakerAccess", "unused"})
public class SpringBundle<T extends Configuration> implements ConfiguredBundle<T> {

    public static final String DEFAULT_CONFIGURATION_BEAN_NAME = "dw";
    public static final String DEFAULT_ENVIRONMENT_BEAN_NAME = "dwEnv";
    public static final String DEFAULT_OBJECT_MAPPER_BEAN_NAME = "dwObjectMapper";

    private static final Logger LOG = LoggerFactory.getLogger(SpringBundle.class);

    private String configurationBeanName = DEFAULT_CONFIGURATION_BEAN_NAME;
    private String environmentBeanName = DEFAULT_ENVIRONMENT_BEAN_NAME;
    private String objectMapperBeanName = DEFAULT_OBJECT_MAPPER_BEAN_NAME;
    private ConfigurableApplicationContext context;
    private boolean registerConfiguration = true;
    private boolean registerEnvironment = true;
    private boolean registerObjectMapper = true;

    /**
     * Creates a new SpringBundle to automatically initialize Dropwizard {@link Environment}
     * <p/>
     *
     * @param context the application context to load
     */
    public SpringBundle(ConfigurableApplicationContext context) {
        this.context = context;
    }

    /**
     * Enable/Disable registering the Configuration object,  Default is true
     */
    public SpringBundle<T> registerConfiguration(boolean registerConfiguration) {
        this.registerConfiguration = registerConfiguration;
        return this;
    }

    /**
     * The Bean name used when registering the Configuration Object,  Default is 'dw'
     */
    public SpringBundle<T> withConfigurationBeanName(String configurationBeanName) {
        this.configurationBeanName = configurationBeanName;
        return this;
    }

    /**
     * Enable/Disable registering the Dropwizard Environment object,  Default is true
     */
    public SpringBundle<T> registerEnvironment(boolean registerEnvironment) {
        this.registerEnvironment = registerEnvironment;
        return this;
    }

    /**
     * The Bean name used when registering the Dropwizard Environment object,  Default is 'dwEnv'
     */
    public SpringBundle<T> withEnvironmentBeanName(String environmentBeanName) {
        this.environmentBeanName = environmentBeanName;
        return this;
    }

    /**
     * Enable/Disable registering Dropwizard's ObjectMapper object,  Default is true
     */
    public SpringBundle<T> registerObjectMapper(boolean registerObjectMapper) {
        this.registerObjectMapper = registerObjectMapper;
        return this;
    }

    /**
     * The Bean name used when registering Dropwizard's ObjectMapper object,  Default is 'dwObjectMapper'
     */
    public SpringBundle<T> withObjectMapperBeanName(String objectMapperBeanName) {
        this.objectMapperBeanName = objectMapperBeanName;
        return this;
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        if (registerConfiguration || registerEnvironment  || registerObjectMapper) {
            Preconditions.checkArgument(!context.isActive(), "Context must be not active in order to register configuration, environment, or objectmapper");
        }

        // Register Dropwizard Configuration as a Bean Spring.
        if (registerConfiguration) registerConfiguration(environment, configuration, context);

        // Register the Dropwizard environment
        if (registerEnvironment) registerEnvironment(environment, context);

        // Register the Dropwizard objectMapper
        if (registerObjectMapper) registerObjectMapper(environment.getObjectMapper(), context);

        // Refresh context if is not active
        if (!context.isActive()) context.refresh();

        // Initialize Dropwizard environment
        registerManaged(environment, context);
        registerLifecycle(environment, context);
        registerServerLifecycleListeners(environment, context);
        registerTasks(environment, context);
        registerHealthChecks(environment, context);
        registerInjectionResolverBinders(environment, context);
        registerProviders(environment, context);
        registerContainerResponseFilters(environment, context);
        registerResources(environment, context);

        environment.lifecycle().manage(new Managed() {
            @Override
            public void start(){}

            @Override
            public void stop() {
                context.stop();
            }
        });
    }


    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // nothing doing
    }

    public ConfigurableApplicationContext getContext() {
        return context;
    }

    // ~ Dropwizard Environment initialization methods -----------------------------------------------------------------

    /**
     * Register {@link Managed}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerManaged(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, Managed> beansOfType = context.getBeansOfType(Managed.class);
        for (String beanName : beansOfType.keySet()) {
            // Add managed to Dropwizard environment
            Managed managed = beansOfType.get(beanName);
            environment.lifecycle().manage(managed);
            LOG.info("Registering managed: " + managed.getClass().getName());
        }
    }


    /**
     * Register {@link LifeCycle}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerLifecycle(Environment environment, ConfigurableApplicationContext context) {
        Map<String, LifeCycle> beansOfType = context.getBeansOfType(LifeCycle.class);
        for (String beanName : beansOfType.keySet()) {
            // Add lifeCycle to Dropwizard environment
            if (!beanName.equals(environmentBeanName)) {
                LifeCycle lifeCycle = beansOfType.get(beanName);
                environment.lifecycle().manage(lifeCycle);
                LOG.info("Registering lifeCycle: " + lifeCycle.getClass().getName());
            }
        }
    }


    /**
     * Register {@link ServerLifecycleListener}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerServerLifecycleListeners(Environment environment, ConfigurableApplicationContext context) {
        Map<String, ServerLifecycleListener> beansOfType = context.getBeansOfType(ServerLifecycleListener.class);
        for (String beanName : beansOfType.keySet()) {
            // Add serverLifecycleListener to Dropwizard environment
            if (!beanName.equals(environmentBeanName)) {
                ServerLifecycleListener serverLifecycleListener = beansOfType.get(beanName);
                environment.lifecycle().addServerLifecycleListener(serverLifecycleListener);
                LOG.info("Registering serverLifecycleListener: " + serverLifecycleListener.getClass().getName());
            }
        }
    }


  /**
     * Register {@link Task}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerTasks(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, Task> beansOfType = context.getBeansOfType(Task.class);
        for (String beanName : beansOfType.keySet()) {
            // Add task to Dropwizard environment
            Task task = beansOfType.get(beanName);
            environment.admin().addTask(task);
            LOG.info("Registering task: " + task.getClass().getName());
        }
    }


    /**
     * Register {@link HealthCheck}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerHealthChecks(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, HealthCheck> beansOfType = context.getBeansOfType(HealthCheck.class);
        for (String beanName : beansOfType.keySet()) {
            // Add healthCheck to Dropwizard environment
            HealthCheck healthCheck = beansOfType.get(beanName);
            environment.healthChecks().register(healthCheck.getClass().getName(), healthCheck);
            LOG.info("Registering healthCheck: " + healthCheck.getClass().getName());
        }
    }


    /**
     * Register {@link InjectionResolver}s in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerInjectionResolverBinders(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, AbstractBinder> beansOfType = context.getBeansOfType(AbstractBinder.class);
        for (String beanName : beansOfType.keySet()) {
            // Add InjectionResolver to Dropwizard environment with Binder
            final AbstractBinder binder = beansOfType.get(beanName);
            environment.jersey().getResourceConfig().register(binder);
            LOG.info("Registering injection resolver binder: " + binder.getClass().getName());
        }
    }

    /**
     * Register objects annotated with {@link Provider} in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerProviders(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Provider.class);
        for (String beanName : beansWithAnnotation.keySet()) {
            // Add injectableProvider to Dropwizard environment
            Object provider = beansWithAnnotation.get(beanName);
            environment.jersey().register(provider);
            LOG.info("Registering provider : " + provider.getClass().getName());
        }
    }

    private void registerContainerResponseFilters(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, ContainerResponseFilter> beansOfType = context.getBeansOfType(ContainerResponseFilter.class);
        for (String beanName : beansOfType.keySet()) {
            ContainerResponseFilter responseFilter = beansOfType.get(beanName);
            environment.jersey().getResourceConfig().register(responseFilter);
            LOG.info("Registering ContainerResponseFilter: " + responseFilter.getClass().getName());
        }
    }

    /**
     * Register resources annotated with {@link Path} in Dropwizard {@link Environment} from Spring application context.
     *
     * @param environment the Dropwizard environment
     * @param context     the Spring application context
     */
    private void registerResources(Environment environment, ConfigurableApplicationContext context) {
        final Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Path.class);
        for (String beanName : beansWithAnnotation.keySet()) {
            // Add injectableProvider to Dropwizard environment
            Object resource = beansWithAnnotation.get(beanName);
            environment.jersey().register(resource);
            LOG.info("Registering resource : " + resource.getClass().getName());
        }
    }

    private void registerConfiguration(Environment environment, T configuration, ConfigurableApplicationContext context)
            throws IOException {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        // Register the Configuration object
        beanFactory.registerSingleton(configurationBeanName, configuration);
        LOG.info("Registering Dropwizard Configuration under name : " + configuration);

        // Add a PropertySource to resolve configuration as properties
        context.getEnvironment().getPropertySources().addFirst(new ConfigurationPropertySource<>(configuration, environment.getObjectMapper()));
    }


    private void registerEnvironment(Environment environment, ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton(environmentBeanName, environment);
        LOG.info("Registering Dropwizard Environment under name : " + environmentBeanName);
    }

    /**
     * Register Dropwizard {@link ObjectMapper} as a Spring Bean.
     *
     * @param objectMapper Dropwizard {@link ObjectMapper}
     * @param context      spring application context
     */
    private void registerObjectMapper(ObjectMapper objectMapper, ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton(objectMapperBeanName, objectMapper);
        LOG.info("Registering Dropwizard ObjectMapper under name : " + objectMapperBeanName);
    }
}
