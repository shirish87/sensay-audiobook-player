package com.dotslashlabs.sensay.util

import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.core.os.bundleOf
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import androidx.compose.material3.windowsizeclass.WindowSizeClass as WindowSizeClazz

/**
 * Information about the posture of the device
 */
data class DevicePosture(
    val postureType: PostureType,
    val hingePosition: Rect? = null,
    val orientation: FoldingFeature.Orientation? = null,
) {

    companion object {
        private const val KEY_POSTURE_TYPE = "postureType"
        private const val KEY_HINGE_POSITION = "hingePosition"
        private const val KEY_ORIENTATION = "orientation"

        fun fromBundle(bundle: Bundle) = DevicePosture(
            postureType = PostureType.valueOf(bundle.getString(KEY_POSTURE_TYPE)!!),
            hingePosition = bundle.getString(KEY_HINGE_POSITION, null)?.let {
                Rect.unflattenFromString(it)
            },
            orientation = when (bundle.getInt(KEY_ORIENTATION, -1)) {
                0 -> FoldingFeature.Orientation.VERTICAL
                1 -> FoldingFeature.Orientation.HORIZONTAL
                else -> null
            }
        )

        fun default() = DevicePosture(postureType = PostureType.NORMAL)
    }

    fun toBundle() = bundleOf(
        KEY_POSTURE_TYPE to postureType.name,
        KEY_HINGE_POSITION to hingePosition?.flattenToString(),
        KEY_ORIENTATION to when (orientation) {
            FoldingFeature.Orientation.VERTICAL -> 0
            FoldingFeature.Orientation.HORIZONTAL -> 1
            else -> -1
        },
    )
}

enum class PostureType {
    BOOK,
    SEPARATING,
    NORMAL,
}

@OptIn(ExperimentalContracts::class)
fun isBookPosture(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
        foldFeature.orientation == FoldingFeature.Orientation.VERTICAL
}

@OptIn(ExperimentalContracts::class)
fun isSeparating(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.FLAT && foldFeature.isSeparating
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) {

    companion object {
        private const val KEY_WIDTH_SIZE_CLASS = "widthSizeClass"
        private const val KEY_HEIGHT_SIZE_CLASS = "heightSizeClass"

        fun fromBundle(bundle: Bundle) = WindowSizeClass(
            widthSizeClass = when (bundle.getInt(KEY_WIDTH_SIZE_CLASS)) {
                0 -> WindowWidthSizeClass.Compact
                1 -> WindowWidthSizeClass.Medium
                2 -> WindowWidthSizeClass.Expanded
                else -> throw Error("Unknown width class")
            },
            heightSizeClass = when (bundle.getInt(KEY_HEIGHT_SIZE_CLASS)) {
                0 -> WindowHeightSizeClass.Compact
                1 -> WindowHeightSizeClass.Medium
                2 -> WindowHeightSizeClass.Expanded
                else -> throw Error("Unknown height class")
            },
        )

        fun default() = WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium)
    }

    fun toBundle() = bundleOf(
        KEY_WIDTH_SIZE_CLASS to when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> 0
            WindowWidthSizeClass.Medium -> 1
            WindowWidthSizeClass.Expanded -> 2
            else -> -1
        },
        KEY_HEIGHT_SIZE_CLASS to when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 0
            WindowHeightSizeClass.Medium -> 1
            WindowHeightSizeClass.Expanded -> 2
            else -> -1
        }
    )
}

fun WindowSizeClazz.toWindowSizeClass() = WindowSizeClass(widthSizeClass, heightSizeClass)

/**
 * Flow of [DevicePosture] that emits every time there's a change in the windowLayoutInfo
 */
fun ComponentActivity.createDevicePostureFlow(): StateFlow<DevicePosture> {
    return WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this)
        .flowWithLifecycle(this.lifecycle)
        .map { layoutInfo ->
            val foldingFeature =
                layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()

            when {
                isBookPosture(foldingFeature) ->
                    DevicePosture(
                        postureType = PostureType.BOOK,
                        hingePosition = foldingFeature.bounds,
                    )

                isSeparating(foldingFeature) ->
                    DevicePosture(
                        postureType = PostureType.SEPARATING,
                        hingePosition = foldingFeature.bounds,
                        orientation = foldingFeature.orientation,
                    )

                else -> DevicePosture(postureType = PostureType.NORMAL)
            }
        }
        .stateIn(
            scope = lifecycleScope,
            started = SharingStarted.Eagerly,
            initialValue = DevicePosture(postureType = PostureType.NORMAL),
        )
}
