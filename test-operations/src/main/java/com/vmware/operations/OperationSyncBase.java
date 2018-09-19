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

/**
 * Abstract base class for implementing operations that are implemented as
 * synchronous functions.
 *
 * This base class implements the asynchronous versions of execute, revert, and cleanup
 * in a standard way.
 */
public abstract class OperationSyncBase extends OperationBase {
    private final ExecutorService executorService;

    protected OperationSyncBase(ExecutorService executorService) {
        super();
        this.executorService = executorService;
    }

    @Override
    public final void execute() throws Exception {
        executeImpl();

        // Unwrap the async exceptions
        try {
            validateExecution(executorService).get();
        } catch (ExecutionException ex) {
            // Unwrap the exception if possible
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public final CompletableFuture<Void> executeAsync() {
        CompletableFuture<Void> executionResult = new CompletableFuture<>();
        try {
            executorService.execute(() -> {
                try {
                    executeImpl();
                    executionResult.complete(null);
                } catch (Exception ex) {
                    executionResult.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            executionResult.completeExceptionally(ex);
        }

        // Compose the validators onto the execution result
        return executionResult.thenComposeAsync(
                (unused) -> validateExecution(executorService), executorService);
    }

    @Override
    public final void revert() throws Exception {
        revertImpl();

        // Unwrap the async exceptions
        try {
            validateRevert(executorService).get();
        } catch (ExecutionException ex) {
            // Unwrap the exception if possible
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public final CompletableFuture<Void> revertAsync() {
        CompletableFuture<Void> revertResult = new CompletableFuture<>();
        try {
            executorService.execute(() -> {
                try {
                    revertImpl();
                    revertResult.complete(null);
                } catch (Exception ex) {
                    revertResult.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            revertResult.completeExceptionally(ex);
        }

        // Compose the validators onto the revert result
        return revertResult.thenComposeAsync(
                (unused) -> validateRevert(executorService), executorService);
    }

    @Override
    public void cleanup() {
        try {
            if (isExecuted()) {
                revertImpl();
            }
        } catch (Throwable throwable) {
            // Catch failures, and suppress them during cleanup
            handleCleanupException(throwable);
        }
    }

    @Override
    public CompletableFuture<Void> cleanupAsync() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            executorService.execute(() -> {
                try {
                    cleanup();
                    result.complete(null);
                } catch (Exception ex) {
                    result.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }
        return result;
    }

    @Override
    public void close() {
        cleanup();
    }

    /**
     * Synchronous implementation of the operation.
     * @throws Exception if the operation cannot be completed
     */
    public abstract void executeImpl() throws Exception;

    /**
     * Synchronous revert implementation of the operation.
     * @throws Exception if the operation cannot be reverted
     */
    public abstract void revertImpl() throws Exception;

    @Override
    public abstract String toString();
}
