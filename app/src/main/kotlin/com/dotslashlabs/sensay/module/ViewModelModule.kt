package com.dotslashlabs.sensay.module

import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.MavericksViewModelComponent
import com.airbnb.mvrx.hilt.ViewModelKey
import com.dotslashlabs.sensay.ui.PlayerAppViewModel
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewModel
import com.dotslashlabs.sensay.ui.screen.restore.RestoreViewModel
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
    fun sensayAppViewModelFactory(
        factory: SensayAppViewModel.Factory
    ): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(PlayerAppViewModel::class)
    fun playerAppViewModelFactory(
        factory: PlayerAppViewModel.Factory
    ): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    fun homeViewModelFactory(factory: HomeViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    fun playerViewModelFactory(factory: PlayerViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(RestoreViewModel::class)
    fun restoreViewModelFactory(factory: RestoreViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(SourcesViewModel::class)
    fun sourcesViewModelFactory(factory: SourcesViewModel.Factory): AssistedViewModelFactory<*, *>
}
