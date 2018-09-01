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

package io.shardingsphere.core.jdbc.core;

import io.shardingsphere.core.constant.ConnectionMode;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.exception.ShardingException;
import io.shardingsphere.core.executor.ShardingExecuteEngine;
import io.shardingsphere.core.jdbc.metadata.JDBCTableMetaDataConnectionManager;
import io.shardingsphere.core.metadata.ShardingMetaData;
import io.shardingsphere.core.rule.ShardingRule;
import lombok.Getter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sharding runtime context.
 * 
 * @author gaohongtao
 * @author panjuan
 */
@Getter
public final class ShardingContext implements AutoCloseable {
    
    private ShardingRule shardingRule;
    
    private DatabaseType databaseType;
    
    private ShardingExecuteEngine executeEngine;
    
    private ShardingMetaData metaData;
    
    private ConnectionMode connectionMode;
    
    private int maxConnectionsSizePerQuery;
    
    private boolean showSQL;
    
    public ShardingContext(final Map<String, DataSource> dataSourceMap, final ShardingRule shardingRule, final DatabaseType databaseType, 
                           final ShardingExecuteEngine executeEngine, final ConnectionMode connectionMode, final int maxConnectionsSizePerQuery, final boolean showSQL) {
        init(dataSourceMap, shardingRule, databaseType, executeEngine, connectionMode, maxConnectionsSizePerQuery, showSQL);
    }
    
    private void init(final Map<String, DataSource> dataSourceMap, final ShardingRule shardingRule, final DatabaseType databaseType, 
                      final ShardingExecuteEngine executeEngine, final ConnectionMode connectionMode, final int maxConnectionsSizePerQuery, final boolean showSQL) {
        this.shardingRule = shardingRule;
        this.executeEngine = executeEngine;
        this.databaseType = databaseType;
        this.connectionMode = connectionMode;
        this.maxConnectionsSizePerQuery = maxConnectionsSizePerQuery;
        this.showSQL = showSQL;
        metaData = new ShardingMetaData(getDataSourceURLs(dataSourceMap), shardingRule, databaseType, executeEngine, new JDBCTableMetaDataConnectionManager(dataSourceMap), maxConnectionsSizePerQuery);
    }
    
    private static Map<String, String> getDataSourceURLs(final Map<String, DataSource> dataSourceMap) {
        Map<String, String> result = new LinkedHashMap<>(dataSourceMap.size(), 1);
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            result.put(entry.getKey(), getDataSourceURL(entry.getValue()));
        }
        return result;
    }
    
    private static String getDataSourceURL(final DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        } catch (final SQLException ex) {
            throw new ShardingException(ex);
        }
    }
    
    @Override
    public void close() {
        executeEngine.close();
    }
}
