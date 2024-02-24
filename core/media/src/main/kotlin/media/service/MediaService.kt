package media.service

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.bundleOf
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.dotslashlabs.media3.extractor.m4b.M4bExtractor
import com.dotslashlabs.media3.extractor.m4b.metadata.ChapterMetadata
import com.dotslashlabs.media3.extractor.m4b.metadata.M4bMetadata
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import config.ConfigStore
import dagger.hilt.android.AndroidEntryPoint
import data.SensayStore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import logcat.logcat
import media.AudioEffectCommands
import media.BookProgressWithDuration
import media.ExtraSessionCommands
import media.MediaId
import media.MediaSessionCommands
import media.PlayableMediaItem
import media.R
import media.ServiceBinder
import media.bookId
import media.chapterId
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds


sealed class MediaServiceState {
    data class Loading(val isLoading: Boolean): MediaServiceState()
    data class Idle(val isReady: Boolean): MediaServiceState()
    data class Playing(val mediaId: MediaId): MediaServiceState()
    data class MediaItemChanged(val mediaId: MediaId): MediaServiceState()
    data class MediaItemsAdded(val mediaIds: List<MediaId>): MediaServiceState()
    data class Stopped(val exception: Throwable?): MediaServiceState()
    data class Error(val mediaId: String?, val message: String): MediaServiceState()
}

@AndroidEntryPoint
class MediaService : MediaSessionService() {

    private val backgroundJob = Job() + CoroutineName(MediaService::class.java.simpleName)
    private val scope = CoroutineScope(Dispatchers.Main + backgroundJob)

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var store: SensayStore

    @Inject
    lateinit var configStore: ConfigStore

    private val _serviceState: MutableStateFlow<MediaServiceState> =
        MutableStateFlow(MediaServiceState.Idle(false))

    val serviceStateFlow: StateFlow<MediaServiceState> = _serviceState

    private val binder = object : ServiceBinder<MediaService>() {
        override fun getService() = this@MediaService
    }

    private val mediaItemsCache: MutableMap<MediaId, BookProgressWithDuration> = mutableMapOf()
    private val appliedAudioEffects: MutableMap<AudioEffectCommands, AudioEffect> = mutableMapOf()

    private val executorService: ListeningExecutorService = MoreExecutors.listeningDecorator(
        Executors.newSingleThreadExecutor { runnable: Runnable? ->
            Thread(runnable, "SessionCommandExecutor")
        })

    private val sampleMediaMap: Map<Long, Uri> by lazy {
        listOf(
            R.raw.sample1,
            R.raw.sample2,
            R.raw.sample3,
            R.raw.sample4,
            R.raw.sample5,
            R.raw.sample6,
        ).mapIndexed { index, resourceId ->
            index.toLong() to Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build()
        }.toMap()
    }

