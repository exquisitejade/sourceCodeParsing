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

package io.shardingsphere.proxy.backend.jdbc.wrapper;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.parsing.SQLJudgeEngine;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.routing.SQLExecutionUnit;
import io.shardingsphere.core.routing.SQLRouteResult;
import io.shardingsphere.core.routing.SQLUnit;
import io.shardingsphere.core.routing.StatementRoutingEngine;
import io.shardingsphere.core.routing.router.masterslave.MasterSlaveRouter;
import io.shardingsphere.proxy.config.RuleRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * Executor wrapper for statement.
 *
 * @author zhangliang
 */
public final class StatementExecutorWrapper implements JDBCExecutorWrapper {
    
    private static final RuleRegistry RULE_REGISTRY = RuleRegistry.getInstance();
    
    @Override
    public SQLRouteResult route(final String sql, final DatabaseType databaseType) {
        return RULE_REGISTRY.isMasterSlaveOnly() ? doMasterSlaveRoute(sql) : doShardingRoute(sql, databaseType);
    }
    
    private SQLRouteResult doMasterSlaveRoute(final String sql) {
        SQLStatement sqlStatement = new SQLJudgeEngine(sql).judge();
        SQLRouteResult result = new SQLRouteResult(sqlStatement);
        for (String each : new MasterSlaveRouter(RULE_REGISTRY.getMasterSlaveRule(), RULE_REGISTRY.isShowSQL()).route(sql)) {
            result.getExecutionUnits().add(new SQLExecutionUnit(each, new SQLUnit(sql, Collections.<List<Object>>emptyList())));
        }
        return result;
    }
    
    private SQLRouteResult doShardingRoute(final String sql, final DatabaseType databaseType) {
        StatementRoutingEngine routingEngine = new StatementRoutingEngine(
                RULE_REGISTRY.getShardingRule(), RULE_REGISTRY.getMetaData().getTable(), databaseType, RULE_REGISTRY.isShowSQL(), RULE_REGISTRY.getMetaData().getDataSource());
        return routingEngine.route(sql);
    }
    
    @Override
    public Statement createStatement(final Connection connection, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        return connection.createStatement();
    }
    
    @Override
    public boolean executeSQL(final Statement statement, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        return statement.execute(sql, isReturnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
    }
}
