package com.dotslashlabs.sensay.ui.screen.home

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

abstract class HomeBaseViewModel<S : MavericksState>(
    initialState: S
) : MavericksViewModel<S>(initialState) {

    protected fun <A, B, C> onEachThrottled(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        delayByMillis: (A, B, C) -> Long,
        action: suspend (A, B, C) -> Unit,
    ) {

        var loadingJob: Job? = null

        onEach(
            prop1,
            prop2,
            prop3,
        ) { val1, val2, val3 ->

            loadingJob?.cancel()
            loadingJob = viewModelScope.launch {
                delay(delayByMillis(val1, val2, val3))
                action(val1, val2, val3)
            }
        }
    }
}
