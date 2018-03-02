/*
 * Copyright (c) 2017-2018 VMware, Inc. All Rights Reserved.
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
public abstract class ValidatorAsyncBase<T extends Operation> implements Validator {
    @Override
    public CompletableFuture<Void> validateExecutionAsync(ExecutorService executorService, Operation initiatingOp) {
        try {
            return validateExecution(executorService, (T) initiatingOp);
        } catch (Exception ex) {
            CompletableFuture result = new CompletableFuture();
            result.completeExceptionally(ex);
            return result;
        }

    }

    @Override
    public CompletableFuture<Void> validateRevertAsync(ExecutorService executorService, Operation initiatingOp) {
        try {
            return validateRevert(executorService, (T) initiatingOp);
        } catch (Exception ex) {
            CompletableFuture result = new CompletableFuture();
            result.completeExceptionally(ex);
            return result;
        }
    }

    public abstract CompletableFuture<Void> validateExecution(ExecutorService executorService,
                                                              T initiatingOp) throws Exception;

    public abstract CompletableFuture<Void> validateRevert(ExecutorService executorService,
                                                           T initiatingOp) throws Exception;
}
