package com.example.android.architecture.blueprints.todoapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule which accept A [TestDispatcher] and applies it.
 * There are classes such as ViewModels that uses an integrated coroutineScope, such as viewModelScope. These scopes
 * are using Dispatchers.Main which schedules and dispatches new coroutines. But, we want to use
 * [TestDispatcher] instead. so that [TestWatcher] rule will replace the Dispatchers.Main to our own [testDispatcher], thus we are making
 * the viewModelScope to use our dispatcher.
 *
 * A [TestDispatcher] control how those new coroutines within the test are scheduled.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    /*
    * When new coroutines are started on an UnconfinedTestDispatcher, they are started eagerly on the current thread.
    * This means that they’ll start running immediately, without waiting for their coroutine builder to return.
    * In many cases, this dispatching behavior results in simpler test code,
    * as you don’t need to manually yield the test thread to let new coroutines run.
    */
    val testDispatcher: TestDispatcher,
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}