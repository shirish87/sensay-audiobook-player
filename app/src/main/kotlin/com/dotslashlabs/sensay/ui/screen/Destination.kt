package com.dotslashlabs.sensay.ui.screen

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dotslashlabs.sensay.ui.screen.book.BookScreen
import com.dotslashlabs.sensay.ui.screen.book.player.PlayerScreen
import com.dotslashlabs.sensay.ui.screen.home.HomeScreen
import com.dotslashlabs.sensay.ui.screen.home.current.CurrentScreen
import com.dotslashlabs.sensay.ui.screen.home.library.LibraryScreen
import com.dotslashlabs.sensay.ui.screen.settings.SettingsScreen
import com.dotslashlabs.sensay.ui.screen.sources.SourcesScreen
import java.io.File

abstract class Destination(vararg routeSegments: String) {
    val route: String = routeSegments.joinToString(File.separator)

    abstract val screen: SensayScreen?
    abstract val arguments: List<NamedNavArgument>

    abstract val children: List<Destination>
    abstract val defaultChild: Destination?

    fun parentRoute() = route.substringBeforeLast(File.separator)

    object Root : Destination("root") {
        override val screen: SensayScreen? = null
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = listOf(Home, Book, Sources, Settings)
        override val defaultChild: Destination = Home
    }

    object Home : Destination("home") {
        override val screen: SensayScreen = HomeScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = listOf(Current, Library)
        override val defaultChild: Destination = Current

        object Current : Destination(this.route, "current") {
            override val screen: SensayScreen = CurrentScreen
            override val arguments: List<NamedNavArgument> = emptyList()

            override val children: List<Destination> = emptyList()
            override val defaultChild: Destination? = null
        }

        object Library : Destination(this.route, "library") {
            override val screen: SensayScreen = LibraryScreen
            override val arguments: List<NamedNavArgument> = emptyList()

            override val children: List<Destination> = emptyList()
            override val defaultChild: Destination? = null
        }
    }

    object Book : Destination("books", "{bookId}") {
        override val screen: SensayScreen = BookScreen
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument("bookId") {
                type = NavType.LongType
                nullable = false
            }
        )

        override val children: List<Destination> = listOf(Player)
        override val defaultChild: Destination = Player

        object Player : Destination(this.route, "player") {
            override val screen: SensayScreen = PlayerScreen
            override val arguments: List<NamedNavArgument> = emptyList()

            override val children: List<Destination> = emptyList()
            override val defaultChild: Destination? = null

            fun useRoute(bookId: Long) = this.route.replace("{bookId}", "$bookId")
        }
    }

    object Settings : Destination("settings") {
        override val screen: SensayScreen = SettingsScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null
    }

    object Sources : Destination("sources") {
        override val screen: SensayScreen = SourcesScreen
        override val arguments: List<NamedNavArgument> = emptyList()

        override val children: List<Destination> = emptyList()
        override val defaultChild: Destination? = null
    }
}
