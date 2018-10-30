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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for implementing operations that are naturally asynchronous,
 * based on futures.
 *
 * This base class implements the synchronous versions of execute, revert, and cleanup
 * in a standard way.  In particular, it attempts to unwrap any thrown ExecutionExceptions
 * for readability.
 */
public abstract class OperationAsyncBase extends OperationBase {
    private static final Logger logger = LoggerFactory.getLogger(OperationAsyncBase.class.getPackage().getName());

    private final ExecutorService executorService;

    protected OperationAsyncBase(ExecutorService executorService) {
        super();
        this.executorService = executorService;
    }

    /**
     * @see com.vmware.operations.Operation#execute()
     */
    @Override
    public final void execute() throws Throwable {
        try {
            logger.debug("Executing {} [async]", toString());
            executeAsync().get();
        } catch (ExecutionException ex) {
            // Unwrap the exception if possible
            Throwable cause = ex.getCause();
            if (cause != null) {
                throw cause;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public final CompletableFuture<Void> executeAsync() {
        // Compose the validators onto the execution result
        return executeImpl()
                .thenComposeAsync((unused) -> validateExecution(executorService), executorService);
    }

    /**
     * @see com.vmware.operations.Operation#revert()
     */
    @Override
    public final void revert() throws Throwable {
        try {
            logger.debug("Reverting {} [async]", toString());
            revertAsync().get();
        } catch (ExecutionException ex) {
            // Unwrap the exception if possible
            Throwable cause = ex.getCause();
            if (cause != null) {
                throw cause;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public final CompletableFuture<Void> revertAsync() {
        // If revert fails, then call cleanup
        return revertImpl()
                .thenCompose(unused -> validateRevert(executorService));
    }

    /**
     * @see com.vmware.operations.Operation#cleanup()
     */
    @Override
    public final void cleanup() {
        try {
            cleanupAsync().get();
        } catch (Exception ex) {
            // This should never happen
        }
    }

    /**
     * @see com.vmware.operations.Operation#cleanupAsync()
     */
    @Override
    public CompletableFuture<Void> cleanupAsync() {
        CompletableFuture<Void> start = isExecuted() ? revertAsync() : CompletableFuture.completedFuture(null);
        return start.exceptionally(throwable -> validateCleanup(executorService).join())
                .exceptionally(throwable -> {
                    // Catch all failures, and suppress them during cleanup
                    logger.info("Cleanup validator error {}", throwable.getMessage());
                    return null;
                });
    }

    /**
     * @see com.vmware.operations.Operation#close()
     */
    @Override
    public void close() {
        try {
            cleanup();
        } catch (Throwable th) {
            // Catch all failures, and suppress them during cleanup
        }
    }

    /**
     * Asynchronous implementation of the operation.
     * @return A future that completes when the command is complete
     */
    public abstract CompletableFuture<Void> executeImpl();

    /**
     * Asynchronous revert implementation of the operation.
     * @return A future that completes when the command is reverted
     */
    public abstract CompletableFuture<Void> revertImpl();

    @Override
    public abstract String toString();
}
