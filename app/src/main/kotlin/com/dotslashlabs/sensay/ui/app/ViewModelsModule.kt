package com.dotslashlabs.sensay.ui.app

import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.MavericksViewModelComponent
import com.airbnb.mvrx.hilt.ViewModelKey
import com.dotslashlabs.sensay.ui.screen.book.player.PlayerViewModel
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap

@Module
@InstallIn(MavericksViewModelComponent::class)
interface ViewModelsModule {

    @Binds
    @IntoMap
    @ViewModelKey(SensayAppViewModel::class)
    fun sensayAppViewModelFactory(factory: SensayAppViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    fun homeViewModelFactory(factory: HomeViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    fun playerViewModelFactory(factory: PlayerViewModel.Factory): AssistedViewModelFactory<*, *>
}
