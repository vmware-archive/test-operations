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

import java.util.List;

/**
 * Interface to collections of operations that execute
 * together.
 */
public interface OperationCollection extends Operation {

    /**
     * Add an operation to the end of the list.  It will be executed
     * when the OperationCollection is executed.
     *
     * @param op the operation to add
     */
    void add(Operation op);

    /**
     * Return true if no child operations have been added.
     * @return true if no child operations have been added.
     */
    boolean isEmpty();

    /**
     * Return the number of chile operations in the collection.
     * @return the number of chile operations in the collection.
     */
    int size();

    /**
     * The list of child operations as a List.
     * @return an unmodifiable, ordered List of operations
     */
    List<Operation> getOperations();

    /**
     * After the list is finished, it cannot be changed.  Calling
     * execute() or cleanup() will automatically finish the list.
     *
     * finish() can be called multiple times.
     */
    void finish();
}
