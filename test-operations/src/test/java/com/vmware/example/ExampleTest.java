/*
 * Copyright (c) 2018 VMware, Inc. All Rights Reserved.
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

package com.vmware.example;

import org.testng.annotations.Test;

import com.vmware.operations.Operation;
import com.vmware.operations.OperationList;
import com.vmware.operations.OperationSequence;
import com.vmware.operations.Operations;
import com.vmware.operations.utils.FuzzUtils;

/**
 * Some example tests to show how to use operations.
 * <p>
 * These examples use folders and files as resources that need to
 * be cleaned up, but with your own operations, you can create
 * any types of resources, like Users, VMs, containers, etc.
 * <p>
 * In our example, we chose that revert is "picky" and will fail
 * if there are errant files in the folder.  Cleanup is not picky
 * and will do everything it can to clean up the test.
 */
public class ExampleTest {

    private String fuzzedName(String prefix) {
        return FuzzUtils.getRandomizedString("folder", FuzzUtils.BASIC_CHARS, 2, 0);
    }

    /**
     * This test just verifies that you can use operations like
     * any AutoCloseable subclass.  This isn't the preferred way
     * to use them, but comes in handy for simple tasks.
     *
     * @throws Exception
     */
    @Test
    public void closeableOperationTest() throws Exception {
        try (CreateFileOp local = new CreateFileOp("foo.txt")) {
            local.execute();
            local.revert();
        }
    }

    /**
     * This test shows how sequences work.  A folder is created
     * first, then a file is created within it.
     * <p>
     * When the sequence is closed, the operations are reverted
     * in reverse order.
     *
     * @throws Exception
     */
    @Test
    public void basicFolderTest() throws Throwable {
        try (OperationSequence ops = Operations.sequence()) {
            CreateFolderOp folderOp = new CreateFolderOp(fuzzedName("folder"));
            ops.addExecute(folderOp);

            Operation fileOp = new CreateFileOp(folderOp, fuzzedName("file"));
            ops.addExecute(fileOp);
        }
    }

    /**
     * This test shows parallel operations.  A folder is created,
     * then ten files are created in parallel within in.
     * <p>
     * Afterwards the whole thing is cleaned up, also in parallel.
     */
    @Test
    public void parallelTest() throws Throwable {
        try (OperationSequence ops = Operations.sequence()) {
            CreateFolderOp folderOp = new CreateFolderOp(fuzzedName("folder"));
            ops.addExecute(folderOp);

            // Create a list which will hold the parallel operations
            OperationList list = Operations.list();

            // File in the list with 10 different file creations
            for (int i = 0; i < 10; ++i) {
                Operation fileOp = new CreateFileOp(folderOp, fuzzedName("file" + i));
                list.add(fileOp);
            }

            // Execute the list in parallel, and add it to the master
            // sequence.
            ops.addExecute(list);
        }

    }

    /**
     * Things don't always go so well.  We will create a folder, and leave an
     * file in it, so that it cannot be deleted.
     * <p>
     * As long as we delete the file before the end of the test, the cleanup
     * will try again and the folder will be deleted.
     */
    @Test
    public void failFolderTest() {

    }

    /**
     * A final test to demonstrate parallelism and scripting.  We will simultaneously
     * create ten folders, each containing another folder containing a file.
     * <p>
     * When it's all done, we will delete them in parallel.
     *
     * @throws Exception
     */

    @Test
    public void scriptedTest() throws Exception {
        try (Operation ops = Operations.list()) {
            for (int i = 0; i < 10; ++i) {
                OperationSequence seq = Operations.sequence();

                CreateFolderOp folderOp = new CreateFolderOp(fuzzedName("folder" + i));
                seq.add(folderOp);

                CreateFolderOp nestedOp = new CreateFolderOp(folderOp, fuzzedName("folder"));
                seq.add(nestedOp);

                Operation fileOp = new CreateFileOp(nestedOp, fuzzedName("file"));
                seq.add(fileOp);
            }

            // Create all the folders and files in parallel
            ops.execute();

            // Clean them up in parallel
            ops.revert();
        }
    }
}
