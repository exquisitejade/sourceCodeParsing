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

package io.shardingsphere.opentracing;

import com.google.common.collect.HashMultimap;
import com.google.common.eventbus.EventBus;
import io.opentracing.NoopTracerFactory;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.api.config.TableRuleConfiguration;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.core.jdbc.core.ShardingContext;
import io.shardingsphere.core.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSource;
import io.shardingsphere.core.jdbc.core.statement.ShardingPreparedStatement;
import io.shardingsphere.core.jdbc.core.statement.ShardingStatement;
import io.shardingsphere.core.merger.MergeEngine;
import io.shardingsphere.core.merger.dal.DALMergeEngine;
import io.shardingsphere.core.metadata.ShardingMetaData;
import io.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import io.shardingsphere.core.metadata.table.ShardingTableMetaData;
import io.shardingsphere.core.metadata.table.TableMetaData;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowColumnsStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowDatabasesStatement;
import io.shardingsphere.core.rule.ShardingRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MergeEventListenerTest {
    
    private static final MockTracer TRACER = new MockTracer(new ThreadLocalActiveSpanSource(), MockTracer.Propagator.TEXT_MAP);
    
    private ShardingContext shardingContext;
    
    private MergeEngine mergeEngine;
    
    private ShardingDataSource shardingDataSource;
    
    @BeforeClass
    public static void init() {
        ShardingTracer.init(TRACER);
    }
    
    @AfterClass
    public static void tearDown() throws NoSuchFieldException, IllegalAccessException {
        releaseTracer();
    }
    
    @Before
    public void setUp() throws SQLException {
        TRACER.reset();
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration();
        tableRuleConfig.setLogicTable("t_order");
        shardingRuleConfig.getTableRuleConfigs().add(tableRuleConfig);
        Map<String, DataSource> dataSourceMap = new HashMap<>(2, 1);
        dataSourceMap.put("ds_0", mockDataSource());
        dataSourceMap.put("ds_1", mockDataSource());
        ShardingRule shardingRule = new ShardingRule(shardingRuleConfig, dataSourceMap.keySet());
        shardingContext = Mockito.mock(ShardingContext.class);
        when(shardingContext.getShardingRule()).thenReturn(shardingRule);
        ShardingMetaData shardingMetaData = Mockito.mock(ShardingMetaData.class);
        Map<String, String> dataSourceUrls = new LinkedHashMap<>();
        dataSourceUrls.put("ds_0", "jdbc:mysql://127.0.0.1:3306/ds");
        when(shardingMetaData.getDataSource()).thenReturn(new ShardingDataSourceMetaData(dataSourceUrls, shardingRule, DatabaseType.MySQL));
        when(shardingMetaData.getTable()).thenReturn(new ShardingTableMetaData(new LinkedHashMap<String, TableMetaData>()));
        when(shardingContext.getMetaData()).thenReturn(shardingMetaData);
        when(shardingContext.getDatabaseType()).thenReturn(DatabaseType.MySQL);
        when(shardingContext.isShowSQL()).thenReturn(true);
        mergeEngine = new DALMergeEngine(null, null, new ShowDatabasesStatement(), null);
        shardingDataSource = Mockito.mock(ShardingDataSource.class);
        when(shardingDataSource.getShardingContext()).thenReturn(shardingContext);
    }
    
    private DataSource mockDataSource() throws SQLException {
        DataSource result = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(databaseMetaData.getURL()).thenReturn("jdbc:mysql://127.0.0.1:3306/ds");
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(result.getConnection()).thenReturn(connection);
        return result;
    }
    
    @Test
    public void assertPreparedStatementRouting() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ShardingPreparedStatement statement = new ShardingPreparedStatement(new ShardingConnection(shardingDataSource), "show databases");
        Method mergeMethod = ShardingPreparedStatement.class.getDeclaredMethod("merge", MergeEngine.class);
        mergeMethod.setAccessible(true);
        mergeMethod.invoke(statement, mergeEngine);
        assertThat(TRACER.finishedSpans().size(), is(1));
    }
    
    @Test
    public void assertStatementRouting() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ShardingStatement statement = new ShardingStatement(new ShardingConnection(shardingDataSource));
        Method mergeMethod = ShardingStatement.class.getDeclaredMethod("merge", MergeEngine.class);
        mergeMethod.setAccessible(true);
        mergeMethod.invoke(statement, mergeEngine);
        assertThat(TRACER.finishedSpans().size(), is(1));
    }
    
    @Test
    public void assertException() {
        try {
            MergeEngine errorMergeEngine = new DALMergeEngine(null, null, new ShowColumnsStatement(), null);
            ShardingStatement statement = new ShardingStatement(new ShardingConnection(shardingDataSource));
            Method mergeMethod = ShardingStatement.class.getDeclaredMethod("merge", MergeEngine.class);
            mergeMethod.setAccessible(true);
            mergeMethod.invoke(statement, errorMergeEngine);
            // CHECKSTYLE:OFF
        } catch (final Exception ignored) {
            // CHECKSTYLE:ON
        }
        assertThat(TRACER.finishedSpans().size(), is(1));
        assertTrue((Boolean) TRACER.finishedSpans().get(0).tags().get(Tags.ERROR.getKey()));
    }
    
    private static void releaseTracer() throws NoSuchFieldException, IllegalAccessException {
        Field tracerField = GlobalTracer.class.getDeclaredField("tracer");
        tracerField.setAccessible(true);
        tracerField.set(GlobalTracer.class, NoopTracerFactory.create());
        Field subscribersByTypeField = EventBus.class.getDeclaredField("subscribersByType");
        subscribersByTypeField.setAccessible(true);
        subscribersByTypeField.set(ShardingEventBusInstance.getInstance(), HashMultimap.create());
    }
}
