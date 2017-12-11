/*
 * Copyright 2017 dc-square GmbH
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

import com.hivemq.spi.config.SystemInformation;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Properties;

/**
 * This reads a property file and provides some utility methods for working with {@link Properties}
 *
 * @author Christoph Schäbel
 */
@Singleton
public class GraphiteConfiguration extends ReloadingPropertiesReader {
    private static final Logger log = LoggerFactory.getLogger(GraphiteConfiguration.class);

    private static final String DEFAULT_VALUE_PREFIX = "";
    private static final String DEFAULT_VALUE_BATCH_MODE = "false";
    private static final String DEFAULT_VALUE_BATCH_SIZE = "3";
    private static final String DEFAULT_VALUE_REPORTING_INTERVAL = "60";

    private RestartListener listener;

    @Inject
    public GraphiteConfiguration(final PluginExecutorService pluginExecutorService,
                                 final SystemInformation systemInformation,
                                 final EnvironmentReader environmentReader) {
        super(pluginExecutorService, systemInformation, environmentReader);

        final ValueChangedCallback callback = new ValueChangedCallback() {
            @Override
            public void valueChanged(final Object newValue) {
                if (listener != null) {
                    listener.restart();
                }
            }
        };


        addCallback(ReloadingPropertiesReader.HOST_KEY, callback);

        addCallback(ReloadingPropertiesReader.PORT_KEY, callback);
        addCallback(ReloadingPropertiesReader.BATCH_SIZE_KEY, callback);
        addCallback(ReloadingPropertiesReader.BATCH_MODE_KEY, callback);
        addCallback(ReloadingPropertiesReader.REPORTING_INTERVAL_KEY, callback);
        addCallback(ReloadingPropertiesReader.PREFIX_KEY, callback);
    }

    public boolean isBatchMode() {
        return Boolean.parseBoolean(properties.getProperty(ReloadingPropertiesReader.BATCH_MODE_KEY, DEFAULT_VALUE_BATCH_MODE));
    }

    public String getHost() {
        String strHost = properties.getProperty(ReloadingPropertiesReader.HOST_KEY);
        if (strHost == null) {
            log.error("Host configuration for Graphite Plugin is missing. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }
        return strHost;
    }

    public int getPort() {
        String strPort = properties.getProperty(ReloadingPropertiesReader.PORT_KEY);
        if (strPort == null) {
            log.error("Port configuration for Graphite Plugin is missing. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }
        try {
            return Integer.parseInt(strPort);
        } catch (Exception e) {
            log.error("Port configuration for Graphite Plugin could not be parsed", e);
            throw new UnrecoverableException(false);
        }
    }

    public int getBatchSize() {
        try {
            return Integer.parseInt(properties.getProperty(ReloadingPropertiesReader.BATCH_SIZE_KEY, DEFAULT_VALUE_BATCH_SIZE));
        } catch (Exception e) {
            log.error("Error while parsing configuration of batchSize for Graphite Plugin. Shutting down HiveMQ", e);
            throw new UnrecoverableException(false);
        }
    }

    public int getReportingInterval() {
        try {
            return Integer.parseInt(properties.getProperty(ReloadingPropertiesReader.REPORTING_INTERVAL_KEY, DEFAULT_VALUE_REPORTING_INTERVAL));
        } catch (Exception e) {
            log.error("Error while parsing configuration of reportingInterval for Graphite Plugin. Shutting down HiveMQ", e);
            throw new UnrecoverableException(false);
        }
    }

    public String getPrefix() {
        return properties.getProperty(ReloadingPropertiesReader.PREFIX_KEY, DEFAULT_VALUE_PREFIX);
    }

    @Override
    public String getFilename() {
        return "graphite-plugin.properties";
    }

    public void setRestartListener(final RestartListener listener) {
        this.listener = listener;
    }

    public static interface RestartListener {

        public void restart();

    }
}
