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

package io.shardingsphere.core.executor.sql.execute.threadlocal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executor runtime data map.
 *
 * @author caohao
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutorDataMap {
    
    private static ThreadLocal<Map<String, Object>> dataMap = new ThreadLocal<Map<String, Object>>() {
        
        @Override
        protected Map<String, Object> initialValue() {
            return new LinkedHashMap<>();
        }
    };
    
    /**
     * Set data map.
     *
     * @param dataMap data map
     */
    public static void setDataMap(final Map<String, Object> dataMap) {
        ExecutorDataMap.dataMap.set(dataMap);
    }
    
    /**
     * Get data map.
     *
     * @return data map
     */
    public static Map<String, Object> getDataMap() {
        return dataMap.get();
    }
}
