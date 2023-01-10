package com.example.android.architecture.blueprints.todoapp.tasks

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.android.architecture.blueprints.todoapp.Event
import com.example.android.architecture.blueprints.todoapp.MainDispatcherRule
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.FakeTasksRepository
import com.example.android.architecture.blueprints.todoapp.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A test runner is a JUnit component that runs tests.
 * Without a test runner, your tests would not run at all.
 * There's a default test runner provided by JUnit that you get automatically without writing any code.
 * @RunWith swaps out that default test runner.
 */
//@RunWith(AndroidJUnit4::class) // Uses the androidX core test library in order to provide 'instrument' classes such as context
@ExperimentalCoroutinesApi
class TasksViewModelTest {

    @get:Rule // Runs the rule, each rule has starting code (before test starts) and finished code( after test ends). see the implementation of InstantTaskExecutorRule as an example
    var instantExecutorRule = InstantTaskExecutorRule() //This rule runs all Architecture Components-related background jobs in the same thread so that the test results happen synchronously, and in a repeatable order. When you write tests that include testing LiveData, use this rule!

    @get:Rule
    var mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var tasksRepository: FakeTasksRepository

    /**
     * Shared member to all different tests
     */
    private lateinit var vm: TasksViewModel // class under test

    @Before
    fun setupViewModel() {
        tasksRepository = FakeTasksRepository()
        val task1 = Task("Title1", "Description1")
        val task2 = Task("Title2", "Description2", true)
        val task3 = Task("Title3", "Description3", true)
        tasksRepository.addTasks(task1, task2, task3)
        //At first the ViewModel was instantiated with application reference:
        // vm = TasksViewModel(ApplicationProvider.getApplicationContext())
        // This why we have needed the Rule
        // But now it is not required, because the ViewModel is using a FAKE repo which no Android's context is required for constructing it.
        vm = TasksViewModel(tasksRepository)
    }

    @Test
    fun addNewTask_setsNewTaskEvent() {
        vm.addNewTask()
        val observer = Observer<Event<Unit>> {}
        try {
            // because we don't have any lifecycleOwner, we'll use observeForever
            /**
             * This observation is important. You need to have active observers for the LiveData in order to:
             * * trigger any onChanged events.
             * * trigger any Transformations.
             */
            vm.newTaskEvent.observeForever(observer)
            // after observation is made:
            assertThat(vm.newTaskEvent.value, `is`(notNullValue()))
        } finally {
            // No matter what happens remove the observer!
            vm.newTaskEvent.removeObserver(observer)
        }

        // This try-finally code is a boilerplate and complicated, we can use instead: (see also following function doc)
         vm.newTaskEvent.getOrAwaitValue { /*This lambda just notified that the observation occurred, you can leave it empty */ }
    }

    @Test
    fun setFilterAllTasks_tasksAddViewVisible() {
        vm.setFiltering(TasksFilterType.ALL_TASKS)
        assertThat(vm.tasksAddViewVisible.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun completeTask_dataAndSnackbarUpdated() {
        // The viewModel uses the FakeRepo which uses coroutine.
        // In order to test ViewModel successfully (which uses coroutines internally) we must replace the Dispatchers.Main, doing so:
        //!-------- Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler)) ------------!
        // And at the end of this function resetting it:
        //!------ Dispatchers.resetMain() // reset the main dispatcher -------!
        //Instead of writing it on each test, we are using the MainDispatcherRule.

        // Create an active task and add it to the repository.
        val task = Task("Title", "Description")
        tasksRepository.addTasks(task)

        // Mark the task as complete task.
        vm.completeTask(task, true)

        // Verify the task is completed.
        assertThat(tasksRepository.tasksServiceData[task.id]?.isCompleted, `is`(true))

        // Assert that the snackbar has been updated with the correct text.
        val snackbarText: Event<Int> =  vm.snackbarText.getOrAwaitValue()
        assertThat(snackbarText.getContentIfNotHandled(), `is`(R.string.task_marked_complete))

    }
}