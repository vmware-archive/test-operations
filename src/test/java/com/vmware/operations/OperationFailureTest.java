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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Tests the Operation classes, ensuring that they behave correctly.
 * <p/>
 * These tests ensure that error and exception handling is performing properly.
 */
public class OperationFailureTest extends OperationTestBase {

    class ExecuteFailure extends IncrementOperation {
        ExecuteFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public void executeImpl() {
            throw new ArithmeticException("execute");
        }
    }

    class RevertFailure extends IncrementOperation {
        RevertFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public void revertImpl() {
            throw new ArithmeticException("revert");
        }
    }

    class CleanupFailure extends IncrementOperation {
        CleanupFailure(AtomicInteger data) {
            super(data);
        }

        @Override
        public void cleanup() {
            throw new ArithmeticException("cleanup");
        }
    }

    /**
     * Verify that failures in the execute() method are handled properly.
     */
    @Test
    public final void testBasicExecuteFailure() {
        AtomicInteger value = new AtomicInteger(0);
        Operation cmd = new ExecuteFailure(value);

        try {
            cmd.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, value.get());

        try {
            cmd.revert();
            Assert.fail("Exception was not thrown");
        } catch (IllegalStateException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, value.get());

        cmd.close();

        Assert.assertEquals(0, value.get());
    }

    /**
     * Verify that failures in the revert() method are handled properly.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testBasicRevertFailure() throws Exception {
        AtomicInteger value = new AtomicInteger(0);
        Operation cmd = new RevertFailure(value);

        cmd.execute();
        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, value.get());

        try {
            cmd.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, value.get());

        cmd.close();

        Assert.assertEquals(1, value.get());
    }

    /**
     * Test Cleanup failure for completeness.  Note that cleanup() should never throw
     * if possible.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testBasicCleanupFailure() throws Exception {
        AtomicInteger value = new AtomicInteger(0);
        Operation cmd = new CleanupFailure(value);

        cmd.execute();
        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, value.get());

        try {
            cmd.close();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertEquals(1, value.get());
    }

    /**
     * Verify that failures in the execute() method are handled properly.
     */
    @Test
    public final void testDoubleExecuteFailure() throws Exception {
        AtomicInteger value = new AtomicInteger(0);
        Operation cmd = new IncrementOperation(value);

        // First one should be successful
        cmd.execute();
        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, value.get());

        // Second should fail
        try {
            cmd.execute();
            Assert.fail("Exception was not thrown");
        } catch (IllegalStateException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertTrue(cmd.isExecuted());
        Assert.assertEquals(1, value.get());

        // Close should work normally
        cmd.close();

        Assert.assertFalse(cmd.isExecuted());
        Assert.assertEquals(0, value.get());
    }

