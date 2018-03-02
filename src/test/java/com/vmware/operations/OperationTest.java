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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Tests the Operations classes, ensuring that they behave correctly.
 */
public class OperationTest {
    private final static Logger logger = LoggerFactory.getLogger(OperationTest.class);

    /**
     * Test method for {@link com.vmware.operations.Operation}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public final void testSequenceBasic() throws Exception {
        final long delayMillis = 100;
        OperationSequence seq = Operations.sequence();

        Assert.assertTrue(seq.isEmpty());
        Assert.assertEquals(0, seq.size());

        AtomicInteger value = new AtomicInteger(0);
        seq.add(new IncrementOperation(value, delayMillis));   // 0 + 1 = 1
        Assert.assertEquals(1, seq.size());

        seq.add(new MultiplyOperation(value, 7, delayMillis));   // 1 * 7 = 7
        Assert.assertEquals(2, seq.size());

        seq.add(new IncrementOperation(value, delayMillis));   // 7 + 1 = 8
        Assert.assertEquals(3, seq.size());

        seq.add(new MultiplyOperation(value, 13, delayMillis)); // 8 * 13 = 104
        Assert.assertEquals(4, seq.size());

        long startTime, executionTime;

        startTime = System.nanoTime();
        seq.execute();
        executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + 1;
        logger.info("Execution time (serial): {} ms", executionTime);

        Assert.assertEquals(104, value.get());
        Assert.assertTrue("Execution time was quicker than expected: " + executionTime,
                executionTime >= seq.size() * delayMillis);

        startTime = System.nanoTime();
        seq.revert();
        executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime + TimeUnit.MILLISECONDS.toNanos(1) / 2);
        Assert.assertEquals(0, value.get());
        Assert.assertTrue("Revert time was quicker than expected: " + executionTime,
                executionTime >= seq.size() * delayMillis);

        seq.close();
    }

    /**
     * Test method for {@link com.vmware.operations.OperationList}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public final void testParallelBasic() throws Exception {
        final long delayMillis = 100;
        OperationCollection list = Operations.list();

        Assert.assertTrue(list.isEmpty());
        Assert.assertEquals(0, list.size());

        AtomicInteger value = new AtomicInteger(0);
        list.add(new IncrementOperation(value, delayMillis));
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(1, list.getOperations().size());

        list.add(new IncrementOperation(value, delayMillis));
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(2, list.getOperations().size());

        list.add(new IncrementOperation(value, delayMillis));
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(3, list.getOperations().size());

        list.add(new IncrementOperation(value, delayMillis));
        Assert.assertEquals(4, list.size());
        Assert.assertEquals(4, list.getOperations().size());

        long startTime, executionTime;

        startTime = System.nanoTime();
        list.execute();
        executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        logger.info("Execution time (parallel): {} ms", executionTime);

        Assert.assertTrue(executionTime < list.size() * delayMillis);
        Assert.assertEquals(4, value.get());

        startTime = System.nanoTime();
        list.revert();
        executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        logger.info("Revert time (parallel): {} ms", executionTime);

        Assert.assertEquals(0, value.get());
        Assert.assertTrue("Execution time was longer than expected: " + executionTime,
                executionTime < list.size() * delayMillis);

        list.close();
    }

    /**
     * Test method for {@link com.vmware.operations.OperationSequence}.
     */
    @Test
    public final void testData() {
        try (OperationSequence seq = Operations.sequence()) {
            AtomicInteger value = new AtomicInteger(0);
            seq.add(new IncrementOperation(value));   // 0 + 1 = 1
            Assert.assertEquals(1, seq.size());
            Assert.assertEquals(1, seq.getOperations().size());

            // test data is writable
            // finalize
            // test data is not writable
        }
    }

    /**
     * Test method for {@link com.vmware.operations.OperationSequence}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public final void testSequenceScale() throws Exception {
        AtomicInteger value = new AtomicInteger(1);

        try (OperationSequence seq = Operations.sequence()) {
            Assert.assertTrue(seq.isEmpty());
            Assert.assertEquals(0, seq.size());

            final int scaleFactor = 10000;
            for (int i = 0; i < scaleFactor / 4; ++i) {
                seq.add(new MultiplyOperation(value, -3));   // 1 * -3 = -3
                seq.add(new IncrementOperation(value));      // -3 + 1 = -2
                seq.add(new IncrementOperation(value));      // -2 + 1 = -1
                seq.add(new MultiplyOperation(value, -1));   // -1 * -1 = 1
            }
            Assert.assertEquals(scaleFactor, seq.size());

            seq.execute();
            Assert.assertEquals(1, value.get());

            seq.revert();
            Assert.assertEquals(1, value.get());
        }

        Assert.assertEquals(1, value.get());
    }

    /**
     * Test method for {@link com.vmware.operations.OperationList}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public final void testParallelScale() throws Exception {
        AtomicInteger value = new AtomicInteger(0);

        try (OperationCollection seq = Operations.list()) {
            Assert.assertTrue(seq.isEmpty());
            Assert.assertEquals(0, seq.size());

            final int scaleFactor = 10000;
            for (int i = 0; i < scaleFactor; ++i) {
                seq.add(new IncrementOperation(value));
            }
            Assert.assertEquals(scaleFactor, seq.size());

            seq.execute();
            Assert.assertEquals(scaleFactor, value.get());

            seq.revert();
            Assert.assertEquals(0, value.get());
        }

        Assert.assertEquals(0, value.get());
    }
}
