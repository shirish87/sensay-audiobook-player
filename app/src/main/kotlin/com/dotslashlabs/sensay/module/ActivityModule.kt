package com.dotslashlabs.sensay.module

import android.content.Context
import com.dotslashlabs.sensay.common.PlayerHolder
import com.dotslashlabs.sensay.ui.ServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped


@InstallIn(ActivityComponent::class)
@Module
class ActivityModule {

    @Provides
    @ActivityScoped
    fun provideServiceConnection(
        @ActivityContext context: Context,
        playerHolder: PlayerHolder,
    ) = ServiceConnection(context, playerHolder)
}
