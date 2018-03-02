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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An operation implementation used for testing.  This operation
 * simply increments the value stored in an AtomicInteger object.
 */
public class IncrementAsyncValidator extends ValidatorAsyncBase<Operation> {
    private final static Logger logger = LoggerFactory.getLogger(IncrementAsyncValidator.class);

    AtomicInteger data;
    AtomicBoolean isExecuted = new AtomicBoolean();
    long delayMillis;

    /**
     * Create an increment operation.  When executed it will increment
     * the value in a AtomicInteger object.
     *
     * @param data AtomicInteger container
     */
    public IncrementAsyncValidator(AtomicInteger data) {
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
    public IncrementAsyncValidator(AtomicInteger data, long delayMillis) {
        Assert.assertNotNull(data);
        this.data = data;
        this.delayMillis = delayMillis;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public CompletableFuture<Void> validateExecution(ExecutorService executorService, Operation initiatingOp) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Validating execution {}", toString());

            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    // Translate any exceptions
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }

            data.incrementAndGet();
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> validateRevert(ExecutorService executorService, Operation initiatingOp) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Validating revert {}", toString());

            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    // Translate any exceptions
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }

            data.decrementAndGet();
        }, executorService);
    }
}