    private val sessionCallback = object : MediaSession.Callback {

        @UnstableApi
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            val queuedMediaItems = (0 until mediaSession.player.mediaItemCount).map {
                mediaSession.player.getMediaItemAt(it)
            }

            if (queuedMediaItems.isEmpty()) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        emptyList(),
                        0,
                        0L,
                    )
                )
            }

            return executorService.submit(Callable {
                val mediaItems = queuedMediaItems.mapNotNull {
                    val mediaUri = it.requestMetadata.mediaUri ?: return@mapNotNull null

                    logcat { "onPlaybackResumption: chapter.uri: $mediaUri" }
                    it.buildUpon().setUri(mediaUri).build()
                }

                MediaSession.MediaItemsWithStartPosition(
                    mediaItems,
                    mediaSession.player.currentMediaItemIndex,
                    mediaSession.player.currentPosition,
                )
            })
        }

        @UnstableApi
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {

            if (mediaItems.isEmpty()) {
                return Futures.immediateFuture(mutableListOf())
            }

            mediaSession.player.clearMediaItems()
            mediaItemsCache.clear()

            return executorService.submit(Callable {
                mediaItems.mapNotNull {
                    val mediaUri = it.requestMetadata.mediaUri ?: return@mapNotNull null

                    logcat { "onAddMediaItems: chapter.uri: $mediaUri" }
                    it.buildUpon().setUri(mediaUri).build()
                }.toMutableList()
            })
        }

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {

            val connectionResult = super.onConnect(session, controller)

            val sessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .apply {
                    ExtraSessionCommands.commands.forEach(::add)
                    AudioEffectCommands.commands.forEach(::add)
                    MediaSessionCommands.commands.forEach(::add)
                }
                .build()

            logcat { "onConnect" }

            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands,
            )
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onPostConnect(session, controller)

            logcat { "onPostConnect" }
            postUpdates()
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            super.onDisconnected(session, controller)

            logcat { "onDisconnected" }
            _serviceState.value = MediaServiceState.Idle(false)
        }

        @UnstableApi
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {

            val combinedArgs = if (args.isEmpty) {
                customCommand.customExtras
            } else Bundle(args).apply {
                putAll(customCommand.customExtras)
            }

            val player = (session.player as? ExoPlayer)
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE))

            ExtraSessionCommands.resolve(customCommand.customAction)?.let {
                player.skipSilenceEnabled = ExtraSessionCommands.isEnabled(combinedArgs)
                logcat { "Applied skipSilenceEnabled: $player.skipSilenceEnabled" }
                return executorService.submit(Callable {
                    SessionResult(SessionResult.RESULT_SUCCESS)
                })
            }

            AudioEffectCommands.resolve(customCommand.customAction)?.let { audioEffectCommand ->
                if (player.audioSessionId <= 0) return@let

                logcat { "Applied audioEffectCommand: ${audioEffectCommand.name}" }
                return executorService.submit(Callable {
                    executeAudioEffectCommand(
                        player,
                        audioEffectCommand,
                        customCommand.customAction,
                        combinedArgs,
                    )
                })
            }

            MediaSessionCommands.resolve(customCommand.customAction)?.let { mediaSessionCommand ->
                logcat { "Applied mediaSessionCommand: ${mediaSessionCommand.name}" }
                return executorService.submit(Callable {
                    executeMediaSessionCommand(
                        player,
                        mediaSessionCommand,
                        combinedArgs,
                    )
                })
            }

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private var mediaSession: MediaSession? = null

    private fun postUpdates() {
        _serviceState.value = MediaServiceState.Idle(true)
    }

    override fun onCreate() {
        super.onCreate()

        logcat { "onCreate" }
        mediaSession = mediaSession ?: provideMediaSession(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE) {
            return super.onBind(intent)
        }

        return binder
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        logcat { "onStartCommand: $intent flags=$flags" }

        if (mediaSession != null) {
            postUpdates()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        logcat { "onDestroy" }

        if (!executorService.isShutdown) {
            executorService.shutdown()
        }

        if (scope.isActive) {
            scope.cancel()
        }

        player.release()

        mediaSession?.release()
        mediaSession = null

        stopSelf()
        super.onDestroy()
    }

    @UnstableApi
    private fun executeAudioEffectCommand(
        player: ExoPlayer,
        audioEffectCommand: AudioEffectCommands,
        customAction: String,
        args: Bundle,
    ): SessionResult = try {
        val effect = AudioEffectCommands.toAudioEffect(
            player.audioSessionId,
            customAction,
            isEnabled = AudioEffectCommands.isEnabled(args),
        )

        check(effect != null) { "AudioEffect not found: $customAction" }

        player.setAuxEffectInfo(AuxEffectInfo(effect.id, 1F))

        if (effect.enabled) {
            appliedAudioEffects[audioEffectCommand] = effect
        } else {
            appliedAudioEffects.remove(audioEffectCommand)?.apply {
                setEnableStatusListener(null)
                release()
            }
        }

        logcat { "Applied effect: ${customAction}=${effect.enabled}" }
        SessionResult(SessionResult.RESULT_SUCCESS)
    } catch (e: Throwable) {
        e.printStackTrace()

        SessionResult(
            SessionResult.RESULT_ERROR_NOT_SUPPORTED,
            bundleOf(AudioEffectCommands.RESULT_ARG_ERROR to e.message),
        )
    }

    @UnstableApi
    private fun executeMediaSessionCommand(
        player: ExoPlayer,
        mediaSessionCommand: MediaSessionCommands,
        args: Bundle,
    ): SessionResult = try {

        logcat { "player: $player mediaSessionCommand: $mediaSessionCommand" }

        val bookId = MediaSessionCommands.getBookId(args)
        val chapterIndex = MediaSessionCommands.getChapterIndex(args)

        val bookProgressWithBookAndChapters = runBlocking {
            store.bookProgressWithBookAndChapters(bookId).firstOrNull()
        }

        val playableMediaItem = if (bookProgressWithBookAndChapters != null) {
            val duration = bookProgressWithBookAndChapters.durationMs

            val chapter = bookProgressWithBookAndChapters.chapters.getOrNull(chapterIndex)
                ?: bookProgressWithBookAndChapters.chapter

            val sourceUri = chapter.uri

            val chapters: List<ChapterMetadata> = bookProgressWithBookAndChapters.chapters.map {
                ChapterMetadata(it.start.ms.milliseconds.inWholeMicroseconds, it.title)
            }

            val metadata = M4bMetadata.Builder()
                .setAlbumArtist(bookProgressWithBookAndChapters.book.author)
                .setAlbumTitle(bookProgressWithBookAndChapters.book.title)
                .setCompilation(bookProgressWithBookAndChapters.book.series)
                .setComposer(bookProgressWithBookAndChapters.book.narrator)
                .setDescription(bookProgressWithBookAndChapters.book.description)
                .setArtist(chapter.author)
                .setTitle(chapter.title)
                .setArtworkUri(chapter.coverUri)
                .setChapters(chapters)
                .setIsPlayable(true)
                .build()

            PlayableMediaItem(
                bookId,
                metadata.toBundle(),
                metadata.chapters?.map {
                    sourceUri to ChapterMetadata(it.startTime, it.chapterTitle?.trim())
                } ?: emptyList(),
                if (duration > 0) duration else null,
                if (duration > 0) 0L else null,
                chapterIndex,
            )
        } else {
            val sourceUri = sampleMediaMap[bookId]
            check(sourceUri != null) { "bookId is invalid" }

            val extractors = M4bExtractor.createDefaultExtractors()
            logcat { "extractors: ${extractors.joinToString { it.javaClass.simpleName }}" }
            val mediaSourceFactory = DefaultMediaSourceFactory(baseContext) { extractors }

            val results = MetadataRetriever.retrieveMetadata(
                mediaSourceFactory,
                MediaItem.fromUri(sourceUri),
            ).get()

            val metadata = (0 until results.length).mapNotNull(results::get)
                .flatMap { trackGroup ->
                    (0 until trackGroup.length).mapNotNull(trackGroup::getFormat)
                }.fold(M4bMetadata.Builder()) { builder, format ->
                    builder.populateFromFormat(format)
                }.build()

            val durationUs = metadata.durationUs ?: C.TIME_UNSET

            PlayableMediaItem(
                bookId,
                metadata.toBundle(),
                metadata.chapters?.map {
                    sourceUri to ChapterMetadata(it.startTime, it.chapterTitle?.trim())
                } ?: emptyList(),
                if (durationUs > 0) durationUs.microseconds.inWholeMilliseconds else null,
                if (durationUs > 0) 0L else null,
                chapterIndex,
            )
        }

        val resultArgs = bundleOf(
            MediaSessionCommands.RESULT_ARG_PLAYABLE_MEDIA_ITEM to playableMediaItem,
        )

        SessionResult(SessionResult.RESULT_SUCCESS, resultArgs)
    } catch (e: Throwable) {
        logcat { "executeMediaSessionCommand: ${e.message}" }
        e.printStackTrace()

        SessionResult(
            SessionResult.RESULT_ERROR_IO,
            bundleOf(MediaSessionCommands.RESULT_ARG_ERROR to e.message),
        )
    }

    private fun provideMediaSession(
        context: Context,
    ): MediaSession {
//
//        val resultIntent = Intent(context, MainActivity::class.java).apply {
//            action = Intent.ACTION_VIEW
//        }

//        val sessionActivityPendingIntent = TaskStackBuilder.create(context)
//            // .addNextIntent(resultIntent)
//            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        logcat { "provideMediaSession" }

        return MediaSession.Builder(context, player)
            .setCallback(sessionCallback)
//            .apply {
//                if (sessionActivityPendingIntent != null) {
//                    setSessionActivity(sessionActivityPendingIntent)
//                }
//            }
            .build()
    }
}
