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

package io.shardingsphere.proxy.backend.jdbc;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.constant.transaction.TransactionType;
import io.shardingsphere.core.merger.MergeEngineFactory;
import io.shardingsphere.core.merger.MergedResult;
import io.shardingsphere.core.metadata.table.executor.TableMetaDataLoader;
import io.shardingsphere.core.parsing.parser.constant.DerivedColumn;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.routing.SQLRouteResult;
import io.shardingsphere.proxy.backend.AbstractBackendHandler;
import io.shardingsphere.proxy.backend.BackendExecutorContext;
import io.shardingsphere.proxy.backend.ResultPacket;
import io.shardingsphere.proxy.backend.jdbc.execute.JDBCExecuteEngine;
import io.shardingsphere.proxy.backend.jdbc.execute.response.ExecuteQueryResponse;
import io.shardingsphere.proxy.backend.jdbc.execute.response.ExecuteResponse;
import io.shardingsphere.proxy.backend.jdbc.execute.response.ExecuteUpdateResponse;
import io.shardingsphere.proxy.config.ProxyTableMetaDataConnectionManager;
import io.shardingsphere.proxy.config.RuleRegistry;
import io.shardingsphere.proxy.transport.mysql.constant.ServerErrorCode;
import io.shardingsphere.proxy.transport.mysql.packet.command.CommandResponsePackets;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.ColumnDefinition41Packet;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.FieldCountPacket;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.QueryResponsePackets;
import io.shardingsphere.proxy.transport.mysql.packet.generic.EofPacket;
import io.shardingsphere.proxy.transport.mysql.packet.generic.ErrPacket;
import io.shardingsphere.proxy.transport.mysql.packet.generic.OKPacket;
import io.shardingsphere.transaction.manager.ShardingTransactionManagerRegistry;
import lombok.RequiredArgsConstructor;

import javax.transaction.Status;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Backend handler via JDBC to connect databases.
 *
 * @author zhaojun
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class JDBCBackendHandler extends AbstractBackendHandler {
    
    private static final RuleRegistry RULE_REGISTRY = RuleRegistry.getInstance();
    
    private final String sql;
    
    private final JDBCExecuteEngine executeEngine;
    
    private ExecuteResponse executeResponse;
    
    private MergedResult mergedResult;
    
    private int currentSequenceId;
    
    @Override
    protected CommandResponsePackets execute0() throws SQLException {
        return execute(executeEngine.getJdbcExecutorWrapper().route(sql, DatabaseType.MySQL));
    }
    
    private CommandResponsePackets execute(final SQLRouteResult routeResult) throws SQLException {
        if (routeResult.getExecutionUnits().isEmpty()) {
            return new CommandResponsePackets(new OKPacket(1));
        }
        SQLStatement sqlStatement = routeResult.getSqlStatement();
        if (isUnsupportedXA(sqlStatement.getType())) {
            return new CommandResponsePackets(new ErrPacket(1, 
                    ServerErrorCode.ER_ERROR_ON_MODIFYING_GTID_EXECUTED_TABLE, sqlStatement.getTables().isSingleTable() ? sqlStatement.getTables().getSingleTableName() : "unknown_table"));
        }
        executeResponse = executeEngine.execute(routeResult);
        if (!RULE_REGISTRY.isMasterSlaveOnly() && SQLType.DDL == sqlStatement.getType() && !sqlStatement.getTables().isEmpty()) {
            String logicTableName = sqlStatement.getTables().getSingleTableName();
            // TODO refresh table meta data by SQL parse result
            TableMetaDataLoader tableMetaDataLoader = new TableMetaDataLoader(RULE_REGISTRY.getMetaData().getDataSource(), BackendExecutorContext.getInstance().getExecuteEngine(), 
                    new ProxyTableMetaDataConnectionManager(RULE_REGISTRY.getBackendDataSource()), RULE_REGISTRY.getMaxConnectionsSizePerQuery());
            RULE_REGISTRY.getMetaData().getTable().put(logicTableName, tableMetaDataLoader.load(logicTableName, RULE_REGISTRY.getShardingRule()));
        }
        return merge(sqlStatement);
    }
    
    private boolean isUnsupportedXA(final SQLType sqlType) throws SQLException {
        return TransactionType.XA == RULE_REGISTRY.getTransactionType() && SQLType.DDL == sqlType
                && Status.STATUS_NO_TRANSACTION != ShardingTransactionManagerRegistry.getInstance().getShardingTransactionManager(TransactionType.XA).getStatus();
    }
    
    private CommandResponsePackets merge(final SQLStatement sqlStatement) throws SQLException {
        if (executeResponse instanceof ExecuteUpdateResponse) {
            return ((ExecuteUpdateResponse) executeResponse).merge();
        }
        mergedResult = MergeEngineFactory.newInstance(
                RULE_REGISTRY.getShardingRule(), ((ExecuteQueryResponse) executeResponse).getQueryResults(), sqlStatement, RULE_REGISTRY.getMetaData().getTable()).merge();
        QueryResponsePackets result = getQueryResponsePacketsWithoutDerivedColumns(((ExecuteQueryResponse) executeResponse).getQueryResponsePackets());
        currentSequenceId = result.getPackets().size();
        return result;
    }
    
    private QueryResponsePackets getQueryResponsePacketsWithoutDerivedColumns(final QueryResponsePackets queryResponsePackets) {
        Collection<ColumnDefinition41Packet> columnDefinition41Packets = new ArrayList<>(queryResponsePackets.getColumnCount());
        int columnCount = 0;
        for (ColumnDefinition41Packet each : queryResponsePackets.getColumnDefinition41Packets()) {
            if (!DerivedColumn.isDerivedColumn(each.getName())) {
                columnDefinition41Packets.add(each);
                columnCount++;
            }
        }
        FieldCountPacket fieldCountPacket = new FieldCountPacket(1, columnCount);
        return new QueryResponsePackets(fieldCountPacket, columnDefinition41Packets, new EofPacket(columnCount + 2));
    }
    
    @Override
    public boolean next() throws SQLException {
        return null != mergedResult && mergedResult.next();
    }
    
    @Override
    public ResultPacket getResultValue() throws SQLException {
        QueryResponsePackets queryResponsePackets = ((ExecuteQueryResponse) executeResponse).getQueryResponsePackets();
        int columnCount = queryResponsePackets.getColumnCount();
        List<Object> data = new ArrayList<>(columnCount);
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            data.add(mergedResult.getValue(columnIndex, Object.class));
        }
        return new ResultPacket(++currentSequenceId, data, columnCount, queryResponsePackets.getColumnTypes());
    }
}
