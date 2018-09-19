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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vmware.operations.OperationSyncBase;
import com.vmware.operations.Operations;

public class CreateFileOp extends OperationSyncBase {

    private final CreateFolderOp folderOp;
    private final String fileName;
    private File file;
    private Writer writer;

    /**
     * Basic constructor.  This will make a file in an
     * undetermined place (actually the user's home folder)
     *
     * @param fileName
     */
    public CreateFileOp(String fileName) {
        super(Operations.getExecutorService());
        this.folderOp = null;
        this.fileName = fileName;
    }

    /**
     * Nested file constructor.  This will create a
     * file with the given name in a specific folder.
     * <p>
     * Rather than taking the path of the folder, we take
     * the folder operation, so that the created folder
     * path can be retrieved at execution time.
     *
     * @param folderOp
     * @param fileName
     */
    public CreateFileOp(CreateFolderOp folderOp, String fileName) {
        super(Operations.getExecutorService());
        this.folderOp = folderOp;
        this.fileName = fileName;
    }

    /**
     * Synchronous implementation of the operation.
     */
    @Override
    public void executeImpl() throws Exception {

        Path basePath;
        if (folderOp != null) {
            basePath = folderOp.getPath();
        } else {
            basePath = Paths.get(System.getProperty("user.home"));
        }

        file = basePath.resolve(fileName).toFile();

        writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "utf-8"));
    }

    /**
     * Synchronous revert implementation of the operation.
     */
    @Override
    public void revertImpl() throws Exception {
        writer.close();
        writer = null;

        file.delete();
        file = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + fileName + ")";
    }

    /**
     * @return the status of the command.
     */
    @Override
    public boolean isExecuted() {
        return writer != null;
    }
}
