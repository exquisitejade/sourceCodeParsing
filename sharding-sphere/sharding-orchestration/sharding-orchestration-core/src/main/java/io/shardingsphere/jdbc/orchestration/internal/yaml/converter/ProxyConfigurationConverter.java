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

package io.shardingsphere.jdbc.orchestration.internal.yaml.converter;

import io.shardingsphere.core.api.config.ProxyBasicRule;
import io.shardingsphere.jdbc.orchestration.internal.yaml.representer.DefaultConfigurationRepresenter;
import io.shardingsphere.jdbc.orchestration.internal.yaml.representer.ProxyConfigurationRepresenter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.Yaml;

/**
 * Proxy Config converter.
 *
 * @author panjuan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProxyConfigurationConverter {
    
    private static final Yaml YAML = new Yaml(new DefaultConfigurationRepresenter());
    
    /**
     * Convert yaml proxy configuration to yaml string.
     *
     * @param proxyBasicRule Yaml proxy configuration
     * @return Yaml string
     */
    public static String proxyConfigToYaml(final ProxyBasicRule proxyBasicRule) {
        Yaml yaml = new Yaml(new ProxyConfigurationRepresenter());
        return yaml.dumpAsMap(proxyBasicRule);
    }
    
    /**
     * Convert yaml to proxy configuration .
     *
     * @param yamlProxyConfigYamlString String in yaml
     * @return Yaml proxy configuration
     */
    public static ProxyBasicRule proxyConfigFromYaml(final String yamlProxyConfigYamlString) {
        return YAML.loadAs(yamlProxyConfigYamlString, ProxyBasicRule.class);
    }
}
