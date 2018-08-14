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
     * Return an operation which will validate an aspect of the request, or null if
     * this validator is not applicable.
     * <p>
     * The provisioning operation will wait for the returned operation to complete, or
     * pass along the exception if there was a failure.
     *
     * @param executorService An executor for running validations on other threads
     * @param initiatingOp The composition service provisioning request
     * @return an operation that will validate an aspect of request or null if this
     * validator is not applicable to the request.  The result of the future should
     * be the number of components in the request which were validated.
     */
    CompletableFuture<Void> validateExecutionAsync(ExecutorService executorService, Operation initiatingOp);

    /**
     * Return an operation which will validate an aspect of the destroy request, or null if
     * this validator is not applicable.
     * <p>
     * The destroy operation will wait for the returned operation to complete, or
     * pass along the exception if there was a failure.
     *
     * @param executorService An executor for running validations on other threads
     * @param initiatingOp The composition service provisioning request
     * @return an operation that will validate an aspect of destroy request or null if this
     * validator is not applicable to the request.   The result of the future should
     * be the number of components in the request which were validated.
     */
    CompletableFuture<Void> validateRevertAsync(ExecutorService executorService, Operation initiatingOp);
}
