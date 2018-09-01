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

package io.shardingsphere.jdbc.orchestration.spring.namespace.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Etcd registry center parser tag constants.
 *
 * @author zhangliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EtcdRegistryCenterBeanDefinitionParserTag {
    
    public static final String ROOT_TAG = "etcd";
    
    public static final String SERVER_LISTS_TAG = "server-lists";
    
    public static final String TIME_TO_LIVE_SECONDS_TAG = "time-to-live-seconds";
    
    public static final String TIMEOUT_MILLISECONDS_TAG = "timeout-milliseconds";
    
    public static final String RETRY_INTERVAL_MILLISECONDS_TAG = "retry-interval-milliseconds";
    
    public static final String MAX_RETRIES_TAG = "max-retries";
}
