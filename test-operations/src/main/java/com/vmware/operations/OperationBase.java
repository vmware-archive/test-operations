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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all operations.  Provides utility methods.
 */
public abstract class OperationBase implements Operation {
    private static final Logger logger = LoggerFactory.getLogger(OperationBase.class.getPackage().getName());
    private List<Validator> validatorList;
    private boolean validationsExecuted;
    private boolean validationsReverted;

    protected OperationBase() {
        this.validatorList = new ArrayList<>();
    }

    public void handleCleanupException(Throwable th) {
        logger.debug("Cleanup error {}", th.getMessage());
    }

    @Override
    public void addValidator(Validator validator) {
        validatorList.add(validator);
    }

    @Override
    public boolean removeValidator(Class<?> clazz) {
        return validatorList.removeIf((validator) -> clazz.isInstance(validator));
    }

    @Override
    public boolean removeValidator(Predicate<Validator> predicate) {
        return validatorList.removeIf(predicate);
    }

    @Override
    public void removeAllValidators() {
        validatorList.clear();
    }

    /**
     * Verify that this operation has not been executed yet, and throw if it has.
     *
     * @throws IllegalStateException if this operation has already been executed
     */
    protected void throwIfExecuted() throws IllegalStateException {
        if (isExecuted()) {
            throw new IllegalStateException("Execute called on executed operation");
        }
    }

    /**
     * Verify that this operation has been executed successfully, and throw if it hasn't.
     *
     * @throws IllegalStateException if this operation has not been executed
     */
    protected void throwIfNotExecuted() throws IllegalStateException {
        if (!isExecuted()) {
            throw new IllegalStateException("Revert called on unexecuted operation");
        }
    }

    /**
     * Wait for the futures of all the validators to complete (or return exceptionally).
     *
     * @param executorService execution context for the internal async calls
     * @return CompletableFuture of the results.  If any of the validations failed, then
     * the result future will hold an exception of one of the failures.
     */
    protected CompletableFuture<Void> validateExecution(ExecutorService executorService) {
        CompletableFuture[] validations = new CompletableFuture[validatorList.size()];
        for (int i = 0; i < validatorList.size(); ++i) {
            validations[i] = validatorList.get(i).validateExecutionAsync(executorService, this);
        }
        validationsExecuted = true;
        validationsReverted = false;
        return CompletableFuture.allOf(validations);
    }

    /**
     * Wait for the futures of all the validators to complete (or return exceptionally).
     *
     * This method tracks whether revert has been called, but it is the caller's
     * responsibility to ensure that it is only called once.
     *
     * @param executorService execution context for the internal async calls
     * @return CompletableFuture of the results.  If any of the validations failed, then
     * the result future will hold an exception of one of the failures.
     */
    protected CompletableFuture<Void> validateRevert(ExecutorService executorService) {
        CompletableFuture[] validations = new CompletableFuture[validatorList.size()];
        for (int i = 0; i < validatorList.size(); ++i) {
            validations[i] = validatorList.get(i).validateRevertAsync(executorService, this);
        }
        validationsReverted = true;
        return CompletableFuture.allOf(validations);
    }

    /**
     * Wait for the futures of all the validators to complete (or return exceptionally).
     *
     * @param executorService execution context for the internal async calls
     * @return CompletableFuture of the results.  If any of the validations failed, then
     * the result future will hold an exception of one of the failures.
     */
    protected CompletableFuture<Void> validateCleanup(ExecutorService executorService) {
        if (validationsExecuted && !validationsReverted) {
            CompletableFuture[] validations = new CompletableFuture[validatorList.size()];
            for (int i = 0; i < validatorList.size(); ++i) {
                validations[i] = validatorList.get(i).validateCleanupAsync(executorService, this);
            }
            validationsReverted = true;
            return CompletableFuture.allOf(validations);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
