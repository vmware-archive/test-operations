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
import java.util.function.Predicate;

/**
 * Operations are an extension of the Command pattern that
 * can exist in one of two states -- executed or not.
 * <p>
 * When created, operations are not executed.  Operations can be executed
 * by calling execute(), and "un-executed" by calling revert().
 * <p>
 * Validators may be added (or removed) before the operation
 * is executed.  Validations will happen after the operation is
 * executed or reverted, and failed validations will result
 * in exceptions.
 * <p>
 * Validations are not called during cleanup.
 * <p>
 * In general, reverted operations can be re-performed by calling execute()
 * a second time.
 * <p>
 * Operations are autocloseable.  On cleanup/close, operations will be reverted, but
 * errors will be suppressed unless fatal.
 */
public interface Operation extends AutoCloseable {
    /**
     * Add a validator to this operation.  This can be added by the operation
     * itself in the constructor, or added later by the caller.
     *
     * @param validator instance of a validator
     */
    void addValidator(Validator validator);

    /**
     * Remove a validator if present.  This can be added by the operation
     * itself in the constructor, or added later by the caller.
     *
     * @param clazz class type of a validator
     * @return true if a validator was removed
     */
    boolean removeValidator(Class<?> clazz);

    /**
     * Remove a validator if present.  This can be added by the operation
     * itself in the constructor, or added later by the caller.
     *
     * @param predicate function called for each validator
     * @return true if a validator was removed
     */
    boolean removeValidator(Predicate<Validator> predicate);

    /**
     * Add a validator to this operation.  This can be added by the operation
     * itself in the constructor, or added later by the caller.
     */
    void removeAllValidators();

    /**
     * Perform the command.
     * Throws an error on failure.
     * When the execution has completed, isExecuted() will return true.
     *
     * @throws Throwable if the operation fails
     */
    void execute() throws Throwable;

    /**
     * Perform the command.
     * Throws an error on failure.
     * When the execution has completed, isExecuted() will return true.
     * @return A future that completes when the command is complete
     */
    CompletableFuture<Void> executeAsync();

    /**
     * "Un-perform" a command.
     * Throws an IllegalStateException on failure.
     * When the revert has completed, isExecuted() will return false.
     *
     * @throws Throwable if the revert operation fails
     */
    void revert() throws Throwable;

    /**
     * "Un-perform" a command.
     * Throws an IllegalStateException on failure.
     * When the revert has completed, isExecuted() will return false.
     * @return A future that completes when the command is reverted
     */
    CompletableFuture<Void> revertAsync();

    /**
     * Get the status of the operation.
     * @return true if the operation has executed, and therefore needs
     *          to be reverted or cleaned up.
     */
    boolean isExecuted();

    /**
     * Gracefully revert the command, and cleanup.
     * It is assumed that the test is over, so the cleanup can
     * be destructive and quick.
     * Commands cannot be re-executed after close().
     */
    void cleanup();

    /**
     * Gracefully revert the command, and cleanup.
     * It is assumed that the test is over, so the cleanup can
     * be destructive and quick.
     * Commands cannot be re-executed after close().
     * @return A future that completes when the command is cleaned
     */
    CompletableFuture<Void> cleanupAsync();

    /**
     * Synchronously cleanup the list and release the resources.
     */
    void close();
}
