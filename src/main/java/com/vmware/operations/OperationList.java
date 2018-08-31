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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform a collection of commands in parallel, waiting for
 * all of them to complete before moving on.
 */
public class OperationList extends OperationAsyncBase implements OperationCollection {
    private static final Logger logger = LoggerFactory.getLogger(OperationBase.class.getPackage().getName());
    private boolean isExecuted = false;
    private ExecutorService executorService;

    /**
     * The list of operations that should be executed as part of this list.
     */
    private List<Operation> operations = new ArrayList<>();

    /**
     * The list of operations successfully executed by this list.
     */
    private List<Operation> completed = new ArrayList<>();

    /**
     * The number of operations that need to successfully complete in
     * order for this list to execute successfully.
     */
    protected int required = -1;

    /**
     * Constructor.
     * Package private to be constructed by factory method only.
     * @param executorService An executor for running validations on other threads
     */
    OperationList(ExecutorService executorService) {
        super(executorService);
        this.executorService = executorService;
    }

    /**
     * @return the required number of successful child operations in order for this operation
     * to succeed.  If the result is negative, or greater then the length of the list, then
     * any child exception will be propagated to the list operation, causing the list operation to fail.
     */
    public int getRequiredForSuccess() {
        return this.required;
    }

    /**
     * Set the required number of successful child operations in order for this operation
     * to succeed.  The default is that all operations must succeed.
     * This method can be used to make "any" lists (default is "all" in this parlance).
     * @param count count of the number of operations required to succeed.  If this is
     *              zero, then no errors will ever be propagated, and if it is negative
     *              then any error will be propagated.
     */
    public void setRequiredForSuccess(int count) {
        this.required = count;
    }

    @Override
    public void add(Operation op) {
        operations.add(op);
    }

    @Override
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    @Override
    public int size() {
        return operations.size();
    }

    @Override
    public List<Operation> getOperations() {
        return operations;
    }

    @Override
    public void finish() {
        operations = Collections.unmodifiableList(operations);
    }

    @Override
    public CompletableFuture<Void> executeImpl() {
        finish();

        // Get a future for each operation
        final Map<CompletableFuture<Void>, Operation> completableFutures = operations.stream()
                .collect(Collectors.toMap(Operation::executeAsync, op -> op));

        // Suppress warnings around the generic array creation
        @SuppressWarnings({"unchecked"})
        CompletableFuture<Void>[] futuresArray = new CompletableFuture[completableFutures.size()];
        completableFutures.keySet().toArray(futuresArray);

        // Wait for all the futures, regardless of whether they were successful
        return CompletableFuture.allOf(futuresArray)
                .handleAsync((unused, throwable) -> {
                    if (throwable == null) {
                        // If there were no exceptions, we are executed.
                        isExecuted = true;
                        completed = operations;
                        return null;
                    }

                    // Always log exceptions to trace level
                    Arrays.stream(futuresArray).forEach(f -> {
                        try {
                            f.getNow(null);
                            completed.add(completableFutures.get(f));
                        } catch (Exception ex) {
                            logger.trace("Exception detected: ", ex);
                        }
                    });

                    // If there are errors, then we calculate whether to propagate them based on the
                    // value of "requiredForSuccess"
                    if (getRequiredForSuccess() >= 0) {
                        if (completed.size() >= getRequiredForSuccess()) {
                            isExecuted = true;
                            return null;
                        }
                    }

                    // Wrap/unwrap the exception if necessary
                    if (throwable instanceof CompletionException) {
                        throw (CompletionException) throwable;
                    } else if (throwable instanceof ExecutionException) {
                        throw new CompletionException(throwable.getCause());
                    } else {
                        throw new CompletionException(throwable);
                    }
                }, executorService);
    }


    @Override
    public CompletableFuture<Void> revertImpl() {
        finish();
        throwIfNotExecuted();

        final CompletableFuture[] completableFutures = completed.stream()
                .map(Operation::revertAsync)
                .collect(Collectors.toList())
                .toArray(new CompletableFuture[completed.size()]);

        // Convert to single future
        return CompletableFuture.allOf(completableFutures)
                .thenRun(() -> {
                    isExecuted = false;
                    completed = null;
                });
    }

    @Override
    public CompletableFuture<Void> cleanupAsync() {
        finish();

        // Get a future for each operation
        final CompletableFuture[] completableFutures = operations.stream()
                .map(Operation::cleanupAsync)
                .collect(Collectors.toList())
                .toArray(new CompletableFuture[operations.size()]);

        // Convert to single future
        return CompletableFuture.runAsync(() -> {
            // Wait for all the futures, regardless of whether they were successful
            try {
                CompletableFuture.allOf(completableFutures).get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new CompletionException(ex);
            } finally {
                // Ignore exceptions during cleanup, and reset the operation anyway
                operations = null;
                isExecuted = false;
            }
        }, executorService);
    }

    @Override
    public boolean isExecuted() {
        return isExecuted;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "(@"
                + Integer.toHexString(System.identityHashCode(this)) + ", " + operations.size() + " items)";
    }
}
