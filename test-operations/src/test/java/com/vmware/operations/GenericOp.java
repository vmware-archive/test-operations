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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic operation implementation that uses dynamic function calls
 * for execute and revert.
 * <p/>
 * This allows for dynamic operations, or linking two operations together.
 */
public class GenericOp<T> extends OperationSyncBase {
    private final static Logger logger = LoggerFactory.getLogger(GenericOp.class);

    private String name;
    private T data;
    private Deque<Function<T, Void>> executeFunctions = new ArrayDeque<>(4);
    private Deque<Function<T, Void>> revertFunctions = new ArrayDeque<>(4);
    private AtomicBoolean isExecuted = new AtomicBoolean();

    /**
     * Constructor of the generic operation.  Execute and revert
     * functions are added dynamically by the caller.
     *
     * @param name name of this operation as displayed in the console
     * @param data data object passed from constructor
     */
    public GenericOp(String name, T data) {
        super(Operations.getExecutorService());
        this.name = name;
        this.data = data;
    }

    /**
     * Constructor of the generic operation.  Execute and revert
     * functions are added dynamically by the caller.
     *
     * @param data data object passed from constructor
     */
    public GenericOp(T data) {
        super(Operations.getExecutorService());
        this.data = data;
    }

    /**
     * Add a new execution function to the operation.
     * Functions are executed in FIFO order.
     * This function cannot be called after the operation has been executed.
     *
     * @param function work function called during execute phase of the operation
     */
    public void addExecuteFunction(Function<T, Void> function) {
        throwIfExecuted();
        this.executeFunctions.addLast(function);
    }

    /**
     * Add a new revert function to the operation.
     * Functions are executed in LIFO order, i.e. reverse of the calling order.
     * Revert functions are only called if the execute phase completed without errors.
     *
     * @param function work function called during revert and cleanup phase of the operation
     */
    public void addRevertFunction(Function<T, Void> function) {
        this.revertFunctions.addFirst(function);
    }

    @Override
    public boolean isExecuted() {
        return isExecuted.get();
    }

    @Override
    public void executeImpl() {
        logger.info("Executing {}", toString());

        throwIfExecuted();
        for (Function<T, Void> fn : executeFunctions) {
            fn.apply(data);
        }
        Assert.assertTrue(isExecuted.compareAndSet(false, true));
    }

    @Override
    public void revertImpl() {
        logger.info("Reverting {}", toString());

        throwIfNotExecuted();
        for (Function<T, Void> fn : revertFunctions) {
            fn.apply(data);
        }
        Assert.assertTrue(isExecuted.compareAndSet(true, false));
    }

    /**
     * Clean up the revert functions individually, in case
     * some throw execptions during their revert functionality.
     */
    @Override
    public void cleanup() {
        logger.info("Cleaning {}", toString());

        if (isExecuted()) {
            for (Function<T, Void> fn : revertFunctions) {
                try {
                    fn.apply(data);
                } catch (Throwable t) {
                    // Catch failures, and suppress them during cleanup
                    logger.debug("Cleanup error {}", t.getMessage());
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName());
        if (name != null) {
            result.append("(");
            result.append(name);
            result.append(")");
        }
        return result.toString();
    }
}