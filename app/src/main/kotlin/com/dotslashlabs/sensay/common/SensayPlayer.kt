package com.dotslashlabs.sensay.common

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.dotslashlabs.sensay.util.PlayerState
import com.dotslashlabs.sensay.util.mediaId
import com.dotslashlabs.sensay.util.mediaIds
import com.dotslashlabs.sensay.util.state
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logcat.logcat
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SensayPlayer(internal val player: Player) {

    private val eventsList: IntArray = intArrayOf(
        Player.EVENT_METADATA,
        Player.EVENT_MEDIA_METADATA_CHANGED,
        Player.EVENT_PLAYLIST_METADATA_CHANGED,
        Player.EVENT_PLAYBACK_STATE_CHANGED,
        Player.EVENT_IS_LOADING_CHANGED,
        Player.EVENT_PLAY_WHEN_READY_CHANGED,
        Player.EVENT_IS_PLAYING_CHANGED,
        Player.EVENT_TIMELINE_CHANGED,
        Player.EVENT_PLAYER_ERROR,
    )

    private val _playerEvents: MutableStateFlow<Instant> = MutableStateFlow(Instant.now())
    private val _priorityEvents: MutableStateFlow<Instant> = MutableStateFlow(Instant.now())

    val playerEvents: Flow<PlaybackConnectionState> = combine(
        _priorityEvents,
        _playerEvents.debounce(500.milliseconds),
    ) { a, b -> maxOf(a, b) }.map { playerToConnectionState(player) }

    val serviceEvents: Flow<PlayerState> = combine(
        _priorityEvents,
        _playerEvents.debounce(500.milliseconds),
    ) { a, b -> maxOf(a, b) }.map { player.state }

    private var isConnected: Boolean = true
    private var liveTracker: Job? = null
    private var serviceTracker: Job? = null

    private val timerFn: (liveTrackerInterval: Duration) -> suspend CoroutineScope.() -> Unit =
        { liveTrackerInterval ->
            {
                logcat { "timer-launch" }
                while (isActive) {
                    logcat { "timer-emit" }
                    thunkFlow(_playerEvents)
                    delay(liveTrackerInterval)
                }
                logcat { "timer-end" }
            }
        }

    private val listener = object : Player.Listener {

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.size() < 1 || !events.containsAny(*eventsList)) return
            logcat { "onEvents: events=${events.eventsString()}" }

            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                )
            ) {
                thunkFlow(_priorityEvents)
            } else {
                thunkFlow(_playerEvents)
            }
        }
    }

    init {
        player.addListener(listener)
        thunkFlow(_priorityEvents)
    }

    var playWhenReady: Boolean
        get() = player.playWhenReady
        set(value) {
            player.playWhenReady = value
        }

    val mediaId: String?
        get() = player.mediaId

    val isPlaying: Boolean
        get() = player.isPlaying

    fun prepare() = player.prepare()
    fun play() = player.play()
    fun pause() = player.pause()
    fun seekBack() = player.seekBack()
    fun seekForward() = player.seekForward()
    fun seekTo(mediaItemIndex: Int, positionMs: Long) = player.seekTo(mediaItemIndex, positionMs)

    fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) = player.setMediaItems(mediaItems, startIndex, startPositionMs)

    fun toPlaybackConnectionState() = playerToConnectionState(player)

    private fun thunkFlow(flow: MutableStateFlow<Instant>) {
        flow.value = Instant.now()
    }

    fun startLiveTracker(scope: CoroutineScope, interval: Duration = 1.seconds) {
        if (liveTracker != null) return
        logcat { "startLiveTracker" }
        liveTracker = scope.launch(CoroutineName("live"), block = timerFn(interval))
    }

    fun stopLiveTracker() {
        logcat { "stopLiveTracker" }
        liveTracker?.cancel()
        liveTracker = null
    }

    fun startServiceTracker(scope: CoroutineScope, interval: Duration = 10.seconds) {
        if (serviceTracker != null) return
        logcat { "startServiceTracker" }
        serviceTracker = scope.launch(CoroutineName("service"), block = timerFn(interval))
    }

    fun stopServiceTracker() {
        logcat { "stopServiceTracker" }
        serviceTracker?.cancel()
        serviceTracker = null
    }

    fun release() {
        isConnected = false
        stopLiveTracker()
        stopServiceTracker()
        player.removeListener(listener)
        player.release()
    }

    private fun playerToConnectionState(p: Player?) = PlaybackConnectionState(
        isConnected = isConnected,
        playerState = p.state,
        playerMediaIds = p.mediaIds,
    )
}


fun SensayPlayer?.toPlaybackConnectionState() =
    this?.toPlaybackConnectionState() ?: PlaybackConnectionState()


fun Player.Events.eventsString() = (0 until size()).joinToString { get(it).toString() }
