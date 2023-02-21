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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic operation implementation that uses dynamic function calls
 * for execute and revert.
 * <p>
 * This allows for dynamic operations, or linking two operations together.
 * @param <T> shared data passed to dynamic functions
 */
public class GenericOp<T> extends OperationSyncBase {
    private static final Logger logger = LoggerFactory.getLogger(GenericOp.class);

    private String name;
    private T data;
    private Deque<Consumer<T>> executeFunctions = new ArrayDeque<>();
    private Deque<Consumer<T>> revertFunctions = new ArrayDeque<>();

    private Deque<Consumer<T>> uncalledRevertFunctions = null;

    /**
     * Constructor of the generic operation.  Execute and revert
     * functions are added dynamically by the caller.
     *
     * @param data data object passed from constructor
     */
    public GenericOp(T data) {
        this(null, data);
    }

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
     * Add a new execution function to the operation.
     * Functions are executed in FIFO order.
     * This function cannot be called after the operation has been executed.
     *
     * @param function work function called during execute phase of the operation
     */
    public void addExecuteFunction(Consumer<T> function) {
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
    public void addRevertFunction(Consumer<T> function) {
        this.revertFunctions.addFirst(function);
    }

    @Override
    public boolean isExecuted() {
        return uncalledRevertFunctions != null;
    }

    @Override
    public void executeImpl() {
        logger.info("Executing {}", toString());

        throwIfExecuted();
        for (Consumer<T> fn : executeFunctions) {
            fn.accept(data);
        }

        // copy revert functions to uncalled list
        uncalledRevertFunctions = new ArrayDeque<>(revertFunctions);
    }

    T getData() {
        return this.data;
    }

    @Override
    public void revertImpl() {
        logger.info("Reverting {}", toString());

        throwIfNotExecuted();

        if (uncalledRevertFunctions != null) {
            while (uncalledRevertFunctions.size() > 0) {
                Consumer<T> fn = uncalledRevertFunctions.getFirst();
                fn.accept(data);
                uncalledRevertFunctions.removeFirst();
            }

            uncalledRevertFunctions = null;
        }
    }

    /**
     * Clean up the revert functions individually, in case
     * some throw exceptions during their revert functionality.
     */
    @Override
    public void cleanup() {
        logger.info("Cleaning {}", toString());

        if (uncalledRevertFunctions != null) {
            while (uncalledRevertFunctions.size() > 0) {
                Consumer<T> fn = uncalledRevertFunctions.getFirst();

                try {
                    fn.accept(data);
                } catch (Throwable t) {
                    // Catch failures, and suppress them during cleanup
                    logger.debug("Cleanup error {}", t.getMessage());
                }
                uncalledRevertFunctions.removeFirst();
            }

            uncalledRevertFunctions = null;
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
