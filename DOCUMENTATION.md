### Table of Contents  
* [Operation Interface](#interface)  
* [Writing Operations](#operations)  
* [Validation](#validators)
* [Operation Collections](#collections)  

<a name="interface"/>

## The interface

An ```Operation``` is an adaptation of the [command pattern](https://en.wikipedia.org/wiki/Command_pattern) that knows how to revert executed commands, so that
artifacts are not left behind after the test is complete.

The concept is very simple, as you can see from the interface below, but the
power of the pattern comes from how they can be combined to write highly
validating, easy to understand, and efficient integration tests.


##### Operation.java
```java
/**
 * Operations are an extension of the Command pattern that
 * can exist in one of two states -- executed or not.
 *
 * When created, operations are not executed automatically.  Operations
 * can be executed by calling execute(), and "un-executed" by calling revert().
 *
 * In general, reverted operations can be re-performed by calling execute()
 * a second time.
 *
 * Operations are autocloseable.  On cleanup/close, operations will be reverted, but
 * errors will be suppressed.
 */
public interface Operation extends AutoCloseable {

/**
 * Perform the command.
 * Throws an error on failure.
 * When the execution has completed, isExecuted() will return true.
 */
void execute() throws Exception;

/**
 * "Un-perform" a command.
 * Throws an IllegalStateException on failure.
 * When the revert has completed, isExecuted() will return false.
 */
void revert() throws Exception;

/**
 * @return the status of the command.
 */
boolean isExecuted();

/**
 * Gracefully revert the command, and cleanup.
 * It is assumed that the test is over, so the cleanup can
 * be destructive and quick.
 * Commands cannot be re-executed after close().
 */
void cleanup();
}
```
<a name="operations"/>

## More on design

When writing tests, generally there are two problems to solve.  Operations can help
with both of them, but it is important to understand their dual role to get the most
value out of writing your tests with Operations.

### Role #1: Setup a scenario for testing

In this capacity, Operations are the essential building blocks for creating a scenario.

As building blocks, they should be easy to configure for the default cases, with as
little required configuration as possible.  For example, maybe you are creating a bank application and
want to create a new account.  The CreateAccountOp should take a single parameter to configure
itself, which might be information about the account holder.

To control the other parts of configuration, it is helpful to write fluent configuration functions.
These might allow you to specify an account type or fee schedule; things which can be defaulted and only need to be changed for specific testing scenarios.

The important thing is to remember that the users of your CreateAccountOp will likely not be too
concerned about the details of the account, so they should be hidden.  That way they can focus on
their specific problems of legal compliance or tax rules, or whatever.

### Role #2: Encapsulating created objects

The operation owns the objects that it creates or the behavior that it encapsulates.  If you need access to that information, it is easiest to get it from the operation itself.

In the previous bank example, rather than passing the name of the account holder to the CreateAccountOp, you probably needed to create the account holder, so pass the CreateEntityOp in the constructor.

Two benefits come from this practice.  First, it is convenient, and gives you flexibility to return different aspects of the resource.  Maybe some places need a name, others need an ID -- both accessors can be available on the operation.

Second, it allows for chained execution.  If you need to create multiple entities and multiple accounts, you can build a sequence that creates the entity and the account, then build 5 copies of that sequence in an OperationList.  Calling execute on the list will create the 5 accounts in parallel!


### Role #3: Validating conditions

The goal of tests is to create scenarios and validate conditions. Operations make a great place to
attach methods used for validations, since they already have all of the state available
to them.

#### Basic validations

Validations that are computationally cheap, such as null pointer checks, equality checks, range checks and the like,
should always be included directly in the execute() and revert() implementations.  This provides
the highest level of confidence that implementations are correct every time the test is run.

#### Complex Validation methods

For complex, time-consuming validations, there are two options.  The first is to add methods to the operation which
can be called directly from tests where they are relevant.  Confirming that external systems
are in sync with local systems, or iterating over large lists and validating items are examples where
this option makes sense.

#### Validator Classes

The most flexible option for complex validations is to add validator instances to operations.  This powerful
mechanism allows you to configure the validators, and add or remove specific validators based on the specific
testing situation.

Validators are also called during cleanup, so they can be a good option to ensure that resources on
external systems are cleaned up when running generic operations.

Generally, validators will be added by test writers (not operation implementors), but they can also be added to operations by default,
and test writers still have full control to remove them in order to test specific scenarios or speed
up long-running scenarios.


## Writing Operations

Two base classes for Operations are provided, depending on whether the test code
you are writing is synchronous or asynchronous. Since it is very common for test
code to be written synchronously (even for asynchronous implementations), this document
will focus on that.  Asynchronous implementations can refer to the source code.

### OperationSyncBase class

To walk through how to write an operation, we will describe this example from the project:

```java (.line-numbers}
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

    public CreateFolderOp writeable(boolean writeable) {
        this.writeable = writeable;
        return this;
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

        path.toFile().setWritable(writeable);
    }

    @Override
    public void revertImpl() throws Exception {
        Files.delete(path);
        path = null;
    }

    @Override
    public boolean isExecuted() {
        return path != null;
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
}
```

### Constructor methods
(lines 12-13)
Operations will often have multiple constructors available.  One is usually the
simplest form, where defaults are assumed and properties are loaded from the
environment where possible. Others will give deeper access to the operation,
so that specialized tests can be written for non-standard options.

It is also common for constructors to take other operations as parameters,
instead of the output of those operations.  Besides being a compact way to
pass results from one operation to another, it allows the second operation to
be "chained" or composed in a sequence with other operations.

It is also very convenient to add fluent methods to operations for configuration
and setup of non-default conditions.  Since operations are not executed immediately
they can be configured until the execute() method is called.

### Execution
(lines 20-30)
The main code of the operation is contained in the executeImpl() and revertImpl()
methods.

These methods should be the inverse of each other.

In between is a method called isExecuted().  This serves as a toggle for the state
of the operation.  If the operation has not been executed, then it should return false,
if it has been executed and is ready for reverting, then it should be true.  After
a successful revert, then it should be false again, signalling that the action can
be redone.

It is a good idea for executeImpl() and revertImpl() to be be reentrant.  If there
is a failure during the execution of either, then the cleanup code will call
revertImpl as long as isExecuted is true.

If more specific error handling is required to clean up half-completed
executions and reversions, then operations can override cleanup directly.

### Accessor methods
(lines 31-35)
It is very common for the result of the operation or clients used by the operation
to be available through an accessor method.

This makes the operation a convenient holder of all the information regarding
the created or reserved resource.  The accessor methods should assert that the
operation has been executed if necessary.

<a name="tests"/>

<a name="validation"/>

## Validation
Well-written Operations that complete without error should externally verify that
the operations completed as expected.  In addition, test writers can add validation
methods to the operation for specific sitations.  These validations are extracted
into reusable "Validator" classes in order to allow reuse.

By giving full control of the validations to the test writers, they can balance
validation time and overall speed.
More elaborate or time-consuming validations can be written, and only added
in situations where they are appropriate.

### Validator Classes
Validation instances are called after execute() and revert()
of the operation has been called successfully.  They are called in parallel
for optimal runtime speed.

The preferred pattern is that validation objects are attached automatically to
the operation in the constructor.  This allows test authors to optionally
remove some validations or add others.

### OperationCollections

Groups of operations can be treated as operations themselves.

Both serialized collections (OperationSequence) and parallel collections (OperationList) are implemented.

#### OperationSequence

This is the simplest collection of operations, and commonly used to
excapsulate a test.

Because operations are auto-closeable, a try-with-resources call can
create a sequence, and then operations can be executed and added to the
sequence simultaneously.

If there is an error during the execution of the operation, then the test
will exit, and the sequence auto-close will call cleanup on all the previously
executed operations.

Because cleanup suppresses throwing more exceptions, the test method will
pass along the original exception, even if there were problems during cleanup.

##### Typical test pattern
```java
@Test
public void fooTest() throws Exception {
  try (ops = Operations.sequence()) {
      Operation firstOp = new SomeOperation();
      ops.addExecute(firstOp);

      Operation secondOp = new AnotherOperation();
      ops.addExecute(secondOp);
  }
}
```

Note that revert and cleanup of sequences are done in reverse order.

#### OperationList

This allows for parallel execution of operations.

By default the OperationList is successful if all of the operations in
the list return successfully.  If any of the operations throws an exception,
then one of the exceptions will be propagated and the list will not
advance to the "executed" state.

This behavior can be adjusted by setting the setRequiredForSuccess() value
to a number less than the number of items in the list.

This can be useful for testing race conditions and first-wins style API calls.

All the elements of a list will be cleaned up when the list is cleaned up,
but note that only successfully executed elements will call to revertImpl by default.

#### Combining collections

Combining parallel lists, and sequential lists, with chained operation calls can
make a powerful combination for creating test fixtures or scale testing.
