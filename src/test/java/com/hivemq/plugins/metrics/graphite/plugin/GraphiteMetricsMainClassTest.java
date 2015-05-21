package com.hivemq.plugins.metrics.graphite.plugin;

import com.hivemq.plugins.metrics.graphite.callbacks.GraphiteReporting;
import com.hivemq.spi.callback.CallbackPriority;
import com.hivemq.spi.callback.registry.CallbackRegistry;
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
public class GraphiteMetricsMainClassTest {

    @Mock
    GraphiteReporting graphiteReporting;

    @Mock
    CallbackRegistry callbackRegistry;

    private GraphiteMetricsMainClass mainClass;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(graphiteReporting.priority()).thenReturn(CallbackPriority.MEDIUM);

        mainClass = new TestGraphiteMetricsMainClass(graphiteReporting, callbackRegistry);
    }

    @Test
    public void test_post_construct() throws Exception {


        mainClass.postConstruct();

        verify(callbackRegistry, times(1)).addCallback(any(GraphiteReporting.class));

    }

    private static class TestGraphiteMetricsMainClass extends GraphiteMetricsMainClass {

        private final CallbackRegistry callbackRegistry;

        public TestGraphiteMetricsMainClass(final GraphiteReporting graphiteReporting,
                                            final CallbackRegistry callbackRegistry) {
            super(graphiteReporting);
            this.callbackRegistry = callbackRegistry;
        }

        @Override
        public CallbackRegistry getCallbackRegistry() {
            return callbackRegistry;
        }
    }

}