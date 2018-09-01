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

package io.shardingsphere.transaction.api.config;

import com.google.common.base.Optional;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSource;
import io.shardingsphere.transaction.constants.TransactionLogDataSourceType;
import io.shardingsphere.transaction.datasource.TransactionLogDataSource;
import io.shardingsphere.transaction.datasource.impl.MemoryTransactionLogDataSource;
import io.shardingsphere.transaction.datasource.impl.RdbTransactionLogDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * B.A.S.E transaction configuration.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
@Getter
@Setter
public class SoftTransactionConfiguration {
    
    /**
     * Data source for transaction manager.
     */
    @Getter(AccessLevel.NONE)
    private final DataSource targetDataSource;
    
    /**
     * Max synchronized delivery try times.
     */
    private int syncMaxDeliveryTryTimes = 3;
    
    /**
     * Transaction log storage type.
     */
    private TransactionLogDataSourceType storageType = TransactionLogDataSourceType.RDB;
    
    /**
     * Transaction log data source.
     */
    private DataSource transactionLogDataSource;
    
    /**
     * Embed best efforts delivery B.A.S.E transaction asynchronized job configuration.
     */
    private Optional<NestedBestEffortsDeliveryJobConfiguration> bestEffortsDeliveryJobConfiguration = Optional.absent();
    
    /**
     * Get connection for transaction manager.
     * 
     * @param dataSourceName data source name for transaction manager
     * @return connection for transaction manager
     * @throws SQLException SQL exception
     */
    public Connection getTargetConnection(final String dataSourceName) throws SQLException {
        if (!(targetDataSource instanceof ShardingDataSource)) {
            return targetDataSource.getConnection();
        }
        return ((ShardingDataSource) targetDataSource).getConnection().getConnection(dataSourceName);
    }

    /**
     * Build transaction log data source.
     *
     * @return transaction log data source
     */
    public TransactionLogDataSource buildTransactionLogDataSource() {
        TransactionLogDataSource result;
        switch (storageType) {
            case MEMORY:
                result = new MemoryTransactionLogDataSource();
                break;
            case RDB:
                result = new RdbTransactionLogDataSource(transactionLogDataSource);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return result;
    }
}
