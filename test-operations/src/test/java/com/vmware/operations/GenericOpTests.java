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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Tests the Operations classes, ensuring that they behave correctly.
 */
public class GenericOpTests {
    private final static Logger logger = LoggerFactory.getLogger(GenericOpTests.class);

    @Test
    public void empty() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>(new AtomicInteger(1));

        op.execute();
        op.revert();

        Assert.assertEquals(1, op.getData().get());
    }

    @Test
    public void named() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>("name", new AtomicInteger(1));

        op.execute();
        op.revert();

        Assert.assertEquals(1, op.getData().get());
    }

    @Test
    public void basicTest() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>(new AtomicInteger(1));

        op.addExecuteFunction(v -> v.getAndUpdate(x -> x * 31));
        op.addExecuteFunction(v -> v.getAndIncrement());

        op.addRevertFunction(v -> v.getAndUpdate(x -> x * 7));
        op.addRevertFunction(v -> v.getAndDecrement());

        op.execute();

        // If done in the correct order, the answer is (1 * 31) + 1 = 32
        Assert.assertEquals(32, op.getData().get());

        op.revert();

        // If done in the correct order, the answer is (32 - 1) * 7 = 217
        Assert.assertEquals(217, op.getData().get());
    }

    @Test
    public void executeFailureTest() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>(new AtomicInteger(1));

        op.addExecuteFunction(v -> v.getAndUpdate(x -> x * 31));
        op.addExecuteFunction(v -> { throw new IllegalArgumentException(); });
        op.addExecuteFunction(v -> v.getAndIncrement());

        op.addRevertFunction(v -> v.getAndUpdate(x -> x * 7));

        try {
            op.execute();
            throw new AssertionError("Exception not thrown");
        } catch (IllegalArgumentException ia) {
            // expected exception
        }

        // If done in the correct order, the answer is (1 * 31) + 1 = 32
        Assert.assertEquals(31, op.getData().get());

        op.cleanup();

        // Revert functions should not have been called
        Assert.assertEquals(31, op.getData().get());
    }

    @Test
    public void revertFailureTest() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>(new AtomicInteger(1));

        op.addExecuteFunction(v -> v.getAndUpdate(x -> x * 31));
        op.addExecuteFunction(v -> v.getAndIncrement());

        op.addRevertFunction(v -> v.getAndUpdate(x -> x * 7));
        op.addRevertFunction(v -> { throw new IllegalArgumentException(); });
        op.addRevertFunction(v -> v.getAndDecrement());

        op.execute();

        // If done in the correct order, the answer is (1 * 31) + 1 = 32
        Assert.assertEquals(32, op.getData().get());

        try {
            op.revert();
            throw new AssertionError("Exception not thrown");
        } catch (IllegalArgumentException ia) {
            // expected exception
        }

        // If done in the correct order, the answer is (32 - 1) = 31
        Assert.assertEquals(31, op.getData().get());

        op.cleanup();

        // Cleanup should get the masked revert function, the answer is (32 - 1) = 217
        Assert.assertEquals(217, op.getData().get());
    }

    @Test
    public void cleanupFailureTest() throws Throwable {
        GenericOp<AtomicInteger> op = new GenericOp<>(new AtomicInteger(1));

        op.addExecuteFunction(v -> v.getAndUpdate(x -> x * 31));
        op.addExecuteFunction(v -> v.getAndIncrement());

        op.addRevertFunction(v -> v.getAndUpdate(x -> x * 7));
        op.addRevertFunction(v -> { throw new IllegalArgumentException(); });
        op.addRevertFunction(v -> v.getAndDecrement());

        op.execute();

        // If done in the correct order, the answer is (1 * 31) + 1 = 32
        Assert.assertEquals(32, op.getData().get());

        op.cleanup();

        // All functions will be attempted
        Assert.assertEquals(217, op.getData().get());
    }
}