    /**
     * Test that sequences properly handle exceptions in the execution stage
     * This means that they don't resolve to isExecuted state, but are
     * still capable of cleaning up.
     */
    @Test
    public final void testSequenceExecuteFailure() {
        OperationSequence seq = Operations.sequence();

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value));
        seq.add(new ExecuteFailure(value));
        seq.add(new IncrementOperation(value));

        try {
            seq.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(seq.isExecuted());
        Assert.assertEquals(1, value.get());

        try {
            seq.revert();
            Assert.fail("Exception was not thrown");
        } catch (IllegalStateException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        seq.close();
        Assert.assertEquals(0, value.get());
    }

    /**
     * Test that sequences properly handle exceptions in the revert stage
     * This means that they don't resolve to isExecuted state as false, but are
     * still capable of cleaning up.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testSequenceRevertFailure() throws Exception {
        OperationSequence seq = Operations.sequence();

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value));
        seq.add(new RevertFailure(value));
        seq.add(new IncrementOperation(value));

        seq.execute();

        Assert.assertTrue(seq.isExecuted());
        Assert.assertEquals(3, value.get());

        try {
            seq.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertTrue(seq.isExecuted());
        Assert.assertEquals(2, value.get());

        seq.close();
    }

    /**
     * Test that sequences properly handle exceptions in the cleanup stage
     * This means that they clean up as cleanly as possible, but don't
     * throw errors.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testSequenceCleanupFailure() throws Exception {
        OperationSequence seq = Operations.sequence();

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value));
        seq.add(new CleanupFailure(value));
        seq.add(new IncrementOperation(value));

        seq.execute();

        Assert.assertTrue(seq.isExecuted());
        Assert.assertEquals(3, value.get());

        seq.close();

        // All possible values should be restored
        Assert.assertEquals(1, value.get());
    }

    /**
     * Test that parallel lists properly handle exceptions in the execution stage
     * This means that they don't resolve to isExecuted state, but are
     * still capable of cleaning up.
     */
    @Test
    public final void testParallelExecuteFailure() {
        OperationList seq = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value));
        seq.add(new ExecuteFailure(value));
        seq.add(new IncrementOperation(value));

        try {
            seq.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(seq.isExecuted());

        try {
            seq.revert();
            Assert.fail("Exception was not thrown");
        } catch (IllegalStateException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        seq.close();

        // Original value should be restored
        Assert.assertEquals(0, value.get());
    }

    /**
     * Test that parallel sequences properly handle exceptions in the revert stage
     * This means that they don't resolve to isExecuted state as false, but are
     * still capable of cleaning up.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testParallelRevertFailure() throws Exception {
        OperationList list = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        list.add(new IncrementOperation(value));
        list.add(new RevertFailure(value));
        list.add(new IncrementOperation(value));

        list.execute();
        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(3, value.get());

        try {
            list.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // This is the expected case
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }
        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(1, value.get());

        list.close();

        // Nothing will clean up that last one
        Assert.assertEquals(1, value.get());
    }

    /**
     * Test that parallel lists properly handle exceptions in the cleanup stage
     * This means that they clean up as cleanly as possible, but don't
     * throw errors.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testParallelCleanupFailure() throws Exception {
        OperationList list = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        list.add(new IncrementOperation(value));
        list.add(new CleanupFailure(value));
        list.add(new IncrementOperation(value));

        list.execute();

        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(3, value.get());

        list.close();

        // All possible values should be restored
        Assert.assertEquals(1, value.get());
    }

    /**
     * Test that parallel lists properly handle exceptions in the execution stage
     * This means that they don't resolve to isExecuted state, but are
     * still capable of cleaning up.
     * This worst case scenario completes the failures before the successes.
     */
    @Test
    public final void testParallelExecuteFailureWorstCase() {
        OperationList seq = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value, 100));
        seq.add(new ExecuteFailure(value));
        seq.add(new IncrementOperation(value, 200));

        try {
            seq.execute();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        Assert.assertFalse(seq.isExecuted());

        try {
            seq.revert();
            Assert.fail("Exception was not thrown");
        } catch (IllegalStateException expected) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }

        seq.close();

        // Original value should be restored
        Assert.assertEquals(0, value.get());
    }

    /**
     * Test that parallel sequences properly handle exceptions in the revert stage
     * This means that they don't resolve to isExecuted state as false, but are
     * still capable of cleaning up.
     * This worst case scenario completes the failures before the successes.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testParallelRevertFailureWorstCase() throws Exception {
        OperationList list = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        list.add(new IncrementOperation(value, 100));
        list.add(new RevertFailure(value));
        list.add(new IncrementOperation(value, 200));

        list.execute();
        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(3, value.get());

        try {
            list.revert();
            Assert.fail("Exception was not thrown");
        } catch (ArithmeticException expected) {
            // This is the expected case
        } catch (Throwable t) {
            Assert.fail("Unexpected exception type thrown: " + t);
        }
        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(1, value.get());

        list.close();

        // Nothing will clean up that last one
        Assert.assertEquals(1, value.get());
    }

    /**
     * Test that parallel lists properly handle exceptions in the cleanup stage
     * This means that they clean up as cleanly as possible, but don't
     * throw errors.
     * This worst case scenario completes the failures before the successes.
     *
     * @throws Exception if an operation throws during execution
     */
    @Test
    public final void testParallelCleanupFailureWorstCase() throws Exception {
        OperationList list = Operations.list();

        AtomicInteger value = new AtomicInteger(0);
        list.add(new IncrementOperation(value, 100));
        list.add(new CleanupFailure(value));
        list.add(new IncrementOperation(value, 200));

        list.execute();

        Assert.assertTrue(list.isExecuted());
        Assert.assertEquals(3, value.get());

        list.close();

        // All possible values should be restored
        Assert.assertEquals(1, value.get());
    }
}
