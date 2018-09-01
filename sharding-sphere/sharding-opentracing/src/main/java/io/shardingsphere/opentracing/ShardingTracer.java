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

package io.shardingsphere.opentracing;

import com.google.common.base.Preconditions;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.shardingsphere.core.exception.ShardingException;
import io.shardingsphere.opentracing.listener.execution.SQLExecuteEventListener;
import io.shardingsphere.opentracing.listener.execution.OverallExecuteEventListener;
import io.shardingsphere.opentracing.listener.merger.MergeEventListener;
import io.shardingsphere.opentracing.listener.routing.RouteEventListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Sharding tracer object container.
 *
 * @author gaohongtao
 * @author wangkai
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShardingTracer {
    
    private static final String OPENTRACING_TRACER_CLASS_NAME = "shardingsphere.opentracing.tracer.class";
    
    /**
     * Initialize sharding tracer.
     */
    public static void init() {
        String tracerClassName = System.getProperty(OPENTRACING_TRACER_CLASS_NAME);
        Preconditions.checkNotNull(tracerClassName, "Can not find opentracing tracer implementation class.");
        try {
            init((Tracer) Class.forName(tracerClassName).newInstance());
        } catch (final ReflectiveOperationException ex) {
            throw new ShardingException("Initialize opentracing tracer class failure.", ex);
        }
    }
    
    /**
     * Initialize sharding tracer.
     *
     * @param tracer opentracing tracer
     */
    public static void init(final Tracer tracer) {
        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(tracer);
            registerListeners();
        }
    }
    
    private static void registerListeners() {
        new RouteEventListener().register();
        new OverallExecuteEventListener().register();
        new SQLExecuteEventListener().register();
        new MergeEventListener().register();
    }
    
    /**
     * Get tracer.
     *
     * @return tracer
     */
    public static Tracer get() {
        return GlobalTracer.get();
    }
}
