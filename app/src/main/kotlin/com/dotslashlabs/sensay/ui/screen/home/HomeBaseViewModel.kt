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

    protected fun <A, B> onEachThrottled(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        delayByMillis: (A, B) -> Long,
        action: suspend (A, B) -> Unit,
    ) {

        var loadingJob: Job? = null

        onEach(
            prop1,
            prop2,
        ) { val1, val2 ->

            loadingJob?.cancel()
            loadingJob = viewModelScope.launch {
                val delayMillis = delayByMillis(val1, val2)
                if (delayMillis > 0) {
                    delay(delayMillis)
                }

                action(val1, val2)
            }
        }
    }
}
