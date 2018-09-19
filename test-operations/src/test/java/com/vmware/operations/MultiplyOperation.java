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
 * <p>
 * AtomicIntegers are used so that concurrent access by multiple
 * instances of this operation will be thread-safe.
 */
public class MultiplyOperation extends OperationSyncBase {
    private final static Logger logger = LoggerFactory.getLogger(MultiplyOperation.class);

    AtomicInteger data;
    AtomicBoolean isExecuted = new AtomicBoolean();
    int multiplier;
    long delayMillis;

    /**
     * Create a multiplication operation.  When executed it will multiply
     * the value in a AtomicInteger object by the specified multiplier.
     *
     * @param data       AtomicInteger container
     * @param multiplier multiplication factor
     */
    public MultiplyOperation(AtomicInteger data, int multiplier) {
        super(Operations.getExecutorService());
        Assert.assertNotNull(data);
        this.data = data;
        this.multiplier = multiplier;
    }

    /**
     * Create a delayed multiplication operation.  When executed it will
     * pause for a specified number of milliseconds before multiplying
     * the value in a AtomicInteger object by the specified multiplier.
     *
     * @param data        AtomicInteger container
     * @param multiplier  multiplication factor
     * @param delayMillis delay time
     */
    public MultiplyOperation(AtomicInteger data, int multiplier, long delayMillis) {
        super(Operations.getExecutorService());
        Assert.assertNotNull(data);
        Assert.assertFalse(multiplier == 0);
        this.data = data;
        this.multiplier = multiplier;
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

        // Transactionally multiply the value in the common integer
        // If the assignment to the atomic fails, then we will retry until successful
        int oldVal, newVal;
        do {
            oldVal = data.get();
            Assert.assertFalse(oldVal == 0);

            newVal = oldVal * this.multiplier;
        } while (data.compareAndSet(oldVal, newVal) == false);

        Assert.assertTrue(isExecuted.compareAndSet(false, true));
    }

    @Override
    public void revertImpl() throws InterruptedException {
        logger.info("Reverting {}", toString());
        throwIfNotExecuted();

        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }

        // Transactionally divide the value in the common integer
        // If the assignment to the atomic fails, then we will retry until successful
        int oldVal, newVal;
        do {
            oldVal = data.get();
            Assert.assertFalse("Divide by zero error", oldVal == 0);
            Assert.assertEquals(0, oldVal % multiplier);

            newVal = oldVal / this.multiplier;
        } while (data.compareAndSet(oldVal, newVal) == false);

        Assert.assertTrue(isExecuted.compareAndSet(true, false));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + multiplier + ")";
    }
}
