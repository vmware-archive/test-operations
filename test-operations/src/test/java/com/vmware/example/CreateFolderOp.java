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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.operations.OperationSyncBase;
import com.vmware.operations.Operations;

/**
 * Operation to create a folder.
 * <p>
 * Folders are created empty and to successful revert they
 * need to be empty.
 * <p>
 * If there are files left in the directory at the end
 * of the test, they will be deleted along with the folder.
 */
public class CreateFolderOp extends OperationSyncBase {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final CreateFolderOp folderOp;
    private final String folderName;
    private Path path;

    /**
     * Basic constructor.  This will make a folder in an
     * undetermined place (actually the user's home folder)
     *
     * @param folderName
     */
    public CreateFolderOp(String folderName) {
        super(Operations.getExecutorService());
        this.folderOp = null;
        this.folderName = folderName;
    }

    /**
     * Nested folder constructor.  This will create a
     * folder with the given name in another folder.
     * <p>
     * Rather than taking the path of the folder, we take
     * the folder operation, so that the created folder
     * path can be retrieved at execution time.
     *
     * @param folderOp
     * @param folderName
     */
    public CreateFolderOp(CreateFolderOp folderOp, String folderName) {
        super(Operations.getExecutorService());
        this.folderOp = folderOp;
        this.folderName = folderName;
    }

    @Override
    public void executeImpl() throws Exception {
        Path basePath;
        if (folderOp != null) {
            basePath = folderOp.getPath();
        } else {
            basePath = Paths.get(System.getProperty("user.home"));
        }

        path = basePath.resolve(folderName);

        logger.info("Creating folder {}", path.toString());
        Files.createDirectories(path);
    }

    @Override
    public void revertImpl() throws Exception {
        Files.delete(path);
        path = null;
    }

    /**
     * Return the path of the created directory.
     *
     * @return Path
     * @throws if the operation is not in the successfully executed state
     */
    public Path getPath() {
        throwIfNotExecuted();
        return path;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + folderName + ")";
    }

    @Override
    public boolean isExecuted() {
        return path != null;
    }
}
