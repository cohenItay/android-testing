package com.example.android.architecture.blueprints.todoapp.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * A view model which i created for test demonstrations purposes.
 */
class FlowViewModel : ViewModel() {

    private val _stateFlow = MutableStateFlow<String>("Hi")

    val emit123Flow = flow {
        emit123()
    }

    val significantWorkFlow: StateFlow<String> = _stateFlow.asStateFlow()

    fun onSomethingClickWhichCausesSignificantWork() = viewModelScope.launch {
        _stateFlow.value = doSomeSignificantWork()
    }

    private suspend fun doSomeSignificantWork(): String {
        delay(SIGNIFICANT_WORK_TIME)
        return SIGNIFICANT_WORK_OUTPUT
    }

    private suspend fun FlowCollector<Int>.emit123() {
        emit(1)
        delay(100L)
        emit(2)
        delay(100L)
        emit(3)
    }

    companion object {
        const val SIGNIFICANT_WORK_TIME = 800L
        const val SIGNIFICANT_WORK_OUTPUT = "Significant work is done after 800ms"
    }
}