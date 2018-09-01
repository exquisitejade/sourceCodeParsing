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

package io.shardingsphere.jdbc.orchestration.spring.datasource;

import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSource;
import io.shardingsphere.core.rule.ShardingRule;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Sharding datasource for spring namespace.
 *
 * @author maxiaoguang
 */
public class SpringShardingDataSource extends ShardingDataSource {
    
    public SpringShardingDataSource(final Map<String, DataSource> dataSourceMap, final ShardingRuleConfiguration shardingRuleConfig,
                                    final Map<String, Object> configMap, final Properties props) throws SQLException {
        super(dataSourceMap, new ShardingRule(shardingRuleConfig, dataSourceMap.keySet()), configMap, props);
    }
}
