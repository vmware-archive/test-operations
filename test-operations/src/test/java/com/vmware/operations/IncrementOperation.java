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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An operation implementation used for testing.  This operation
 * simply increments the value stored in an AtomicInteger object.
 */
public class IncrementOperation extends OperationSyncBase {
    private final static Logger logger = LoggerFactory.getLogger(IncrementOperation.class);

    AtomicInteger data;
    AtomicBoolean isExecuted = new AtomicBoolean();
    long delayMillis;

    /**
     * Create an increment operation.  When executed it will increment
     * the value in a AtomicInteger object.
     *
     * @param data AtomicInteger container
     */
    public IncrementOperation(AtomicInteger data) {
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
    public IncrementOperation(AtomicInteger data, long delayMillis) {
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
    public void executeImpl() throws InterruptedException {
        logger.info("Executing {}", toString());

        throwIfExecuted();
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }

        data.incrementAndGet();
        Assert.assertTrue(isExecuted.compareAndSet(false, true));
    }

    @Override
    public void revertImpl() throws InterruptedException {
        logger.info("Reverting {}", toString());
        throwIfNotExecuted();
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }

        data.decrementAndGet();
        Assert.assertTrue(isExecuted.compareAndSet(true, false));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
