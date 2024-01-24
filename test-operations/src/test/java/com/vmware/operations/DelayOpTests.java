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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Tests the Operations classes, ensuring that they behave correctly.
 */
public class DelayOpTests {
    private final static Logger logger = LoggerFactory.getLogger(DelayOpTests.class);

    @Test
    public void defaultConstructor() throws Throwable {
        DelayOp op = new DelayOp();

        long startTime = System.nanoTime();
        op.execute();
        op.revert();

        long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + 1;
        logger.info("Execution time: {} ms", executionTime);

        Assert.assertTrue("Execution time was slower than expected: " + executionTime,
                executionTime < 100);
    }

    /**
     * Test method for {@link Operation}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public void alternateConstructor() throws Throwable {
        DelayOp op = new DelayOp(Duration.ofSeconds(1));

        long startTime = System.nanoTime();
        op.execute();
        op.revert();

        long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + 1;
        logger.info("Execution time: {} ms", executionTime);

        Assert.assertTrue("Execution time was faster than expected: " + executionTime,
                executionTime >= 1000);
    }

    /**
     * Test method for {@link Operation}.
     *
     * @throws Exception for unexpected exceptions
     */
    @Test
    public final void delayMutators() throws Throwable {
        DelayOp op = new DelayOp()
                .executeDelay(Duration.ofSeconds(1))
                .revertDelay(Duration.ofSeconds(1));

        long startTime = System.nanoTime();
        op.execute();

        long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + 1;
        logger.info("Execution time: {} ms", executionTime);

        Assert.assertTrue("Execution time was faster than expected: " + executionTime,
                executionTime >= 1000);

        op.revert();

        executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + 1;
        logger.info("Execution time: {} ms", executionTime);

        Assert.assertTrue("Execution time was faster than expected: " + executionTime,
                executionTime >= 2000);
    }

    /**
     * Test method for {@link Operation}.
     */
    @Test
    public final void negativeConstructorDelay() {
        DelayOp op = new DelayOp(Duration.ofSeconds(-1));
        Assert.assertEquals("Execution Delay was not constrained to bounds",
                Duration.ZERO, op.getExecuteDelay());
    }

    /**
     * Test method for {@link Operation}.
     */
    @Test
    public final void negativeExecutorDelay() {
        DelayOp op = new DelayOp()
                .executeDelay(Duration.ofSeconds(-1));
        Assert.assertEquals("Execution Delay was not constrained to bounds",
                Duration.ZERO, op.getExecuteDelay());
    }

    /**
     * Test method for {@link OperationList}.
     */
    @Test
    public final void negativeRevertDelay() {
        DelayOp op = new DelayOp()
                .revertDelay(Duration.ofSeconds(-1));
        Assert.assertEquals("Revert Delay was not constrained to bounds",
                Duration.ZERO, op.getRevertDelay());
    }

    /**
     * Test method for {@link Operation}.
     */
    @Test
    public final void extremeConstructorDelay() {
        DelayOp op = new DelayOp(Duration.ofDays(1));
        Assert.assertNotEquals("Execution Delay was not constrained to bounds",
                Duration.ofDays(1), op.getExecuteDelay());
    }

    /**
     * Test method for {@link Operation}.
     */
    @Test
    public final void extremeExecutorDelay() {
        DelayOp op = new DelayOp()
                .executeDelay(Duration.ofDays(1));
        Assert.assertNotEquals("Execution Delay was not constrained to bounds",
                Duration.ofDays(1), op.getExecuteDelay());
    }

    /**
     * Test method for {@link OperationList}.
     */
    @Test
    public final void extremeRevertDelay() {
        DelayOp op = new DelayOp()
                .revertDelay(Duration.ofDays(1));
        Assert.assertNotEquals("Revert Delay was not constrained to bounds",
                Duration.ofDays(1), op.getRevertDelay());
    }
}
