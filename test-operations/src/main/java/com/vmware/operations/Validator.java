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
 * Generic interface for validation after operations.
 */
public interface Validator {
    /**
     * Perform an action to validate the execution of an operation.
     *
     * This is called after the execute operation has been completed.  The initiating
     * operation will wait for the validator to complete, passing along the exception
     * if there was a failure.
     *
     * @param executorService An executor for running validations on other threads
     * @param initiatingOp The operation that is being validated
     * @return a future that completes when the validation is done or has an error.
     */
    CompletableFuture<?> validateExecutionAsync(ExecutorService executorService, Operation initiatingOp);

    /**
     * Perform an action to validate the revert of an operation.
     *
     * This is called after the revert operation has been completed.  The initiating
     * operation will wait for the validator to complete, passing along the exception
     * if there was a failure.
     *
     * @param executorService An executor for running validations on other threads
     * @param initiatingOp The operation that is being validated
     * @return a future that completes when the validation is done or has an error.
     */
    CompletableFuture<?> validateRevertAsync(ExecutorService executorService, Operation initiatingOp);

    /**
     * Perform an action to cleanup the validator in case the execution of
     * an operation succeeded, but revert of the operation failed (in that case validateRevert()
     * will not normally be called).
     *
     * Generally, nothing needs to be done, but for validators that keep state, they
     * can use this method to clean up.
     *
     * Note that if the *validator* fails during revert, validateCleanupAsync will not be called.
     *
     * The initiating operation will wait for the validator to complete.  Exceptions
     * during cleanup will be ignored.
     *
     * @param executorService An executor for running validations on other threads
     * @param initiatingOp The operation that is being validated
     * @return a future that completes when the cleanup is done or has an error.
     */
    CompletableFuture<?> validateCleanupAsync(ExecutorService executorService, Operation initiatingOp);
}
