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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Tests the Operation classes, ensuring that they behave correctly.
 * <p/>
 * These tests ensure that error and exception handling is performing properly.
 */
public class ValidatorFailureTest extends OperationTestBase {

    class SyncExecuteFailure extends IncrementSyncValidator {
        SyncExecuteFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public void validateExecution(Operation op) {
            throw new ArithmeticException("execute");
        }
    }

    class SyncRevertFailure extends IncrementSyncValidator {
        SyncRevertFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public void validateRevert(Operation op) {
            throw new ArithmeticException("revert");
        }
    }

    class AsyncExecuteFailure extends IncrementAsyncValidator {
        AsyncExecuteFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public CompletableFuture<Void> validateExecution(ExecutorService executorService, Operation op) {
            throw new ArithmeticException("execute");
        }
    }

    class AsyncRevertFailure extends IncrementAsyncValidator {
        AsyncRevertFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public CompletableFuture<Void> validateRevert(ExecutorService executorService, Operation op) {
            throw new ArithmeticException("revert");
        }
    }

    /**
     * Verify that failures in the execute() method are handled properly.
     */
    @Test
    public final void testSyncExecuteFailure() throws Exception {
        AtomicInteger opValue = new AtomicInteger(0);
        AtomicInteger validValue = new AtomicInteger(0);

        Operation cmd = new IncrementOperation(opValue);
        cmd.addValidator(new SyncExecuteFailure(validValue));

        try {
            cmd.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, opValue.get());
        Assert.assertEquals(0, validValue.get());

        // The revert command should execute without error.
        // The validator will also run, so we see the side-effect
        // value getting decremented.
        cmd.revert();

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(-1, validValue.get());

        cmd.close();

        Assert.assertEquals(0, opValue.get());
    }

    /**
     * Verify that failures in the revert() method are handled properly.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testSyncRevertFailure() throws Exception {
        AtomicInteger opValue = new AtomicInteger(0);
        AtomicInteger validValue = new AtomicInteger(0);

        Operation cmd = new IncrementOperation(opValue);
        cmd.addValidator(new SyncRevertFailure(validValue));

        cmd.execute();
        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, opValue.get());
        Assert.assertEquals(1, validValue.get());

        try {
            cmd.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(1, validValue.get());

        cmd.close();

        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(1, validValue.get());
    }

    /**
     * Verify that failures in the execute() method are handled properly.
     */
    @Test
    public final void testAsyncExecuteFailure() throws Exception {
        AtomicInteger opValue = new AtomicInteger(0);
        AtomicInteger validValue = new AtomicInteger(0);

        Operation cmd = new IncrementOperation(opValue);
        cmd.addValidator(new AsyncExecuteFailure(validValue));

        try {
            cmd.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, opValue.get());
        Assert.assertEquals(0, validValue.get());

        // The revert command should execute without error.
        // The validator will also run, so we see the side-effect
        // value getting decremented.
        cmd.revert();

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(-1, validValue.get());

        cmd.close();

        Assert.assertEquals(0, opValue.get());
    }

    /**
     * Verify that failures in the revert() method are handled properly.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testAsyncRevertFailure() throws Exception {
        AtomicInteger opValue = new AtomicInteger(0);
        AtomicInteger validValue = new AtomicInteger(0);

        Operation cmd = new IncrementOperation(opValue);
        cmd.addValidator(new AsyncRevertFailure(validValue));

        cmd.execute();
        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, opValue.get());
        Assert.assertEquals(1, validValue.get());

        try {
            cmd.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(1, validValue.get());

        cmd.close();

        Assert.assertEquals(0, opValue.get());
        Assert.assertEquals(1, validValue.get());
    }
}
