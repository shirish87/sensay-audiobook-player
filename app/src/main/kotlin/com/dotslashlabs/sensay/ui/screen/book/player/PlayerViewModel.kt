package com.dotslashlabs.sensay.ui.screen.book.player

import android.net.Uri
import android.os.Bundle
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters

data class PlayerViewState(
    @PersistState val bookId: Long,
    val bookProgressWithChapters: Async<BookProgressWithBookAndChapters> = Uninitialized,
) : MavericksState {
    constructor(arguments: Bundle) : this(bookId = arguments.getString("bookId")!!.toLong())

    val coverUri: Uri? = (bookProgressWithChapters as? Success)?.invoke()?.book?.coverUri
}

class PlayerViewModel @AssistedInject constructor(
    @Assisted private val state: PlayerViewState,
    store: SensayStore,
) : MavericksViewModel<PlayerViewState>(state) {

    init {
        store.bookProgressWithBookAndChapters(state.bookId)
            .execute(retainValue = PlayerViewState::bookProgressWithChapters) {
                copy(bookProgressWithChapters = it)
            }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object : MavericksViewModelFactory<PlayerViewModel, PlayerViewState>
    by hiltMavericksViewModelFactory()
}
