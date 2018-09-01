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

package io.shardingsphere.core.routing;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.metadata.table.ShardingTableMetaData;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import io.shardingsphere.core.routing.router.masterslave.ShardingMasterSlaveRouter;
import io.shardingsphere.core.routing.router.sharding.ShardingRouter;
import io.shardingsphere.core.routing.router.sharding.ShardingRouterFactory;
import io.shardingsphere.core.rule.ShardingRule;

import java.util.Collections;

/**
 * Statement routing engine.
 * 
 * @author zhangiang
 * @author panjuan
 */
public final class StatementRoutingEngine {
    
    private final ShardingRouter shardingRouter;
    
    private final ShardingMasterSlaveRouter masterSlaveRouter;
    
    public StatementRoutingEngine(final ShardingRule shardingRule, final ShardingTableMetaData shardingTableMetaData,
                                  final DatabaseType databaseType, final boolean showSQL, final ShardingDataSourceMetaData shardingDataSourceMetaData) {
        shardingRouter = ShardingRouterFactory.createSQLRouter(shardingRule, shardingTableMetaData, databaseType, showSQL, shardingDataSourceMetaData);
        masterSlaveRouter = new ShardingMasterSlaveRouter(shardingRule.getMasterSlaveRules());
    }
    
    /**
     * SQL route.
     *
     * @param logicSQL logic SQL
     * @return route result
     */
    public SQLRouteResult route(final String logicSQL) {
        SQLStatement sqlStatement = shardingRouter.parse(logicSQL, false);
        return masterSlaveRouter.route(shardingRouter.route(logicSQL, Collections.emptyList(), sqlStatement));
    }
}
