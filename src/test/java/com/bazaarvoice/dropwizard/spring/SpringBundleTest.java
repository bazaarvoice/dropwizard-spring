package com.bazaarvoice.dropwizard.spring;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import hello.config.HelloAppConfiguration;
import hello.config.HelloBinder;
import hello.config.HelloConfiguration;
import hello.health.HelloHealthCheck;
import hello.resources.HelloResource;
import hello.server_lifecycle_listeners.HelloServerLifecycleListener;
import hello.service.HelloService;
import hello.tasks.HelloTask;
import io.dropwizard.Configuration;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.ws.rs.container.ContainerResponseFilter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings ("unchecked")
public class SpringBundleTest {

    @Mock
    private Environment environment;

    @Mock
    private LifecycleEnvironment lifecycleEnvironment;

    @Mock
    private AdminEnvironment adminEnvironment;

    @Mock
    private HealthCheckRegistry healthCheckRegistry;

    @Mock
    private JerseyEnvironment jerseyEnvironment;

    @Mock
    private DropwizardResourceConfig resourceConfig;

    @Mock
    private ObjectMapper objectMapper;

    private HelloAppConfiguration configuration;

    private SpringBundle bundle;

    private AnnotationConfigApplicationContext context;

    @Before
    public void setup() {
        context = new AnnotationConfigApplicationContext();
        context.scan("hello");
        bundle = new SpringBundle(context);

        MockitoAnnotations.initMocks(this);

        HelloConfiguration hello = new HelloConfiguration();
        hello.setMessage("Hello");

        configuration = new HelloAppConfiguration();
        configuration.setHello(hello);

        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.admin()).thenReturn(adminEnvironment);
        when(environment.healthChecks()).thenReturn(healthCheckRegistry);
        when(environment.jersey()).thenReturn(jerseyEnvironment);

        when(jerseyEnvironment.getResourceConfig()).thenReturn(resourceConfig);
    }

    @Test
    public void registerResources() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<HelloResource> resource = ArgumentCaptor.forClass(HelloResource.class);
        verify(environment.jersey()).register(resource.capture());
        assertThat(resource.getValue(), is(HelloResource.class));
    }

    @Test
    public void registerHealthChecks() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<? extends HealthCheck> healthCheck = ArgumentCaptor.forClass(HealthCheck.class);
        verify(environment.healthChecks()).register(eq(HelloHealthCheck.class.getName()), healthCheck.capture());
        assertThat(healthCheck.getValue(), is(HelloHealthCheck.class));
    }

    @Test
    public void registerContainerResponseFilters() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<? extends ContainerResponseFilter> responseFilter = ArgumentCaptor.forClass(ContainerResponseFilter.class);
        verify(environment.jersey().getResourceConfig(), times(2)).register(responseFilter.capture());
        assertThat(responseFilter.getValue(), is(ContainerResponseFilter.class));
    }

    @Test
    public void registerHelloInjectionResolver() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<HelloBinder> resource = ArgumentCaptor.forClass(HelloBinder.class);
        verify(environment.jersey().getResourceConfig(), times(2)).register(resource.capture());
        assertThat(resource.getAllValues().get(0), is(HelloBinder.class));
    }

    @Test
    public void registerTasks() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<? extends Task> task = ArgumentCaptor.forClass(Task.class);
        verify(environment.admin()).addTask(task.capture());
        assertThat(task.getValue(), is(HelloTask.class));
    }

    @Test
    public void registerServerLifecycleListener() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<? extends ServerLifecycleListener> listener = ArgumentCaptor.forClass(ServerLifecycleListener.class);
        verify(environment.lifecycle()).addServerLifecycleListener(listener.capture());
        assertThat(listener.getValue(), is(HelloServerLifecycleListener.class));
    }

    @Test
    public void registerConfiguration() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<HelloResource> resource = ArgumentCaptor.forClass(HelloResource.class);
        verify(environment.jersey()).register(resource.capture());

        HelloResource r = resource.getValue();
        assertThat(r.getPort(), is(8080)); // Defaut port
    }

    @Test
    public void registerEnvironment() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        assertThat(context.getBean("dwEnv"), instanceOf(Environment.class));
    }


    @Test
    public void wiresUpDependencies() throws Exception {
        // When
        bundle.run(configuration, environment);

        // Then
        ArgumentCaptor<HelloResource> resource = ArgumentCaptor.forClass(HelloResource.class);
        verify(environment.jersey()).register(resource.capture());

        HelloResource r = resource.getValue();
        final HelloService helloService = r.getHelloService();
        assertNotNull(helloService);
        assertThat(helloService.getMessage(), is("Hello"));

        assertThat(r.getConfiguration(), instanceOf(Configuration.class));
        assertThat(r.getEnvironment(), instanceOf(Environment.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void unableToRegisterConfigurationIfSpringContextIsActive() throws Exception {
        // When
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("test"); // active context
        bundle = new SpringBundle(context)
                .registerConfiguration(true)
                .registerEnvironment(false)
                .registerObjectMapper(false);
        bundle.run(configuration, environment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unableToRegisterEnvironmentIfSpringContextIsActive() throws Exception {
        // When
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("test"); // active context
        bundle = new SpringBundle(context)
                .registerConfiguration(false)
                .registerEnvironment(true)
                .registerObjectMapper(false);
        bundle.run(configuration, environment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unableToRegisterObjectMapperIfSpringContextIsActive() throws Exception {
        // When
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("test"); // active context
        bundle = new SpringBundle(context)
                .registerConfiguration(false)
                .registerEnvironment(false)
                .registerObjectMapper(true);
        bundle.run(configuration, environment);
    }
}
