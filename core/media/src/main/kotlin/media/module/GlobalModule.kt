package media.module

import android.content.ComponentName
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import media.BindConnection
import media.service.MediaService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object GlobalModule {

    @Singleton
    @Provides
    fun provideMediaConnection(@ApplicationContext context: Context) = BindConnection(context, MediaService::class.java)

    @Singleton
    @Provides
    fun provideMediaServiceComponentName(@ApplicationContext context: Context) =
        ComponentName(context, MediaService::class.java)
}
