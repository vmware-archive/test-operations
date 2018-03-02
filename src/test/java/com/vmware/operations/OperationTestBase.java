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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;

/**
 * Base class for writing test cases based on operations.
 * <p/>
 * The base class maintains two collections of operations -- the first
 * is for setup (using the @BeforeClass annotation in TestNG), and the
 * other is for running individual tests.
 * <p/>
 * The base class implements cleanup methods that revert any
 * executed operations at the appropriate times.
 */
public class OperationTestBase {
    private static final Logger logger = LoggerFactory.getLogger(OperationTestBase.class.getPackage().getName());

    protected OperationSequence fixtureOps = Operations.sequence();

    /**
     * Method called by TestNG after each test class.  We pass
     * the alwaysRun flag so that it will be called regardless of
     * the pass/fail status of the tests.
     */
    @AfterClass
    public void teardown() {
        logger.info("Cleaning fixture");
        fixtureOps.cleanup();
        fixtureOps = null;
    }

    /**
     * Run a command immediately, but track it for later cleanup.
     *
     * @param op the command to run
     */
    public void fixtureExecute(Operation op) {
        try {
            fixtureOps.addExecute(op);
        } catch (Throwable thrown) {
            throw new AssertionError("Failure during setup of the test", thrown);
        }
    }
}
