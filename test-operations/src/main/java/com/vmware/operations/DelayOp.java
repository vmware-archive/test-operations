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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic operation implementation that uses dynamic function calls
 * for execute and revert.
 * <p/>
 * This allows for dynamic operations, or linking two operations together.
 */
public class DelayOp extends OperationSyncBase {
    private final static Logger logger = LoggerFactory.getLogger(DelayOp.class);

    // Maximum delay is 100 minutes
    private final long maximumDelayMs = 100 * 60 * 1000L;

    private long executeDelayMs;
    private long revertDelayMs;
    private AtomicBoolean isExecuted = new AtomicBoolean();

    /**
     * Constructor of the delay operation.
     */
    public DelayOp() {
        super(Operations.getExecutorService());
        executeDelayMs = 0L;
        revertDelayMs = 0L;
    }

    /**
     * Alternate constructor for the most basic case of delaying the execution.
     */
    public DelayOp(Duration d) {
        super(Operations.getExecutorService());

        this.executeDelay(d);
        revertDelayMs = 0L;
    }

    /**
     * Set the delay during execution.
     *
     * @param d delay time expressed as a {@link java.time.Duration}
     * @return fluent operation returns itself
     */
    public DelayOp executeDelay(Duration d) {
        long millis = d.toMillis();
        if (millis < 0L || millis > maximumDelayMs) {
            throw new IllegalArgumentException("delay must be between 0 and " + maximumDelayMs + " ms");
        }
        this.executeDelayMs = millis;
        return this;
    }

    /**
     * Set the delay during revert.
     *
     * @param d delay time expressed as a {@link java.time.Duration}
     * @return fluent operation returns itself
     */
    public DelayOp revertDelay(Duration d) {
        long millis = d.toMillis();
        if (millis < 0L || millis > maximumDelayMs) {
            throw new IllegalArgumentException("delay must be between 0 and " + maximumDelayMs + " ms");
        }
        this.revertDelayMs = millis;
        return this;
    }

    @Override
    public boolean isExecuted() {
        return isExecuted.get();
    }

    @Override
    public void executeImpl() throws InterruptedException {
        logger.info("Executing {} for {} ms", toString(), executeDelayMs);

        throwIfExecuted();
        if (executeDelayMs > 0L) {
            Thread.sleep(executeDelayMs);
        }

        if (!isExecuted.compareAndSet(false, true)) {
            throw new AssertionError("Error setting isExecuted true");
        }
    }

    @Override
    public void revertImpl() throws InterruptedException {
        logger.info("Executing {} for {} ms", toString(), revertDelayMs);

        throwIfNotExecuted();
        if (revertDelayMs > 0) {
            Thread.sleep(revertDelayMs);
        }

        if (!isExecuted.compareAndSet(true, false)) {
            throw new AssertionError("Error setting isExecuted false");
        }
    }


    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
