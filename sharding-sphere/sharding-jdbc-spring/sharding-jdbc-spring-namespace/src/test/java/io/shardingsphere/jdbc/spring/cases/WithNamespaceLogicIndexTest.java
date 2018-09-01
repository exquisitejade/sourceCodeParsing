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

package io.shardingsphere.jdbc.spring.cases;

import io.shardingsphere.jdbc.spring.AbstractShardingBothDataBasesAndTablesJUnitTest;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@ContextConfiguration(locations = "classpath:META-INF/rdb/withNamespaceLogicIndex.xml")
public final class WithNamespaceLogicIndexTest extends AbstractShardingBothDataBasesAndTablesJUnitTest {
    
    @Test
    public void assertIndex() throws SQLException {
        try (Connection connection = getShardingDataSource().getConnection()) {
            String sql = "CREATE INDEX t_order_index ON t_order(user_id)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
            sql = "DROP INDEX t_order_index";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
            preparedStatement.close();
        }
    }
    
}
