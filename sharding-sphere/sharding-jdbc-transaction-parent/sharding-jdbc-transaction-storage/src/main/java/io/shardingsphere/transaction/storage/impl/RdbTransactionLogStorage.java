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

package io.shardingsphere.transaction.storage.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.shardingsphere.transaction.constants.SoftTransactionType;
import io.shardingsphere.transaction.exception.TransactionCompensationException;
import io.shardingsphere.transaction.exception.TransactionLogStorageException;
import io.shardingsphere.transaction.storage.TransactionLog;
import io.shardingsphere.transaction.storage.TransactionLogStorage;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction relationship database log storage.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class RdbTransactionLogStorage implements TransactionLogStorage {
    
    private final DataSource dataSource;
    
    @Override
    public void add(final TransactionLog transactionLog) {
        String sql = "INSERT INTO `transaction_log` (`id`, `transaction_type`, `data_source`, `sql`, `parameters`, `creation_time`) VALUES (?, ?, ?, ?, ?, ?);";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, transactionLog.getId());
            preparedStatement.setString(2, SoftTransactionType.BestEffortsDelivery.name());
            preparedStatement.setString(3, transactionLog.getDataSource());
            preparedStatement.setString(4, transactionLog.getSql());
            preparedStatement.setString(5, new Gson().toJson(transactionLog.getParameters()));
            preparedStatement.setLong(6, transactionLog.getCreationTime());
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
    }
    
    @Override
    public void remove(final String id) {
        String sql = "DELETE FROM `transaction_log` WHERE `id`=?;";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
    }
    
    @Override
    public List<TransactionLog> findEligibleTransactionLogs(final int size, final int maxDeliveryTryTimes, final long maxDeliveryTryDelayMillis) {
        List<TransactionLog> result = new ArrayList<>(size);
        String sql = "SELECT `id`, `transaction_type`, `data_source`, `sql`, `parameters`, `creation_time`, `async_delivery_try_times` "
            + "FROM `transaction_log` WHERE `async_delivery_try_times`<? AND `transaction_type`=? AND `creation_time`<? LIMIT ?;";
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, maxDeliveryTryTimes);
                preparedStatement.setString(2, SoftTransactionType.BestEffortsDelivery.name());
                preparedStatement.setLong(3, System.currentTimeMillis() - maxDeliveryTryDelayMillis);
                preparedStatement.setInt(4, size);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        Gson gson = new Gson();
                        // TODO for batch parameters should split 2-level records
                        List<Object> parameters = gson.fromJson(rs.getString(5), new TypeToken<List<Object>>() { }.getType());
                        result.add(new TransactionLog(rs.getString(1), "", SoftTransactionType.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), parameters, rs.getLong(6), rs.getInt(7)));
                    }
                }
            }
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
        return result;
    }
    
    @Override
    public void increaseAsyncDeliveryTryTimes(final String id) {
        String sql = "UPDATE `transaction_log` SET `async_delivery_try_times`=`async_delivery_try_times`+1 WHERE `id`=?;";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
    }
    
    @Override
    public boolean processData(final Connection connection, final TransactionLog transactionLog, final int maxDeliveryTryTimes) {
        try (
            Connection conn = connection;
            PreparedStatement preparedStatement = conn.prepareStatement(transactionLog.getSql())) {
            for (int parameterIndex = 0; parameterIndex < transactionLog.getParameters().size(); parameterIndex++) {
                preparedStatement.setObject(parameterIndex + 1, transactionLog.getParameters().get(parameterIndex));
            }
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            increaseAsyncDeliveryTryTimes(transactionLog.getId());
            throw new TransactionCompensationException(ex);
        }
        remove(transactionLog.getId());
        return true;
    }
}
