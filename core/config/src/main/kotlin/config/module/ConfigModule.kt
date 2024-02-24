package config.module

import android.content.Context
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ConfigModule {

    @Singleton
    @Provides
    fun provideConfigStore(@ApplicationContext context: Context) = ConfigStore.instance(context)
}
