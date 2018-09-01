/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.opentracing.listener;

import com.google.common.collect.HashMultimap;
import com.google.common.eventbus.EventBus;
import io.opentracing.NoopTracerFactory;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.opentracing.ShardingTracer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.lang.reflect.Field;

public abstract class BaseEventListenerTest {
    
    private static final MockTracer TRACER = new MockTracer(new ThreadLocalActiveSpanSource(), MockTracer.Propagator.TEXT_MAP);
    
    protected static MockTracer getTracer() {
        return TRACER;
    }
    
    @BeforeClass
    public static void initTracer() {
        ShardingTracer.init(TRACER);
    }
    
    @AfterClass
    public static void releaseTracer() throws NoSuchFieldException, IllegalAccessException {
        Field tracerField = GlobalTracer.class.getDeclaredField("tracer");
        tracerField.setAccessible(true);
        tracerField.set(GlobalTracer.class, NoopTracerFactory.create());
        Field subscribersByTypeField = EventBus.class.getDeclaredField("subscribersByType");
        subscribersByTypeField.setAccessible(true);
        subscribersByTypeField.set(ShardingEventBusInstance.getInstance(), HashMultimap.create());
    }
    
    @Before
    public void resetTracer() {
        TRACER.reset();
    }
}
