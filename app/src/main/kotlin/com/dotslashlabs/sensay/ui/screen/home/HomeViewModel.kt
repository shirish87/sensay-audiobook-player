package com.dotslashlabs.sensay.ui.screen.home

import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookProgressWithBookAndShelves
import data.entity.BookWithChapters
import kotlinx.coroutines.launch

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

data class HomeViewState(
    val books: Async<List<BookProgressWithBookAndShelves>> = Uninitialized,
    @PersistState val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
) : MavericksState

class HomeViewModel @AssistedInject constructor(
    @Assisted private val state: HomeViewState,
    private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<HomeViewState>(state) {

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it.invoke() ?: DEFAULT_HOME_LAYOUT)
        }
    }

    fun setHomeLayout(layout: HomeLayout) = viewModelScope.launch { configStore.setHomeLayout(layout) }

    fun booksWithChapters() = store.booksWithChapters()
    fun booksCount() = store.booksCount()

    suspend fun createBooksWithChapters(booksWithChapters: List<BookWithChapters>) =
        store.createBooksWithChapters(booksWithChapters)

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState>
    by hiltMavericksViewModelFactory()
}
