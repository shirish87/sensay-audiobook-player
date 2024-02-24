package com.dotslashlabs.sensay.module

import androidx.media3.common.util.UnstableApi
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.MavericksViewModelComponent
import com.airbnb.mvrx.hilt.ViewModelKey
import com.dotslashlabs.sensay.ui.common.ScannerViewModel
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewModel
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewModel
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
    @ViewModelKey(HomeViewModel::class)
    fun homeViewModelFactory(factory: HomeViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    @UnstableApi
    fun playerViewModelFactory(factory: PlayerViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(NowPlayingViewModel::class)
    @UnstableApi
    fun nowPlayingViewModelFactory(factory: NowPlayingViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(SourcesViewModel::class)
    fun sourcesViewModelFactory(factory: SourcesViewModel.Factory): AssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @ViewModelKey(ScannerViewModel::class)
    fun scannerViewModelFactory(factory: ScannerViewModel.Factory): AssistedViewModelFactory<*, *>
}
