package com.bazaarvoice.dropwizard.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationPropertySourceTest {

    private ConfigurationPropertySource<TestConfiguration> propertySource;

    @Test
    public void simpleTest() throws Exception {
        TestObject tObject = new TestObject();
        tObject.first = "test1";
        tObject.second = new String[] {"test2", "test3"};
        tObject.third = 4;

        TestConfiguration testBean = new TestConfiguration();
        testBean.test = tObject;

        propertySource = new ConfigurationPropertySource<>(testBean, new ObjectMapper());

        // Then
        Assert.assertEquals("test1", propertySource.getProperty("test.first"));
        Assert.assertEquals("test2", propertySource.getProperty("test.second[0]"));
        Assert.assertEquals("test3", propertySource.getProperty("test.second[1]"));
        Assert.assertEquals("test2,test3", propertySource.getProperty("test.second"));
        Assert.assertEquals("4", propertySource.getProperty("test.third"));
    }

    private class TestConfiguration extends Configuration {
        private TestObject test;

        public TestObject getTest() {
            return test;
        }
    }

    private class TestObject {
        private String first;
        private String[] second;
        private int third;

        public String getFirst() {
            return first;
        }

        public String[] getSecond() {
            return second;
        }

        public int getThird() {
            return third;
        }
    }
}
