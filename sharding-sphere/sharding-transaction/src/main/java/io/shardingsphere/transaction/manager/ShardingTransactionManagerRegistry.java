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

package io.shardingsphere.transaction.manager;

import io.shardingsphere.core.constant.transaction.TransactionType;
import io.shardingsphere.transaction.manager.local.LocalTransactionManager;
import io.shardingsphere.transaction.manager.xa.XATransactionManagerSPILoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Sharding transaction manager register.
 *
 * @author zhangliang
 */
public final class ShardingTransactionManagerRegistry {
    
    private static final ShardingTransactionManagerRegistry INSTANCE = new ShardingTransactionManagerRegistry();
    
    private final Map<TransactionType, ShardingTransactionManager> shardingTransactionManagers = new HashMap<>(TransactionType.values().length, 1);
    
    private ShardingTransactionManagerRegistry() {
        for (TransactionType each : TransactionType.values()) {
            shardingTransactionManagers.put(each, loadShardingTransactionManager(each));
        }
    }
    
    private ShardingTransactionManager loadShardingTransactionManager(final TransactionType transactionType) {
        switch (transactionType) {
            case LOCAL:
                return new LocalTransactionManager();
            case XA:
                return XATransactionManagerSPILoader.getInstance().getTransactionManager();
            case BASE:
            default: 
                return null;
        }
    }
    
    /**
     * Get instance of sharding transaction manager register.
     * 
     * @return instance of sharding transaction manager register
     */
    public static ShardingTransactionManagerRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get sharding transaction manager.
     *
     * @param transactionType transaction type
     * @return sharding transaction manager
     */
    public ShardingTransactionManager getShardingTransactionManager(final TransactionType transactionType) {
        return shardingTransactionManagers.get(transactionType);
    }
}
