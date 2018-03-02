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

/**
 * A validation instance that provides an asynchronous wrapper around
 * a synchronous implementation which determines validity of an aspect of
 * an operation.
 *
 * @param <T> Operation type this validator can be attached to.
 */
public abstract class ValidatorSyncBase<T> implements Validator {
    @Override
    public CompletableFuture<Void> validateExecutionAsync(ExecutorService executorService, Operation initiatingOp) {
        CompletableFuture<Void> executeResult = new CompletableFuture<>();
        try {
            executorService.execute(() -> {
                try {
                    validateExecution((T) initiatingOp);
                    executeResult.complete(null);
                } catch (Exception ex) {
                    executeResult.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            executeResult.completeExceptionally(ex);
        }

        return executeResult;
    }

    @Override
    public CompletableFuture<Void> validateRevertAsync(ExecutorService executorService, Operation initiatingOp) {
        CompletableFuture<Void> executeResult = new CompletableFuture<>();
        try {
            executorService.execute(() -> {
                try {
                    validateRevert((T) initiatingOp);
                    executeResult.complete(null);
                } catch (Exception ex) {
                    executeResult.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            executeResult.completeExceptionally(ex);
        }

        return executeResult;
    }

    public abstract void validateExecution(T initiatingOp) throws Exception;

    public abstract void validateRevert(T initiatingOp) throws Exception;
}
