package com.dotslashlabs.sensay.ui.screen.book.player

import android.net.Uri
import android.os.Bundle
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

data class PlayerViewState(
    @PersistState val bookId: Long,
) : MavericksState {
    constructor(arguments: Bundle) : this(bookId = arguments.getString("bookId", "0").toLong())

    val coverUri: Uri? = null
}

class PlayerViewModel @AssistedInject constructor(
    @Assisted private val state: PlayerViewState,
) : MavericksViewModel<PlayerViewState>(state) {

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object : MavericksViewModelFactory<PlayerViewModel, PlayerViewState>
    by hiltMavericksViewModelFactory()
}
