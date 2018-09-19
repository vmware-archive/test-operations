/*
 * Copyright (c) 2015-2018 VMware, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vmware.operations;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An operation implementation used for testing.  This operation
 * simply increments the value stored in an AtomicInteger object.
 */
public class IncrementAsyncOperation extends OperationAsyncBase {
    private final static Logger logger = LoggerFactory.getLogger(IncrementAsyncOperation.class);

    AtomicInteger data;
    AtomicBoolean isExecuted = new AtomicBoolean();
    long delayMillis;

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Create an increment operation.  When executed it will increment
     * the value in a AtomicInteger object.
     *
     * @param data AtomicInteger container
     */
    public IncrementAsyncOperation(AtomicInteger data) {
        super(Operations.getExecutorService());
        Assert.assertNotNull(data);
        this.data = data;
    }

    /**
     * Create a delayed increment operation.  When executed it will
     * pause for a specified number of milliseconds before incrementing
     * the value in a AtomicInteger object.
     *
     * @param data        AtomicInteger container
     * @param delayMillis delay time
     */
    public IncrementAsyncOperation(AtomicInteger data, long delayMillis) {
        super(Operations.getExecutorService());
        Assert.assertNotNull(data);
        this.data = data;
        this.delayMillis = delayMillis;
    }

    @Override
    public boolean isExecuted() {
        return isExecuted.get();
    }

    @Override
    public CompletableFuture<Void> executeImpl() {
        logger.info("Executing {}", toString());
        throwIfExecuted();

        CompletableFuture<Void> result = new CompletableFuture<>();
        if (delayMillis > 0) {
            scheduler.schedule(() -> { result.complete(null); },
                    delayMillis, TimeUnit.MILLISECONDS);
        } else {
            result.complete(null);
        }

        return result.thenRunAsync(() -> {
            data.incrementAndGet();
            Assert.assertTrue(isExecuted.compareAndSet(false, true));
        });
    }

    @Override
    public CompletableFuture<Void> revertImpl() {
        logger.info("Reverting {}", toString());
        throwIfNotExecuted();

        CompletableFuture<Void> result = new CompletableFuture<>();
        if (delayMillis > 0) {
            scheduler.schedule(() -> { result.complete(null); },
                    delayMillis, TimeUnit.MILLISECONDS);
        } else {
            result.complete(null);
        }

        return result.thenRunAsync(() -> {
            data.decrementAndGet();
            Assert.assertTrue(isExecuted.compareAndSet(true, false));
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}