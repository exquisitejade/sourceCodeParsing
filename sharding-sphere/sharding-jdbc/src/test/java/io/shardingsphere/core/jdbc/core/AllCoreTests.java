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

import io.shardingsphere.core.jdbc.core.connection.ShardingConnectionTest;
import io.shardingsphere.core.jdbc.core.datasource.MasterSlaveDataSourceTest;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSourceTest;
import io.shardingsphere.core.jdbc.core.resultset.GeneratedKeysResultSetMetaDataTest;
import io.shardingsphere.core.jdbc.core.resultset.GeneratedKeysResultSetTest;
import io.shardingsphere.core.jdbc.core.resultset.ResultSetUtilTest;
import io.shardingsphere.core.jdbc.core.resultset.ShardingResultSetTest;
import io.shardingsphere.core.jdbc.core.statement.ShardingPreparedStatementTest;
import io.shardingsphere.core.jdbc.core.statement.ShardingStatementTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ShardingDataSourceTest.class, 
        MasterSlaveDataSourceTest.class, 
        ShardingConnectionTest.class, 
        ShardingStatementTest.class, 
        ShardingPreparedStatementTest.class, 
        ShardingResultSetTest.class,
        ResultSetUtilTest.class, 
        GeneratedKeysResultSetTest.class, 
        GeneratedKeysResultSetMetaDataTest.class 
    })
public final class AllCoreTests {
}
