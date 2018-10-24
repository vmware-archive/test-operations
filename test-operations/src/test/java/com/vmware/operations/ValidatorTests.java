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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Tests for adding and removing validators
 */
public class ValidatorTests {

    /**
     * Validates basic validation
     */
    @Test
    public final void testOneSyncValidator() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementSyncValidator(validationCount));

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 1, validationCount.get());

        op.revert();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 1001, validationCount.get());
    }

    /**
     * Validates basic validation
     */
    @Test
    public final void testOneAsyncValidator() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementAsyncValidator(validationCount));

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 1, validationCount.get());

        op.revert();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 1001, validationCount.get());
    }

    /**
     * Validates cleanup aspect of validation
     */
    @Test
    public final void testOneSyncValidatorCleanup() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementSyncValidator(validationCount));

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 1, validationCount.get());

        op.close();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 1001, validationCount.get());
    }

    /**
     * Validates cleanup aspect of validation
     */
    @Test
    public final void testOneAsyncValidatorCleanup() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementAsyncValidator(validationCount));

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 1, validationCount.get());

        op.close();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 1001, validationCount.get());
    }

    /**
     * Validates removal of Validators by type
     */
    @Test
    public final void testClassRemoval() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementSyncValidator(validationCount));

        op.removeValidator(IncrementSyncValidator.class);

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());

        op.revert();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());
    }

    /**
     * Validates removal of Validators by predicate
     */
    @Test
    public final void testPredicateRemoval() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementSyncValidator(validationCount));

        op.removeValidator((v) -> IncrementSyncValidator.class.isInstance(v));

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());

        op.revert();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());
    }

    /**
     * Validates removal of all Validators
     */
    @Test
    public final void TestAllRemoval() throws Exception {
        AtomicInteger opCount = new AtomicInteger();
        AtomicInteger validationCount = new AtomicInteger();

        IncrementOperation op = new IncrementOperation(opCount);
        op.addValidator(new IncrementSyncValidator(validationCount));

        op.removeAllValidators();

        op.execute();
        Assert.assertEquals("operation count should match", 1, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());

        op.revert();
        Assert.assertEquals("operation count should match", 0, opCount.get());
        Assert.assertEquals("validation count should match", 0, validationCount.get());
    }
}
