package com.hivemq.plugins.metrics.graphite.callbacks;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.plugins.metrics.graphite.utils.GraphiteConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class GraphiteReportingTest {

    @Mock
    GraphiteConfiguration graphiteConfiguration;

    @Mock
    MetricRegistry metricRegistry;

    private GraphiteReporting graphiteReporting;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        graphiteReporting = new GraphiteReporting(metricRegistry, graphiteConfiguration);
    }

    @Test
    public void test_onBrokerStart() throws Exception {

        when(graphiteConfiguration.getReportingInterval()).thenReturn(5);

        graphiteReporting.onBrokerStart();

        verify(graphiteConfiguration, times(1)).setRestartListener(any(GraphiteConfiguration.RestartListener.class));
    }

}