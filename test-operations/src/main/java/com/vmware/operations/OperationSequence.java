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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform a collection of operations in sequence, waiting for
 * each one to complete before starting the next.
 */
public class OperationSequence extends OperationSyncBase implements OperationCollection {
    private static final Logger logger = LoggerFactory.getLogger(OperationSequence.class.getPackage().getName());

    private boolean isExecuted = false;
    private List<Operation> data = new ArrayList<>();
    private List<Operation> revdata = null;

    /**
     * Constructor.
     * @param executorService An executor for running validations on other threads
     */
    public OperationSequence(ExecutorService executorService) {
        super(executorService);
    }

    /**
     * Add a command to the list.
     *
     * @param cmd the command to run
     */
    public void add(Operation cmd) {
        if (cmd != null) {
            data.add(cmd);
        }
    }

    /**
     * Add a command to the list, and execute it immediately.
     * This ensures that it will be cleaned up when this list is reverted/cleaned up.
     *
     * @param cmd the command to run
     * @throws Throwable if the execution cannot be completed
     */
    public void addExecute(Operation cmd) throws Throwable {
        if (cmd != null) {
            data.add(cmd);
            cmd.execute();
        }
    }

    /**
     * @return true if no commands have been added
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * @return the number of commands added
     */
    public int size() {
        return data.size();
    }

    /*
     * Suppress Spotbugs warning because we make the list immutable at
     * finish() time.
     */
    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<Operation> getOperations() {
        return data;
    }

    /**
     * After the list is finished, it cannot be changed.  Calling
     * execute() will automatically finish the list.
     *
     * The implementation also creates a reversed copy of the list, since
     * it is guaranteed to be used at least once.
     *
     * finish() is idempotent, and can be called multiple times.
     */
    public void finish() {
        if (revdata == null) {
            data = Collections.unmodifiableList(data);

            List<Operation> clone = new ArrayList<>(data);
            Collections.reverse(clone);
            revdata = Collections.unmodifiableList(clone);
        }

        // If there are operations, and all have been executed, then we are executed too.
        if (!data.isEmpty()) {
            for (Operation o : data) {
                if (!o.isExecuted()) {
                    return;
                }
            }
            isExecuted = true;
        }
    }

    /**
     * Perform a list of commands in sequence, waiting for
     * each one to complete before starting the next.
     */
    @Override
    public void executeImpl() throws Throwable {
        finish();

        logger.info("Executing {}", toString());
        throwIfExecuted();

        for (Operation cmd : data) {
            cmd.execute();
        }
        this.isExecuted = true;
    }

    /**
     * Revert the list of commands in reverse order of
     * execution.
     *
     * If any of the commands are marked as not isExecuted(),
     * they will be silently skipped.  This allows for easy
     * reverting of a test, when one or more of the individual
     * operations where undone explicitly.
     */
    @Override
    public void revertImpl() throws Throwable {
        finish();

        logger.info("Reverting {}", toString());
        throwIfNotExecuted();

        // Create a new trace entry for each operation
        for (Operation cmd : revdata) {
            cmd.revert();
        }
        this.isExecuted = false;
    }

    /**
     * Returns true only if all of the commands in the list
     * have been executed.
     *
     * Note that partially executed lists (which can happen if one of the
     * commands threw an exception) still return false.
     */
    @Override
    public boolean isExecuted() {
        return this.isExecuted;
    }

    @Override
    public void cleanup() {
        finish();

        // Log to establish the current token
        logger.info("Cleaning {}", toString());

        for (Operation cmd : revdata) {
            try {
                cmd.cleanup();
            } catch (Throwable throwable) {
                handleCleanupException(throwable);
            }
        }

        this.isExecuted = false;
        this.data = null;
        this.revdata = null;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();
        message.append(getClass().getSimpleName());
        message.append("(@");
        message.append(Integer.toHexString(System.identityHashCode(this)));

        if (data != null) {
            message.append(", ");
            message.append(data.size());
            message.append(" items");
        }

        message.append(")");
        return message.toString();
    }
}
