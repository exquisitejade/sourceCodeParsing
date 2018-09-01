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

package io.shardingsphere.orchestration.reg.newzk.client.zookeeper;


import io.shardingsphere.orchestration.reg.newzk.client.cache.CacheStrategy;
import io.shardingsphere.orchestration.reg.newzk.client.cache.PathTree;
import io.shardingsphere.orchestration.reg.newzk.client.utility.PathUtil;
import io.shardingsphere.orchestration.reg.newzk.client.utility.ZookeeperConstants;
import io.shardingsphere.orchestration.reg.newzk.client.zookeeper.base.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.List;

/**
 * Cache Client.
 * 
 * @author lidongbo
 */
// TODO Partially prepared product
@Slf4j
public final class CacheClient extends UsualClient {
    
    private PathTree pathTree;
    
    CacheClient(final BaseContext context) {
        super(context);
    }
    
    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        try {
            useCacheStrategy(CacheStrategy.WATCH);
        } catch (final KeeperException ex) {
            log.error("CacheClient useCacheStrategy : " + ex.getMessage());
        }
    }
    
    @Override
    public void close() {
        super.close();
        pathTree.close();
    }
    
    //todo put it here?
    private void useCacheStrategy(final CacheStrategy cacheStrategy) throws KeeperException, InterruptedException {
        log.debug("use cache strategy:{}", cacheStrategy);
        switch (cacheStrategy) {
            case WATCH:
                pathTree = new PathTree(getRootNode(), this);
                pathTree.watch();
                break;
            case ALL:
                pathTree = loadPathTree();
                pathTree.refreshPeriodic(ZookeeperConstants.THREAD_PERIOD);
                break;
            case NONE:
            default:
        }
    }
    
    private PathTree loadPathTree() throws KeeperException, InterruptedException {
        return loadPathTree(getRootNode());
    }
    
    private PathTree loadPathTree(final String treeRoot) throws KeeperException, InterruptedException {
        PathTree result = new PathTree(treeRoot, this);
        log.debug("load path result: {}", treeRoot);
        result.load();
        result.watch();
        return result;
    }
    
    @Override
    public void createCurrentOnly(final String key, final String value, final CreateMode createMode) throws KeeperException, InterruptedException {
        super.createCurrentOnly(key, value, createMode);
        pathTree.put(PathUtil.getRealPath(getRootNode(), key), value);
    }
    
    @Override
    public void deleteOnlyCurrent(final String key) throws KeeperException, InterruptedException {
        super.deleteOnlyCurrent(key);
        pathTree.delete(PathUtil.getRealPath(getRootNode(), key));
    }
    
    @Override
    public void deleteOnlyCurrent(final String key, final AsyncCallback.VoidCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        super.deleteOnlyCurrent(key, callback, ctx);
        pathTree.delete(PathUtil.getRealPath(getRootNode(), key));
    }
    
    @Override
    public byte[] getData(final String key) throws KeeperException, InterruptedException {
        String path = PathUtil.getRealPath(getRootNode(), key);
        byte[] data = pathTree.getValue(path);
        if (null != data) {
            log.debug("getData cache hit: {}", key);
            return data;
        }
        log.debug("getData cache not hit: {}", key);
        return getExecStrategy().getData(key);
    }
    
    @Override
    public List<String> getChildren(final String key) throws KeeperException, InterruptedException {
        String path = PathUtil.getRealPath(getRootNode(), key);
        List<String> keys = pathTree.getChildren(path);
        if (keys != null && !keys.isEmpty()) {
            log.debug("getChildren cache hit: {}", keys);
            return keys;
        }
        log.debug("getChildren cache not hit: {}", keys);
        return getExecStrategy().getChildren(PathUtil.getRealPath(getRootNode(), key));
    }
}
