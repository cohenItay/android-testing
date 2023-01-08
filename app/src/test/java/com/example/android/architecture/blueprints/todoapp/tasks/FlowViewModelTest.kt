package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class FlowViewModelTest {

    val testDispatcher = StandardTestDispatcher()

    @get:Rule
    var mainDispatcherRule = MainDispatcherRule(testDispatcher)
    val viewModel = FlowViewModel()

    @Test
    fun getEmit123Flow() =
        // In order to test ViewModel successfully (which uses coroutines internally) we must replace the Dispatchers.Main, doing so:
        //!-------- Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler)) ------------!
        // And at the end of this function resetting it:
        //!------ Dispatchers.resetMain() // reset the main dispatcher -------!
        //Instead of writing it on each test, we are using the MainDispatcherRule.

        //Key Point: runTest runs the test coroutine in a TestScope, using a TestDispatcher.
        // TestDispatchers use a TestCoroutineScheduler to control virtual time and schedule new coroutines in a test.
        // All TestDispatchers in a test must use the same scheduler instance.
        // So we should use it as follows:
        // runTest(testDispatcher) { ... }
        // But! If the Main dispatcher has been replaced with a TestDispatcher,
        // any newly-created TestDispatchers will automatically use the scheduler from the Main dispatcher
        // So we can just use: runTest {...}
        runTest {
            val firstItem = viewModel.emit123Flow.first()
            assertThat(firstItem, `is`(1))

            val result = viewModel.emit123Flow.toList(mutableListOf())
            val expected = mutableListOf(1,2,3)
            assertThat(result, equalTo(expected)) // `is` is a shortcut for equalsTo
        }

    @Test
    fun onSomethingClickWhichCausesSignificantWork() = runTest {
        // This test shows how to collect from contentious-emitting flow
        val result = mutableListOf<String>()
        // Let us make the internal coroutines of the view model start eagerly using the unconfined, and remember to use the shared scheduler!:
        val collectFromFlowJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.significantWorkFlow.toList(result)
        }
        // Start the significant work
        viewModel.onSomethingClickWhichCausesSignificantWork()
        // The initial value of the flow is "Hi", It will take some time until the significant work will end,
        // so fir now we should only contain the initial value of `Hi`
        assertThat(result.size, `is`(1))
        assertThat(result.getOrNull(0), `is`("Hi"))
        delay(FlowViewModel.SIGNIFICANT_WORK_TIME/2)

        // No changes should occur after 400ms:
        assertThat(result.size, `is`(1))
        assertThat(result.getOrNull(0), `is`("Hi"))

        //wait a bit more
        delay((FlowViewModel.SIGNIFICANT_WORK_TIME/2) + 20)

        // The significant work should be finished finished.
        assertThat(result.size, `is`(2))
        assertThat(result.getOrNull(1), `is`(FlowViewModel.SIGNIFICANT_WORK_OUTPUT))
        collectFromFlowJob.cancel()

        /*
        Note that this stream of values is conflated,
        which means that if values are set in a StateFlow rapidly,
        collectors of that StateFlow are not guaranteed to receive all intermediate values, only the most recent one.

        Note2: If we don't know how much time would a work take, we would need to either, manage the list ourselves
        (entering the values ourselves to the list), or use the Turbine library!  https://github.com/cashapp/turbine

        Caution: When testing a StateFlow created with .stateIn operator (from cold to hot flow),
        there must be at least one collector present during the test.
        Otherwise the stateIn operator doesn't start collecting the underlying flow, and the StateFlow's value will never be updated.
         */
    }
}