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

package io.shardingsphere.proxy.backend.jdbc.datasource;

import io.shardingsphere.core.constant.transaction.TransactionType;
import io.shardingsphere.core.exception.ShardingException;
import io.shardingsphere.core.rule.DataSourceParameter;
import io.shardingsphere.proxy.backend.BackendDataSource;
import io.shardingsphere.proxy.config.RuleRegistry;
import lombok.Getter;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Backend data source for JDBC.
 *
 * @author zhaojun
 * @author zhangliang
 * @author panjuan
 */
@Getter
public final class JDBCBackendDataSource implements BackendDataSource, AutoCloseable {
    
    private final Map<String, DataSource> dataSourceMap;
    
    public JDBCBackendDataSource() {
        dataSourceMap = createDataSourceMap();
    }
    
    private Map<String, DataSource> createDataSourceMap() {
        TransactionType transactionType = RuleRegistry.getInstance().getTransactionType();
        Map<String, DataSourceParameter> dataSourceParameters = RuleRegistry.getInstance().getDataSourceConfigurationMap();
        // TODO getCircuitDataSourceMap if RuleRegistry.getInstance().getCircuitBreakerDataSourceNames().isEmpty() is false
        return getNormalDataSourceMap(transactionType, dataSourceParameters);
    }
    
    private Map<String, DataSource> getNormalDataSourceMap(final TransactionType transactionType, final Map<String, DataSourceParameter> dataSourceParameters) {
        Map<String, DataSource> result = new LinkedHashMap<>(dataSourceParameters.size());
        for (Entry<String, DataSourceParameter> entry : dataSourceParameters.entrySet()) {
            try {
                result.put(entry.getKey(), getBackendDataSourceFactory(transactionType).build(entry.getKey(), entry.getValue()));
            // CHECKSTYLE:OFF
            } catch (final Exception ex) {
                // CHECKSTYLE:ON
                throw new ShardingException(String.format("Can not build data source, name is `%s`.", entry.getKey()), ex);
            }
        }
        return result;
    }
    
    private JDBCBackendDataSourceFactory getBackendDataSourceFactory(final TransactionType transactionType) {
        switch (transactionType) {
            case XA:
                return new JDBCXABackendDataSourceFactory();
            default:
                return new JDBCRawBackendDataSourceFactory();
        }
    }
    
    /**
     * Get connection.
     *
     * @param dataSourceName data source name
     * @return connection
     * @throws SQLException SQL exception
     */
    public Connection getConnection(final String dataSourceName) throws SQLException {
        return getDataSourceMap().get(dataSourceName).getConnection();
    }
    
    private Map<String, DataSource> getDataSourceMap() {
        if (!RuleRegistry.getInstance().getDisabledDataSourceNames().isEmpty()) {
            return getAvailableDataSourceMap();
        }
        return dataSourceMap;
    }
    
    private Map<String, DataSource> getAvailableDataSourceMap() {
        Map<String, DataSource> result = new LinkedHashMap<>(dataSourceMap);
        for (String each : RuleRegistry.getInstance().getDisabledDataSourceNames()) {
            result.remove(each);
        }
        return result;
    }
    
    @Override
    public void close() {
        closeOriginalDataSources();
    }
    
    private void closeOriginalDataSources() {
        for (DataSource each : dataSourceMap.values()) {
            try {
                Method method = each.getClass().getDeclaredMethod("close");
                method.invoke(each);
            } catch (final NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
        }
    }
}
