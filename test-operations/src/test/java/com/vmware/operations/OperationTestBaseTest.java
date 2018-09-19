/*
 * Copyright (c) 2016-2018 VMware, Inc. All Rights Reserved.
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the OperationTestBase class, ensuring that BeforeClass and AfterClass
 * methods work correctly.
 */
public class OperationTestBaseTest extends OperationTestBase {
    private final static Logger logger = LoggerFactory.getLogger(OperationTestBaseTest.class);

    private AtomicInteger classCounter = new AtomicInteger(0);

    @BeforeClass
    public void fixtureSetup() {
        Operation op = new IncrementOperation(classCounter);
        fixtureExecute(op);
    }

    @Test
    public void testFixtureRan() {
        Assert.assertEquals(1, classCounter.get());
    }
}
