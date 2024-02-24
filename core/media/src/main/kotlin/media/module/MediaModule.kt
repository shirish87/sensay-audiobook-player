package media.module

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import data.SensayStore
import media.DocumentScanner

@UnstableApi
@InstallIn(ServiceComponent::class)
@Module
object MediaModule {

    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Provides
    @ServiceScoped
    fun providePlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
    ): Player = ExoPlayer.Builder(context)
        .setRenderersFactory(DefaultRenderersFactory(context))
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    @Provides
    @ServiceScoped
    fun provideDocumentScanner(
        @ApplicationContext context: Context,
        store: SensayStore,
    ) = DocumentScanner(context, store)

//    @Provides
//    @ServiceScoped
//    fun provideMediaSession(
//        @ApplicationContext context: Context,
//        player: Player,
//    ): MediaSession {
//
//        val resultIntent = Intent(context, MainActivity::class.java).apply {
//            action = Intent.ACTION_VIEW
//        }
//
//        val sessionActivityPendingIntent = TaskStackBuilder.create(context)
//            .addNextIntent(resultIntent)
//            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//
//        return MediaSession.Builder(context, player)
//            .setCallback(mediaSessionQueue.mediaSessionCallback)
//            .apply {
//                if (sessionActivityPendingIntent != null) {
//                    setSessionActivity(sessionActivityPendingIntent)
//                }
//            }
//            .build()
//    }
}
