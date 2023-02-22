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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Factory method for operations.
 * <p>
 * Rather than incur the cost of adding dependency injection to
 * a platform library, we allow users to explicitly set a
 * non-default ExecutorService.
 */
public final class Operations {
    private static ExecutorService executorService = ForkJoinPool.commonPool();

    private Operations() {
    }

    /**
     * Return the executor service to use when submitting asynchronous work.
     * Note that most calls are asynchronous in the library, so this will be used
     * heavily.
     * @return configured executor service
     */
    public static ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Set the executor service to use when submitting asynchronous work.
     * Note that most calls are asynchronous in the library, so this will be used
     * heavily.
     * @param executorService An executor used to submit work.
     */
    public static void setExecutorService(ExecutorService executorService) {
        Operations.executorService = executorService;
    }

    /**
     * Factory method for a list of parallel operations.
     * @return OperationList initialized with the default executor service.
     */
    public static OperationList list() {
        return new OperationList(executorService);
    }

    /**
     * Factory method for a list of sequential operations.
     * @return OperationSequence initialized with the default executor service.
     */
    public static OperationSequence sequence() {
        return new OperationSequence(executorService);
    }
}
