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

package io.shardingsphere.core.executor;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.shardingsphere.core.exception.ShardingException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sharding execute engine.
 * 
 * @author zhangliang
 */
public final class ShardingExecuteEngine implements AutoCloseable {
    
    private static final ExecutorService SHUTDOWN_EXECUTOR = Executors.newSingleThreadExecutor(ShardingThreadFactoryBuilder.build("Executor-Engine-Closer"));
    
    private final ListeningExecutorService executorService;
    
    public ShardingExecuteEngine(final int executorSize) {
        executorService = MoreExecutors.listeningDecorator(
                0 == executorSize ? Executors.newCachedThreadPool(ShardingThreadFactoryBuilder.build()) : Executors.newFixedThreadPool(executorSize, ShardingThreadFactoryBuilder.build()));
        MoreExecutors.addDelayedShutdownHook(executorService, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Execute all callbacks.
     *
     * @param inputs input values
     * @param callback sharding execute callback
     * @param <I> type of input value
     * @param <O> type of return value
     * @return execute result
     * @throws SQLException throw if execute failure
     */
    public <I, O> List<O> execute(final Collection<I> inputs, final ShardingExecuteCallback<I, O> callback) throws SQLException {
        return execute(inputs, null, callback);
    }
    
    /**
     * Execute all callbacks.
     *
     * @param inputs input values
     * @param firstCallback first sharding execute callback
     * @param callback sharding execute callback
     * @param <I> type of input value
     * @param <O> type of return value
     * @return execute result
     * @throws SQLException throw if execute failure
     */
    public <I, O> List<O> execute(final Collection<I> inputs, final ShardingExecuteCallback<I, O> firstCallback, final ShardingExecuteCallback<I, O> callback) throws SQLException {
        if (inputs.isEmpty()) {
            return Collections.emptyList();
        }
        Iterator<I> inputIterator = inputs.iterator();
        I firstInput = inputIterator.next();
        Collection<ListenableFuture<O>> restFutures = asyncExecute(Lists.newArrayList(inputIterator), callback);
        return getResults(syncExecute(firstInput, null == firstCallback ? callback : firstCallback), restFutures);
    }
    
    private <I, O> Collection<ListenableFuture<O>> asyncExecute(final Collection<I> inputs, final ShardingExecuteCallback<I, O> callback) {
        Collection<ListenableFuture<O>> result = new ArrayList<>(inputs.size());
        for (final I each : inputs) {
            result.add(executorService.submit(new Callable<O>() {
                
                @Override
                public O call() throws SQLException {
                    return callback.execute(each);
                }
            }));
        }
        return result;
    }
    
    private <I, O> O syncExecute(final I input, final ShardingExecuteCallback<I, O> callback) throws SQLException {
        return callback.execute(input);
    }
    
    private <O> List<O> getResults(final O firstResult, final Collection<ListenableFuture<O>> restFutures) throws SQLException {
        List<O> result = new LinkedList<>();
        result.add(firstResult);
        for (ListenableFuture<O> each : restFutures) {
            try {
                result.add(each.get());
            } catch (final InterruptedException | ExecutionException ex) {
                return throwException(ex);
            }
        }
        return result;
    }
    
    /**
     * execute all callbacks for group.
     *
     * @param inputs input value's map
     * @param callback sharding execute callback
     * @param <I> type of input value
     * @param <O> type of return value
     * @return execute result
     * @throws SQLException throw if execute failure
     */
    public <I, O> List<O> groupExecute(final Map<String, List<List<I>>> inputs, final ShardingGroupExecuteCallback<I, O> callback) throws SQLException {
        return groupExecute(inputs, null, callback);
    }
    
    /**
     * execute all callbacks for group.
     *
     * @param inputs input value's map
     * @param callback sharding execute callback
     * @param firstCallback first sharding execute callback
     * @param <I> type of input value
     * @param <O> type of return value
     * @return execute result
     * @throws SQLException throw if execute failure
     */
    public <I, O> List<O> groupExecute(
            final Map<String, List<List<I>>> inputs, final ShardingGroupExecuteCallback<I, O> firstCallback, final ShardingGroupExecuteCallback<I, O> callback) throws SQLException {
        if (inputs.isEmpty()) {
            return Collections.emptyList();
        }
        String firstKey = inputs.keySet().iterator().next();
        Iterator<List<I>> firstInputGroup = inputs.get(firstKey).iterator();
        Collection<I> firstInputs = firstInputGroup.next();
        inputs.put(firstKey, Lists.newArrayList(firstInputGroup));
        Collection<ListenableFuture<Collection<O>>> restResultFutures = asyncGroupExecute(inputs, callback);
        return getGroupResults(syncGroupExecute(firstInputs, null == firstCallback ? callback : firstCallback), restResultFutures);
    }
    
    private <I, O> Collection<ListenableFuture<Collection<O>>> asyncGroupExecute(final Map<String, List<List<I>>> inputs, final ShardingGroupExecuteCallback<I, O> callback) {
        Collection<ListenableFuture<Collection<O>>> result = new LinkedList<>();
        for (Entry<String, List<List<I>>> entry : inputs.entrySet()) {
            result.addAll(asyncGroupExecute(entry.getValue(), callback));
        }
        return result;
    }
    
    private <I, O> Collection<ListenableFuture<Collection<O>>> asyncGroupExecute(final List<List<I>> inputs, final ShardingGroupExecuteCallback<I, O> callback) {
        Collection<ListenableFuture<Collection<O>>> result = new LinkedList<>();
        for (final List<I> each : inputs) {
            result.add(executorService.submit(new Callable<Collection<O>>() {
                
                @Override
                public Collection<O> call() throws SQLException {
                    return callback.execute(each);
                }
            }));
        }
        return result;
    }
    
    private <I, O> Collection<O> syncGroupExecute(final Collection<I> inputs, final ShardingGroupExecuteCallback<I, O> callback) throws SQLException {
        return callback.execute(inputs);
    }
    
    private <O> List<O> getGroupResults(final Collection<O> firstResults, final Collection<ListenableFuture<Collection<O>>> restFutures) throws SQLException {
        List<O> result = new LinkedList<>();
        result.addAll(firstResults);
        for (ListenableFuture<Collection<O>> each : restFutures) {
            try {
                result.addAll(each.get());
            } catch (final InterruptedException | ExecutionException ex) {
                return throwException(ex);
            }
        }
        return result;
    }
    
    private <O> List<O> throwException(final Exception ex) throws SQLException {
        if (ex.getCause() instanceof SQLException) {
            throw (SQLException) ex.getCause();
        }
        throw new ShardingException(ex);
    }
    
    @Override
    public void close() {
        SHUTDOWN_EXECUTOR.execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    executorService.shutdown();
                    while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
