package com.dotslashlabs.sensay.ui.screen.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    title: @Composable () -> Unit,
    isBusy: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackPress: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {

    Box {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
            title = title,
            actions = actions,
            navigationIcon = {
                onBackPress?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "",
                        )
                    }
                }
            },
        )

        if (isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            )
        }
    }
}
