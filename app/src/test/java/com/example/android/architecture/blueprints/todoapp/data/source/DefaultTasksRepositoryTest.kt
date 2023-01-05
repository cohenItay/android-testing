package com.example.android.architecture.blueprints.todoapp.data.source

import com.example.android.architecture.blueprints.todoapp.MainDispatcherRule
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultTasksRepositoryTest {

    @get:Rule
    var mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val task1 = Task("Title1", "Description1")
    private val task2 = Task("Title2", "Description2")
    private val task3 = Task("Title3", "Description3")
    private val remoteTasks = listOf(task1, task2).sortedBy { it.id }
    private val localTasks = listOf(task3)
    private lateinit var tasksRemoteDataSource: FakeDataSource
    private lateinit var tasksLocalDataSource: FakeDataSource

    // Class under test:
    private lateinit var tasksRepository: TasksRepository

    @Before
    fun createRepository() {
        tasksRemoteDataSource = FakeDataSource(remoteTasks.toMutableList())
        tasksLocalDataSource = FakeDataSource(localTasks.toMutableList())
        tasksRepository = DefaultTasksRepository(
            tasksRemoteDataSource = tasksRemoteDataSource,
            tasksLocalDataSource = tasksLocalDataSource,
            ioDispatcher = mainDispatcherRule.testDispatcher // IMPORTANT!
        )
    }

    @Test
    fun getTasks_requestsAllTasksFromRemoteDataSource() = runTest { // Use this kotlin function to run tests.
        /*
         runTest has the following behaviors meant for testing:
                1. It skips delay, so your tests run faster.
                2. It adds testing related assertions to the end of the coroutine.
                   These assertions fail if you launch a coroutine and it continues running after
                   the end of the runBlocking lambda (which is a possible coroutine leak) or
                   if you have an uncaught exception.
                3. It gives you timing control over the coroutine execution.
         */
        // When tasks are requested from the tasks repository
        val tasks = tasksRepository.getTasks(true) as Result.Success
        // Then tasks are loaded from the remote data source
        assertThat(tasks.data, IsEqual(remoteTasks))
    }

    @Test
    fun getTasks_requestsAllTasksFromRemoteDataSourceNewCoroutine() = runTest {
        /*
            Code under test might use dispatchers to switch threads (using withContext) or to start new coroutines.
            When code is executed on multiple threads in parallel, tests can become flaky.
            It can be difficult to perform assertions at the correct time or to wait for tasks to complete if
            they’re running on background threads that you have no control over.

            When your code creates new coroutines other than the top-level test coroutine that runTest creates,
            you’ll need to control how those new coroutines are scheduled by choosing the appropriate TestDispatcher.
            runTest uses a StandardTestDispatcher by default.
            If your code moves the coroutine execution to other dispatchers (for example, by using withContext), runTest will still generally work,
            but delays will no longer be skipped, and tests will be less predictable as code runs on multiple threads.
            For these reasons, in tests you should inject test dispatchers to replace real dispatchers.
         */
        var tasks: Result.Success<List<Task>>? = null
        launch {
            tasks = tasksRepository.getTasks(true) as Result.Success
        }
        /**
         * When you start new coroutines on a StandardTestDispatcher they are queued up on the underlying scheduler,
         * to be run whenever the test thread is free to use.
         * To let these new coroutines run, you need to yield the test thread
         * (free it up for other coroutines to use)

         * advanceUntilIdle - Runs all other coroutines on the scheduler until there is nothing left in the queue.
         * This is a good default choice to let all pending coroutines run, and it will work in most test scenarios.
         * You can read on more possibilities of coroutines control here:
         * https://developer.android.com/kotlin/coroutines/test#standardtestdispatcher
         */
        advanceUntilIdle() // Without it, the test will fail. This is because the Dispatcher which is being used is StandardTestDispatcher

       // You could also join the Job instances returned by the launch calls to make sure that the new coroutines are done before performing the assertion.
        assertThat(tasks?.data, IsEqual(remoteTasks))
    }
}