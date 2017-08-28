Dropwizard/Spring
===================================

Welcome to the Dropwizard/Spring project. This is a updated version of the work
 done by Nicolas Huray at [https://github.com/nhuray/dropwizard-spring](https://github.com/nhuray/dropwizard-spring) and
 [https://github.com/the41/dropwizard-spring](https://github.com/the41/dropwizard-spring)


Introduction
------------

[Dropwizard](http://dropwizard.codahale.com) is a Java framework for developing ops-friendly, high-performance, RESTful web services.

[Spring](http://www.springsource.org/spring-framework) is the most popular application development framework for enterprise Java™.

This project provide a simple method for integrating Spring with Dropwizard.


Required Versions
------------

- Java 8 or grater is required.
- Spring version 3.1 or grater is required.  Spring version 4.3.10.RELEASED is used for testing.
- Dropwizard version 0.8 or grater is required.  Dropwizard 1.1 is used for testing.


Maven dependency
------------

```xml
<dependency>
     <groupId>com.bazaarvoice</groupId>
     <artifactId>dropwizard-spring</artifactId>
     <version>2.0.0</version>
</dependency>
```

Usage
------------

The Dropwizard/Spring integration allow to automatically initialize Dropwizard environment through a Spring application context including health checks, resources, providers, tasks and managed.

To use Dropwizard/Spring you just have to add a ```SpringBundle``` and create your Spring application context.

For example :

```java
public class HelloApp extends Service<HelloAppConfiguration> {

    public static void main(String[] args) throws Exception {
      new HelloApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<HelloAppConfiguration> bootstrap) {
      // Create and add the SpringBundle, this will register configuration, environment and placeholder
      bootstrap.addBundle(new SpringBundle<>(applicationContext())
                .registerConfiguration(true)     // Enable/Disable registering the Configuration object,  Default is true
                .withConfigurationBeanName("dw") // Bean name to use when registering the Configuration object, Default is 'dw'
                .registerEnvironment(true)       // Enable/Disable registering the Dropwizard Environment object,  Default is true
                .withEnvironmentBeanName("dwEnv")// Bean name to use when registering the Dropwizard Environment object, Default is 'dwEnv'
                .registerObjectMapper(true)      // Enable/Disable registering Dropwizard's ObjectMapper object,  Default is true
                .withObjectMapperBeanName("dwObjectMapper")// Bean name to use when registering Dropwizard's ObjectMapper object, Default is 'dwObjectMapper'
      );
    }

    @Override
    public void run(HelloAppConfiguration configuration, Environment environment) throws Exception {
        // All beans will be added to the Spring Context during the run() phase
     }


    private ConfigurableApplicationContext applicationContext() throws BeansException {
      // the ApplicationContext must be not active in order to register configuration, environment or objectmapper
      // The easiest way to do this is to use a AnnotationConfigApplicationContext.  If you use a ClassPathXmlApplicationContext, it will automatically be active!
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.register(JavaBeanConfiguration.class);
      context.scan("my.root.package");
      return context;
    }
}
```

In this example we create a Spring application context based on annotation to resolve Spring beans.

The ```SpringBundle``` class use the application context to initialize Dropwizard environment including health checks, resources, providers, tasks and managed.

Moreover the ```SpringBundle``` class register :

 - a ```PropertySource``` to resolve Dropwizard configuration as [Spring placeholders](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/beans.html#beans-factory-placeholderconfigurer) (For example : ```${myConfigurationProperty}```).
 - the Dropwizard configuration with the name ```dw``` to retrieve complex configuration with [Spring Expression Language](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/expressions.html) (For example : ```#{dw.httpConfiguration}```).
 - the Dropwizard environment with the name ```dwEnv``` to retrieve complex configuration with [Spring Expression Language](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/expressions.html) (For example : ```#{dwEnv.validator}```).
 - the Dropwizard ObjectMapper with the name ```dwObjectMapper``` to retrieve complex configuration with [Spring Expression Language](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/expressions.html) (For example : ```#{dwObjectMapper}```).

Please take a look at the hello application located in ```src/test/java/hello```.


License
------------

    The Apache Software License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0.txt
