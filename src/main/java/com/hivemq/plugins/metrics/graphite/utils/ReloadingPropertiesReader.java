/*
 * Copyright 2015 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.plugins.metrics.graphite.utils;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.config.SystemInformation;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Schäbel
 */
public abstract class ReloadingPropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(GraphiteConfiguration.class);

    private static final String ENV_VARIABLES_PREFIX = "HIVEMQ_GRAPHITE_";

    private final PluginExecutorService pluginExecutorService;
    private final SystemInformation systemInformation;
    private final EnvironmentReader environmentReader;
    private File file;
    protected Properties properties;
    protected Map<String, List<ValueChangedCallback<String>>> callbacks = Maps.newHashMap();

    public ReloadingPropertiesReader(final PluginExecutorService pluginExecutorService,
                                     final SystemInformation systemInformation,
                                     final EnvironmentReader environmentReader) {
        this.pluginExecutorService = pluginExecutorService;
        this.systemInformation = systemInformation;
        this.environmentReader = environmentReader;
    }

    @PostConstruct
    public void postConstruct() {

        file = new File(systemInformation.getConfigFolder() + "/" + getFilename());

        try {
            loadProperties();
        } catch (IOException e) {
            log.error("Not able to load configuration file {}", file.getAbsolutePath());
            properties = new Properties();
        }

        pluginExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                reload();
            }
        }, 10, 3, TimeUnit.SECONDS);
    }

    @NotNull
    public abstract String getFilename();

    /**
     * Reloads the specified .properties file
     */
    public void reload() {

        Map<String, String> oldValues = getCurrentValues();

        try {
            loadProperties();

            Map<String, String> newValues = getCurrentValues();

            logChanges(oldValues, newValues);

        } catch (IOException e) {
            log.debug("Not able to reload configuration file {}", this.file.getAbsolutePath());
        }
    }

    public void addCallback(final String propertyName, final ValueChangedCallback<String> changedCallback) {

        if (!callbacks.containsKey(propertyName)) {
            callbacks.put(propertyName, Lists.<ValueChangedCallback<String>>newArrayList());
        }

        callbacks.get(propertyName).add(changedCallback);
    }

    private Map<String, String> getCurrentValues() {
        Map<String, String> values = Maps.newHashMap();
        for (String key : properties.stringPropertyNames()) {
            values.put(key, properties.getProperty(key));
        }
        return values;
    }

    private void loadProperties() throws IOException {
        final Properties fileProperties = new Properties();
        fileProperties.load(new FileReader(file));

        final Map<String, String> propertiesMap = Maps.newHashMap(Maps.fromProperties(fileProperties));
        for (String key : propertiesMap.keySet()) {
            final String environmentVariableName = getEnvironmentVariableName(key);
            final Optional<String> environmentVariableValue = environmentReader.getEnvironmentVariable(environmentVariableName);

            if (environmentVariableValue.isPresent()) {
                propertiesMap.put(key, environmentVariableValue.get());
            }
        }
        properties = new Properties();
        properties.putAll(propertiesMap);
    }

    private String getEnvironmentVariableName(String key) {
        return ENV_VARIABLES_PREFIX + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, key);
    }

    private void logChanges(final Map<String, String> oldValues, final Map<String, String> newValues) {
        final MapDifference<String, String> difference = Maps.difference(oldValues, newValues);

        for (Map.Entry<String, MapDifference.ValueDifference<String>> stringValueDifferenceEntry : difference.entriesDiffering().entrySet()) {
            log.debug("Plugin configuration {} changed from {} to {}",
                    stringValueDifferenceEntry.getKey(), stringValueDifferenceEntry.getValue().leftValue(),
                    stringValueDifferenceEntry.getValue().rightValue());

            if (callbacks.containsKey(stringValueDifferenceEntry.getKey())) {
                for (ValueChangedCallback<String> callback : callbacks.get(stringValueDifferenceEntry.getKey())) {
                    callback.valueChanged(stringValueDifferenceEntry.getValue().rightValue());
                }
            }
        }

        for (Map.Entry<String, String> stringStringEntry : difference.entriesOnlyOnLeft().entrySet()) {
            log.debug("Plugin configuration {} removed", stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        for (Map.Entry<String, String> stringStringEntry : difference.entriesOnlyOnRight().entrySet()) {
            log.debug("Plugin configuration {} added: {}", stringStringEntry.getValue(), stringStringEntry.getValue());
        }
    }

    @NotNull
    public Properties getProperties() {
        return properties;
    }


}
