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

package io.shardingsphere.transaction.base;

import com.google.common.base.Optional;
import io.shardingsphere.transaction.api.SoftTransactionManager;
import io.shardingsphere.transaction.api.config.NestedBestEffortsDeliveryJobConfiguration;
import io.shardingsphere.transaction.api.config.SoftTransactionConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import org.junit.Before;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractSoftTransactionMockTest {
    
    private SoftTransactionManager softTransactionManager;
    
    @Before
    public void setUp() {
        SoftTransactionConfiguration softTransactionConfiguration = mock(SoftTransactionConfiguration.class);
        when(softTransactionConfiguration.getTransactionLogDataSource()).thenReturn(mock(DataSource.class));
        when(softTransactionConfiguration.getBestEffortsDeliveryJobConfiguration()).thenReturn(Optional.<NestedBestEffortsDeliveryJobConfiguration>absent());
        softTransactionConfiguration.setBestEffortsDeliveryJobConfiguration(Optional.<NestedBestEffortsDeliveryJobConfiguration>absent());
        softTransactionManager = new SoftTransactionManager(softTransactionConfiguration);
    }
}
