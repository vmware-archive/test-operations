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
 * increases a value stored in an AtomicInteger array on every call.
 *
 * The execute count is kept in lowest three digits, the
 * revert count is kept in the next three digits, and the cleanup
 * count is kept in the highest three digits (all base 10).
 */
public class IncrementAsyncValidator extends ValidatorAsyncBase<Operation> {
    private final static Logger logger = LoggerFactory.getLogger(IncrementAsyncValidator.class);

    final AtomicInteger data;
    final long delayMillis;

    /**
     * Create an increment operation.  When executed it will increment
     * the value in a AtomicInteger object.
     *
     * @param data AtomicInteger container
     */
    public IncrementAsyncValidator(AtomicInteger data) {
        this(data, 0L);
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
        String result = getClass().getSimpleName() + "[";
        if (data == null) {
            result += "null]";
        } else {
            result += data.get() + "]";
        }
        return result;
    }

    @Override
    public CompletableFuture<Void> validateExecution(ExecutorService executorService, Operation initiatingOp) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Validating execution {}", toString());
            sleepSafely(delayMillis);
            data.incrementAndGet();
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> validateRevert(ExecutorService executorService, Operation initiatingOp) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Validating revert {}", toString());
            sleepSafely(delayMillis);
            data.addAndGet(1000);
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> validateCleanupAsync(ExecutorService executorService, Operation initiatingOp) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Validating revert {}", toString());
            sleepSafely(delayMillis);
            data.addAndGet(1000000);
        }, executorService);
    }

    private static void sleepSafely(long delayMillis) {
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                // Translate any exceptions
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }
}
