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

package io.shardingsphere.orchestration.reg.newzk.client.zookeeper.section;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * Build public watcher.
 *
 * @author lidongbo
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class WatcherCreator {
    
    /**
     * Get string type data.
     *
     * @param zookeeperEventListener listener
     * @return watcher
     */
    public static Watcher deleteWatcher(final ZookeeperEventListener zookeeperEventListener) {
        return new Watcher() {
            
            @Override
            public void process(final WatchedEvent event) {
                if (zookeeperEventListener.getPath().equals(event.getPath()) && Event.EventType.NodeDeleted.equals(event.getType())) {
                    zookeeperEventListener.process(event);
                    log.debug("delete node event: {}", event.toString());
                }
            }
        };
    }
}
