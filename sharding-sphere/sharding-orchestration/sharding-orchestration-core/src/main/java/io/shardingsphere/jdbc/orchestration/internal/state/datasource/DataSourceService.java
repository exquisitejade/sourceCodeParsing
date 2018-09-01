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

package io.shardingsphere.jdbc.orchestration.internal.state.datasource;

import io.shardingsphere.core.api.config.MasterSlaveRuleConfiguration;
import io.shardingsphere.core.api.config.ProxyBasicRule;
import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.rule.DataSourceParameter;
import io.shardingsphere.core.yaml.masterslave.YamlMasterSlaveRuleConfiguration;
import io.shardingsphere.jdbc.orchestration.internal.config.ConfigurationService;
import io.shardingsphere.jdbc.orchestration.internal.state.StateNode;
import io.shardingsphere.jdbc.orchestration.internal.state.StateNodeStatus;
import io.shardingsphere.orchestration.reg.api.RegistryCenter;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Data source service.
 * 
 * @author caohao
 * @author zhangliang
 */
public final class DataSourceService {
    
    private final StateNode stateNode;
    
    private final RegistryCenter regCenter;
    
    private final ConfigurationService configService;
    
    public DataSourceService(final String name, final RegistryCenter regCenter) {
        stateNode = new StateNode(name);
        this.regCenter = regCenter;
        configService = new ConfigurationService(name, regCenter);
    }
    
    /**
     * Persist master-salve data sources node.
     */
    public void persistDataSourcesNode() {
        regCenter.persist(stateNode.getDataSourcesNodeFullPath(), "");
    }
    
    /**
     * Get available data sources.
     *
     * @return available data sources
     */
    public Map<String, DataSource> getAvailableDataSources() {
        Map<String, DataSource> result = configService.loadDataSourceMap();
        Collection<String> disabledDataSourceNames = getDisabledDataSourceNames();
        for (String each : disabledDataSourceNames) {
            result.remove(each);
        }
        return result;
    }
    
    /**
     * Get available data source parameters.
     *
     * @return available data source parameters
     */
    public Map<String, DataSourceParameter> getAvailableDataSourceParameters() {
        Map<String, DataSourceParameter> result = configService.loadDataSources();
        Collection<String> disabledDataSourceNames = getDisabledDataSourceNames();
        for (String each : disabledDataSourceNames) {
            result.remove(each);
        }
        return result;
    }
    
    /**
     * Get available sharding rule configuration.
     *
     * @return available sharding rule configuration
     */
    public ShardingRuleConfiguration getAvailableShardingRuleConfiguration() {
        ShardingRuleConfiguration result = configService.loadShardingRuleConfiguration();
        Collection<String> disabledDataSourceNames = getDisabledDataSourceNames();
        for (String each : disabledDataSourceNames) {
            for (MasterSlaveRuleConfiguration masterSlaveRuleConfig : result.getMasterSlaveRuleConfigs()) {
                masterSlaveRuleConfig.getSlaveDataSourceNames().remove(each);
            }
        }
        return result;
    }
    
    /**
     * Get available master-slave rule configuration.
     *
     * @return available master-slave rule configuration
     */
    public MasterSlaveRuleConfiguration getAvailableMasterSlaveRuleConfiguration() {
        MasterSlaveRuleConfiguration result = configService.loadMasterSlaveRuleConfiguration();
        Collection<String> disabledDataSourceNames = getDisabledDataSourceNames();
        for (String each : disabledDataSourceNames) {
            result.getSlaveDataSourceNames().remove(each);
        }
        return result;
    }
    
    /**
     *  Get available proxy rule configuration.
     *
     * @return available yaml proxy configuration
     */
    public ProxyBasicRule getAvailableYamlProxyConfiguration() {
        ProxyBasicRule result = configService.loadProxyConfiguration();
        Collection<String> disabledDataSourceNames = getDisabledDataSourceNames();
        for (String each : disabledDataSourceNames) {
            result.getMasterSlaveRule().getSlaveDataSourceNames().remove(each);
            removeDisabledDataSourceNames(each, result.getShardingRule().getMasterSlaveRules());
        }
        return result;
    }
    
    private void removeDisabledDataSourceNames(final String disabledDataSourceName,
                                               final Map<String, YamlMasterSlaveRuleConfiguration> masterSlaveRules) {
        for (Map.Entry<String, YamlMasterSlaveRuleConfiguration> each : masterSlaveRules.entrySet()) {
            each.getValue().getSlaveDataSourceNames().remove(disabledDataSourceName);
        }
    }
    
    /**
     * Get disabled data source names.
     *
     * @return disabled data source names
     */
    public Collection<String> getDisabledDataSourceNames() {
        Collection<String> result = new HashSet<>();
        String dataSourcesNodePath = stateNode.getDataSourcesNodeFullPath();
        List<String> dataSources = regCenter.getChildrenKeys(dataSourcesNodePath);
        for (String each : dataSources) {
            if (StateNodeStatus.DISABLED.toString().equalsIgnoreCase(regCenter.get(dataSourcesNodePath + "/" + each))) {
                result.add(each);
            }
        }
        return result;
    }
}
