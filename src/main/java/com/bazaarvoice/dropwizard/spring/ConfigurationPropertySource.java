package com.bazaarvoice.dropwizard.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings ("WeakerAccess")
public class ConfigurationPropertySource<T extends Configuration> extends EnumerablePropertySource<T> {
    private Map<String, Object> sourceMap = new HashMap<>();

    public ConfigurationPropertySource(T configuration, ObjectMapper mapper)
            throws JsonProcessingException {
        super("dwConfigurationPropertySource", configuration);
        loadData(configuration, mapper);
    }

    private void loadData(T configuration, ObjectMapper mapper)
            throws JsonProcessingException {
        Map<String, Object> inputMap = mapper.convertValue(configuration, new TypeReference<Map<String,Object>>(){});
        assignProperties(inputMap, null);
    }

    private void assignProperties(Map<String, Object> input, String path) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.hasText(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + "." + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                sourceMap.put(key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                assignProperties(map, key);
            } else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                sourceMap.put(key, StringUtils.collectionToCommaDelimitedString(collection));
                int count = 0;
                for (Object object : collection) {
                    assignProperties(Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            } else {
                sourceMap.put(key, value == null ? "" : String.valueOf(value));
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        return sourceMap.get(name);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.sourceMap.keySet());
    }
}
