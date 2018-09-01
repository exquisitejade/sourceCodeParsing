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

package io.shardingsphere.transaction.listener.local;

import com.google.common.eventbus.EventBus;
import io.shardingsphere.core.constant.transaction.TransactionOperationType;
import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.core.event.transaction.local.LocalTransactionEvent;
import io.shardingsphere.transaction.manager.ShardingTransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class LocalTransactionListenerTest {
    
    private final EventBus eventBus = ShardingEventBusInstance.getInstance();
    
    private final LocalTransactionListener localTransactionListener = new LocalTransactionListener();
    
    @Mock
    private ShardingTransactionManager shardingTransactionManager;
    
    @Before
    public void setUp() throws ReflectiveOperationException {
        Field field = LocalTransactionListener.class.getDeclaredField("shardingTransactionManager");
        field.setAccessible(true);
        field.set(localTransactionListener, shardingTransactionManager);
        localTransactionListener.register();
    }
    
    @After
    public void tearDown() {
        eventBus.unregister(localTransactionListener);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertListenBegin() throws SQLException {
        LocalTransactionEvent event = new LocalTransactionEvent(TransactionOperationType.BEGIN, Collections.<Connection>emptyList(), false);
        eventBus.post(event);
        verify(shardingTransactionManager).begin(event);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertListenCommit() throws SQLException {
        LocalTransactionEvent event = new LocalTransactionEvent(TransactionOperationType.COMMIT, Collections.<Connection>emptyList(), false);
        eventBus.post(event);
        verify(shardingTransactionManager).commit(event);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertListenRollback() throws SQLException {
        LocalTransactionEvent event = new LocalTransactionEvent(TransactionOperationType.ROLLBACK, Collections.<Connection>emptyList(), false);
        eventBus.post(event);
        verify(shardingTransactionManager).rollback(event);
    }
}
