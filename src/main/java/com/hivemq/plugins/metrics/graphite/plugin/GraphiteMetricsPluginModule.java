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

package com.hivemq.plugins.metrics.graphite.plugin;

import com.google.inject.Singleton;
import com.hivemq.plugins.metrics.graphite.utils.EnvironmentReader;
import com.hivemq.plugins.metrics.graphite.utils.SystemEnvironmentReader;
import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.plugin.meta.Information;


/**
 * @author Christoph Schaebel
 */
@Information(name = "HiveMQ Graphite Metrics Plugin", author = "dc-square GmbH", version = "3.1.1")
public class GraphiteMetricsPluginModule extends HiveMQPluginModule {

    /**
     * This method is provided to execute some custom plugin configuration stuff. Is is the place
     * to execute Google Guice bindings,etc if needed.
     */
    @Override
    protected void configurePlugin() {
        bind(EnvironmentReader.class).to(SystemEnvironmentReader.class).in(Singleton.class);
    }

    /**
     * This method needs to return the main class of the plugin.
     *
     * @return callback priority
     */
    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return GraphiteMetricsMainClass.class;
    }
}
