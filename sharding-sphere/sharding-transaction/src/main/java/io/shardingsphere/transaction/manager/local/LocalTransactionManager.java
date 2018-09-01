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

package io.shardingsphere.transaction.manager.local;

import io.shardingsphere.core.event.transaction.local.LocalTransactionEvent;
import io.shardingsphere.transaction.manager.ShardingTransactionManager;

import javax.transaction.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Local transaction manager.
 *
 * @author zhaojun
 */
public final class LocalTransactionManager implements ShardingTransactionManager<LocalTransactionEvent> {
    
    @Override
    public void begin(final LocalTransactionEvent event) throws SQLException {
        Collection<SQLException> exceptions = new LinkedList<>();
        for (Connection each : event.getCachedConnections()) {
            try {
                each.setAutoCommit(event.isAutoCommit());
            } catch (final SQLException ex) {
                exceptions.add(ex);
            }
        }
        throwSQLExceptionIfNecessary(exceptions);
    }
    
    @Override
    public void commit(final LocalTransactionEvent event) throws SQLException {
        Collection<SQLException> exceptions = new LinkedList<>();
        for (Connection each : event.getCachedConnections()) {
            try {
                each.commit();
            } catch (final SQLException ex) {
                exceptions.add(ex);
            }
        }
        throwSQLExceptionIfNecessary(exceptions);
    }
    
    @Override
    public void rollback(final LocalTransactionEvent event) throws SQLException {
        Collection<SQLException> exceptions = new LinkedList<>();
        for (Connection each : event.getCachedConnections()) {
            try {
                each.rollback();
            } catch (final SQLException ex) {
                exceptions.add(ex);
            }
        }
        throwSQLExceptionIfNecessary(exceptions);
    }
    
    private void throwSQLExceptionIfNecessary(final Collection<SQLException> exceptions) throws SQLException {
        if (exceptions.isEmpty()) {
            return;
        }
        SQLException sqlException = new SQLException();
        for (SQLException each : exceptions) {
            sqlException.setNextException(each);
        }
        throw sqlException;
    }
    
    @Override
    public int getStatus() {
        // TODO :zhaojun need confirm, return Status.STATUS_NO_TRANSACTION or zero? 
        return Status.STATUS_NO_TRANSACTION;
    }
}
