package com.dotslashlabs.sensay.ui.nav

import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dotslashlabs.sensay.ui.screen.home.HomeScreen
import com.dotslashlabs.sensay.ui.screen.player.PlayerScreen
import com.dotslashlabs.sensay.ui.screen.settings.SettingsScreen
import com.dotslashlabs.sensay.ui.screen.sources.SourcesScreen
import java.io.File

abstract class Destination(
    vararg routeSegments: String,
    private val params: List<String> = emptyList(),
    val isDialog: Boolean = false,
) {

    val route: String = routeSegments.joinToString(File.separator) + (
            if (params.isEmpty()) ""
            else "?" + params.joinToString(separator = "&") { "${it}={${it}}" })

    abstract val screen: AppScreen?
    abstract val arguments: List<NamedNavArgument>

    abstract val children: List<Destination>
    abstract val defaultChild: Destination?

    fun parentRoute() = route.substringBeforeLast(File.separator)

    object Root : Destination("root") {
        override val screen: AppScreen? = null
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = listOf(Home, Player, Sources, Settings)
        override val defaultChild: Destination = Home
    }

    object Home : Destination("home") {
        override val screen: AppScreen = HomeScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null
    }

    object Player : Destination("player", params = listOf("bookId", "chapterIndex")) {
        override val screen: AppScreen = PlayerScreen
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument("bookId") {
                type = NavType.LongType
                nullable = false
            },
            navArgument("chapterIndex") {
                type = NavType.IntType
                nullable = false
                defaultValue = 0
            },
        )

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null

        fun useRoute(bookId: Long, chapterIndex: Int = 0) =
            this.route.replace("{bookId}", bookId.toString())
                .replace("{chapterIndex}", chapterIndex.toString())

        fun useRoute(extras: Bundle) =
            this.route.replace("{bookId}", extras.getLong("bookId").toString())
                .replace("{chapterIndex}", extras.getInt("chapterIndex").toString())
    }

    object Sources : Destination("sources") {
        override val screen: AppScreen = SourcesScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null
    }

    object Settings : Destination("settings") {
        override val screen: AppScreen = SettingsScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null
    }
}
