package media.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@InstallIn(ServiceComponent::class)
@Module
object SpeechModule {

    @Provides
    @ServiceScoped
    fun provideAudioManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @ServiceScoped
    fun provideNotificationManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    @ServiceScoped
    fun provideForegroundServiceNotificationChannel() =
        NotificationChannel(
            "service.foreground",
            "Speech Service",
            NotificationManager.IMPORTANCE_LOW,
        )

    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
}
