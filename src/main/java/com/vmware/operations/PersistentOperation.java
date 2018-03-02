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

/**
 * Persistent operations are a pattern used to create or find objects
 * in the system that can take a long time to create, or contain a
 * substantial amount of state that makes them inefficient to create
 * every time.
 */
public interface PersistentOperation {
    /**
     * Override the persistent behavior and delete the products when the operation is reverted or cleaned up.
     *
     * @param value true to delete the object instead of leaving it
     */
    void setDeleteOnRevert(boolean value);
}
