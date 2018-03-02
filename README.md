

# test-operations

[![Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Overview

An ```Operation``` is an adaptation of the [command pattern](https://en.wikipedia.org/wiki/Command_pattern) that knows how to revert executed commands, so that
artifacts are not left behind after the test is complete.

For testing, this is a extremely useful pattern.  This library provides a proven
and tested framework to use as a base for writing integration tests in your projects.


## Try it out

It's easy to get started adding operations to your Java project.  Just add to your ```<dependencies>``` section:

```xml
<dependency>
  <groupId>com.vmware</groupId>
  <artifactId>test-operations</artifactId>
  <version>1.0</version>
  <scope>test</scope>
</dependency>
```

Then implement your own sub-class of Operation.  Examples are in the project.

## Why do I want this?

Operations are a powerful pattern for building integration tests that need to clean up after themselves.  They
encourage well-written tests and have the following benefits:

### Independent, reusable blocks
Operations encourage tests to be designed as a collection of reusable blocks.  Each operation is responsible for implementing
one specific behavior, usually around an allocated or reserved resource.
All of the code around creating, deleting, and validation of that resource is kept in a single object.

Test scenarios are then built up from these composable blocks, and then behavior is confirmed.  Where helpful, the Operations can
also contain helper methods to write verifications or extract information.

### Cleaning up
Operations, like [Commands](https://en.wikipedia.org/wiki/Command_pattern), encapsulate code that should be executed to create or reserve a resource.

Additionally they can "revert" the creation or reservation, to put the environment in the state it was before the test ran.

Once the tests are complete (successful or not), the operations need to clean up any allocated or reserved resources.
Operations treat this as a special mode, and make the following decisions
1. cleanup must complete
2. cleanup is best effort and resources may not be released (because of #1), and
3. errors are suppressed (so as to not interfere with the actual test results)

### Composable
Groups of operations can be treated as operations themselves. 
 
The simplest composition is just a sequence of operations treated as a single 
operation.  This is the 
mechanism commonly used in every test, so that after its execution, the combined
sequence of operations is reverted or cleaned up together.

But we can also group operations in a parallel list.  Lists are executed concurrently
(using a ExecutorService instance), which makes testing on multiple threads for
concurrency extremely easy.

By combining the two, complex structures can be setup and torn down very quickly to
decrease the running times of tricky integration tests.  See
[OperationCollections](DOCUMENTATION.md#OperationCollections) for more information.

### Built-in validation
Operations understand validations as a first-class helper object.
By default, validations are run after the successful execution or reversion of an 
operation.

This encourages every operation to be validated in every test, without
test writers having to remember to include validations and assertions.

For special situations, the validation list is mutable -- default validations can be 
removed and additional validations can be added easily.

Validations are also encapsulated in individual classes, so new ones can be written
or they can be derived from existing tests.



For more details about the library and how to use it effectively, refer to
[DOCUMENTATION.md](DOCUMENTATION.md#Collections).

## Contributing

The test-operations project team welcomes contributions from the community. If you wish to contribute code and you have not
signed our contributor license agreement (CLA), our bot will update the issue when you open a Pull Request. For any
questions about the CLA process, please refer to our [FAQ](https://cla.vmware.com/faq). For more detailed information,
refer to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
