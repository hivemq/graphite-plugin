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

package com.hivemq.plugins.metrics.graphite.callbacks;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.PickledGraphite;
import com.hivemq.plugins.metrics.graphite.utils.GraphiteConfiguration;
import com.hivemq.spi.callback.CallbackPriority;
import com.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.hivemq.spi.callback.exception.BrokerUnableToStartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Schaebel
 */
public class GraphiteReporting implements OnBrokerStart, OnBrokerStop {

    Logger log = LoggerFactory.getLogger(GraphiteReporting.class);

    private final MetricRegistry metricRegistry;
    private final GraphiteConfiguration graphiteConfiguration;
    private GraphiteSender graphite;
    private GraphiteReporter reporter;

    @Inject
    public GraphiteReporting(final MetricRegistry metricRegistry,
                             final GraphiteConfiguration graphiteConfiguration) {
        this.metricRegistry = metricRegistry;
        this.graphiteConfiguration = graphiteConfiguration;
    }

    @Override
    public void onBrokerStart() throws BrokerUnableToStartException {

        startGraphiteReporting();

        addRestartListener();
    }

    @Override
    public void onBrokerStop()
    {
       //if reporter was not initiated yet
        if(reporter != null){
            reporter.stop();
        }
    }

    @Override
    public int priority() {
        return CallbackPriority.MEDIUM;
    }


    private void addRestartListener() {

        graphiteConfiguration.setRestartListener(new GraphiteConfiguration.RestartListener() {
            @Override
            public void restart() {
                reporter.stop();

                startGraphiteReporting();
            }
        });

    }

    private void startGraphiteReporting() {
        setupGraphiteSender();
        setupGraphiteReporter();

        reporter.start(graphiteConfiguration.getReportingInterval(), TimeUnit.SECONDS);
    }

    private void setupGraphiteReporter() {
        String prefix = graphiteConfiguration.getPrefix();
        if (prefix == null) {
            prefix = "";
        }

        reporter = GraphiteReporter.forRegistry(metricRegistry)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
    }

    private void setupGraphiteSender() {

        final String host = graphiteConfiguration.getHost();
        final int port = graphiteConfiguration.getPort();

        if (graphiteConfiguration.isBatchMode()) {
            log.info("Creating batched Graphite sender for server {}:{}", host, port);
            graphite = new PickledGraphite(host, port, graphiteConfiguration.getBatchSize());
        } else {
            log.info("Creating non-batched Graphite sender for server {}:{}", host, port);
            graphite = new Graphite(host, port);
        }
    }

}
