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

package io.shardingsphere.core.constant.properties;

import io.shardingsphere.core.constant.ConnectionMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sharding properties constant.
 * 
 * @author gaohongtao
 * @author caohao
 */
@RequiredArgsConstructor
@Getter
public enum ShardingPropertiesConstant {
    
    /**
     * Enable or Disable to show SQL details.
     * 
     * <p>
     * Print SQL details can help developers debug easier. 
     * The details includes: logic SQL, parse context and rewrote actual SQL list. 
     * Enable this property will log into log topic: {@code Sharding-Sphere-SQL}, log level is {@code INFO}.
     * Default: false
     * </p>
     */
    SQL_SHOW("sql.show", Boolean.FALSE.toString(), boolean.class),
    
    /**
     * Worker group or user group thread max size.
     *
     * <p>
     * Worker group accept tcp connection.
     * User group accept MySQL command.
     * Default: CPU cores * 2.
     * </p>
     */
    ACCEPTOR_SIZE("acceptor.size", String.valueOf(Runtime.getRuntime().availableProcessors() * 2), int.class),
    
    /**
     * Worker thread max size.
     * 
     * <p>
     * Execute SQL Statement and PrepareStatement will use this thread pool.
     * One sharding data source will use a independent thread pool, it does not share thread pool even different data source in same JVM.
     * Default: infinite.
     * </p>
     */
    EXECUTOR_SIZE("executor.size", String.valueOf(0), int.class),
    
    /**
     * Connection mode of connected to databases.
     *
     * <p>
     * MEMORY_STRICTLY:
     * Sharding-Sphere holds as many connections as the count of actual tables routed in a database.
     * The benefit of this approach is saving memory for Proxy by Stream ResultSet.
     * </p>
     * 
     * <p>
     * CONNECTION_STRICTLY:
     * harding-Sphere will release connections after get the overall rows from the ResultSet.
     * Meanwhile, the cost of the memory will be increased.
     * </p>
     */
    CONNECTION_MODE("connection.mode", ConnectionMode.MEMORY_STRICTLY.name(), String.class),
    
    MAX_CONNECTIONS_SIZE_PER_QUERY("max.connections.size.per.query", String.valueOf(Runtime.getRuntime().availableProcessors()), int.class),
    
    PROXY_TRANSACTION_ENABLED("proxy.transaction.enabled", Boolean.FALSE.toString(), boolean.class),
    
    PROXY_BACKEND_USE_NIO("proxy.backend.use.nio", Boolean.FALSE.toString(), boolean.class),
    
    PROXY_BACKEND_MAX_CONNECTIONS("proxy.backend.max.connections", 8 + "", int.class),
    
    PROXY_BACKEND_CONNECTION_TIMEOUT_SECONDS("proxy.backend.connection.timeout.seconds", 60 + "", int.class);
    
    private final String key;
    
    private final String defaultValue;
    
    private final Class<?> type;
    
    /**
     * Find value via property key.
     * 
     * @param key property key
     * @return value enum, return {@code null} if not found
     */
    public static ShardingPropertiesConstant findByKey(final String key) {
        for (ShardingPropertiesConstant each : ShardingPropertiesConstant.values()) {
            if (each.getKey().equals(key)) {
                return each;
            }
        }
        return null;
    }
}
