package com.dotslashlabs.sensay.module

import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.MavericksViewModelComponent
import com.airbnb.mvrx.hilt.ViewModelKey
import com.dotslashlabs.sensay.ui.app.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.book.player.PlayerViewModel
import com.dotslashlabs.sensay.ui.screen.home.current.CurrentViewModel
import com.dotslashlabs.sensay.ui.screen.home.library.LibraryViewModel
import com.dotslashlabs.sensay.ui.screen.sources.SourcesViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap

@Module
@InstallIn(MavericksViewModelComponent::class)
interface ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SensayAppViewModel::class)
    fun sensayAppViewModelFactory(factory: SensayAppViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(CurrentViewModel::class)
    fun currentViewModelFactory(factory: CurrentViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(LibraryViewModel::class)
    fun libraryViewModelFactory(factory: LibraryViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    fun playerViewModelFactory(factory: PlayerViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(SourcesViewModel::class)
    fun sourcesViewModelFactory(factory: SourcesViewModel.Factory): AssistedViewModelFactory<*, *>
}
