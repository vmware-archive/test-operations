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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Perform a collection of commands in parallel, waiting for
 * all of them to complete before moving on.
 */
public class OperationList extends OperationAsyncBase implements OperationCollection {

    private boolean isExecuted = false;
    private ExecutorService executorService;
    private List<Operation> operations = new ArrayList<>();
    protected int required = -1;

    /**
     * Constructor.
     * Package private to be constructed by factory method only.
     */
    OperationList(ExecutorService executorService) {
        super(executorService);
        this.executorService = executorService;
    }

    /**
     * Get the required number of successful child operations in order for this operation
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
        final CompletableFuture[] completableFutures = operations.stream()
                .map(Operation::executeAsync)
                .collect(Collectors.toList())
                .toArray(new CompletableFuture[operations.size()]);

        // Convert to single future
        return CompletableFuture.runAsync(() -> {
            try {
                // Wait for all the futures, regardless of whether they were successful
                CompletableFuture.allOf(completableFutures).get();

                // If there were no exceptions, we are executed.
                isExecuted = true;
            } catch (ExecutionException | InterruptedException | CompletionException e) {
                // Always log the errors to debug level
                Arrays.stream(completableFutures).forEach(f -> {
                    try {
                        f.getNow(null);
                    } catch (Exception ex) {
                        //logger.debug("Exception detected: ", ex);
                    }
                });

                // If there are errors, then we calculate whether to propagate them based on the
                // value of "requiredForSuccess"
                if (getRequiredForSuccess() >= 0) {
                    long succeeded = Arrays.stream(completableFutures)
                            .filter(f -> !f.isCompletedExceptionally())
                            .count();
                    if (succeeded >= getRequiredForSuccess()) {
                        isExecuted = true;
                        return;
                    }
                }

                // Wrap/unwrap the exception if necessary
                if (e instanceof CompletionException) {
                    throw (CompletionException) e;
                } else if (e instanceof ExecutionException) {
                    throw new CompletionException(e.getCause());
                } else {
                    throw new CompletionException(e);
                }
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> revertImpl() {
        finish();

        final CompletableFuture[] completableFutures = operations.stream()
                .map(Operation::revertAsync)
                .collect(Collectors.toList())
                .toArray(new CompletableFuture[operations.size()]);

        // Convert to single future
        return CompletableFuture.allOf(completableFutures)
                .thenRun(() -> {
                    isExecuted = false;
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
