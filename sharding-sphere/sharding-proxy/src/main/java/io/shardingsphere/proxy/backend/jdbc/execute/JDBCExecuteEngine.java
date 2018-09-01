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

package io.shardingsphere.proxy.backend.jdbc.execute;

import io.shardingsphere.core.merger.QueryResult;
import io.shardingsphere.proxy.backend.SQLExecuteEngine;
import io.shardingsphere.proxy.backend.jdbc.connection.BackendConnection;
import io.shardingsphere.proxy.backend.jdbc.execute.response.unit.ExecuteQueryResponseUnit;
import io.shardingsphere.proxy.backend.jdbc.execute.response.unit.ExecuteResponseUnit;
import io.shardingsphere.proxy.backend.jdbc.execute.response.unit.ExecuteUpdateResponseUnit;
import io.shardingsphere.proxy.backend.jdbc.wrapper.JDBCExecutorWrapper;
import io.shardingsphere.proxy.transport.mysql.constant.ColumnType;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.ColumnDefinition41Packet;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.FieldCountPacket;
import io.shardingsphere.proxy.transport.mysql.packet.command.query.QueryResponsePackets;
import io.shardingsphere.proxy.transport.mysql.packet.generic.EofPacket;
import io.shardingsphere.proxy.transport.mysql.packet.generic.OKPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * SQL Execute engine for JDBC.
 *
 * @author zhaojun
 * @author zhangliang
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class JDBCExecuteEngine implements SQLExecuteEngine {
    
    private final List<QueryResult> queryResults = new LinkedList<>();
    
    private final BackendConnection backendConnection;
    
    private final JDBCExecutorWrapper jdbcExecutorWrapper;
    
    private int columnCount;
    
    private List<ColumnType> columnTypes;
    
    protected final ExecuteResponseUnit executeWithMetadata(final Statement statement, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        backendConnection.add(statement);
        if (!jdbcExecutorWrapper.executeSQL(statement, sql, isReturnGeneratedKeys)) {
            return new ExecuteUpdateResponseUnit(new OKPacket(1, statement.getUpdateCount(), isReturnGeneratedKeys ? getGeneratedKey(statement) : 0));
        }
        ResultSet resultSet = statement.getResultSet();
        backendConnection.add(resultSet);
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        if (0 == resultSetMetaData.getColumnCount()) {
            return new ExecuteUpdateResponseUnit(new OKPacket(1));
        }
        return new ExecuteQueryResponseUnit(getHeaderPackets(resultSetMetaData), createQueryResult(resultSet));
    }
    
    protected final ExecuteResponseUnit executeWithoutMetadata(final Statement statement, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        backendConnection.add(statement);
        if (!jdbcExecutorWrapper.executeSQL(statement, sql, isReturnGeneratedKeys)) {
            return new ExecuteUpdateResponseUnit(new OKPacket(1, statement.getUpdateCount(), isReturnGeneratedKeys ? getGeneratedKey(statement) : 0));
        }
        ResultSet resultSet = statement.getResultSet();
        backendConnection.add(resultSet);
        return new ExecuteQueryResponseUnit(null, createQueryResult(resultSet));
    }
    
    private long getGeneratedKey(final Statement statement) throws SQLException {
        ResultSet resultSet = statement.getGeneratedKeys();
        return resultSet.next() ? resultSet.getLong(1) : 0L;
    }
    
    private QueryResponsePackets getHeaderPackets(final ResultSetMetaData resultSetMetaData) throws SQLException {
        int currentSequenceId = 0;
        int columnCount = resultSetMetaData.getColumnCount();
        FieldCountPacket fieldCountPacket = new FieldCountPacket(++currentSequenceId, columnCount);
        Collection<ColumnDefinition41Packet> columnDefinition41Packets = new LinkedList<>();
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            columnDefinition41Packets.add(new ColumnDefinition41Packet(++currentSequenceId, resultSetMetaData, columnIndex));
        }
        return new QueryResponsePackets(fieldCountPacket, columnDefinition41Packets, new EofPacket(++currentSequenceId));
    }
    
    protected abstract QueryResult createQueryResult(ResultSet resultSet) throws SQLException;
}
