package com.dotslashlabs.sensay.ui.screen.lookup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import lookup.Item

object LookupScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val argsBundle = backStackEntry.arguments ?: return

        val viewModel: LookupViewModel =
            mavericksViewModel(argsFactory = { LookupViewArgs(argsBundle) })

        val searchResultItems by viewModel.collectAsState(LookupViewState::searchResultItems)

        ElevatedCard(
            modifier = Modifier.fillMaxHeight(fraction = 0.9F),
        ) {
            Box(Modifier.fillMaxSize()) {
                when (searchResultItems) {
                    is Success -> {
                        SuccessView((searchResultItems as Success<List<Item>>).invoke())
                    }
                    is Fail -> {
                        ErrorView(
                            (searchResultItems as Fail).error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }

                IconButton(
                    onClick = { navHostController.popBackStack() },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "",
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessView(searchResultItems: List<Item>) {
    val item = searchResultItems.firstNotNullOfOrNull { it.volumeInfo } ?: return

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Attribution(modifier = Modifier.align(Alignment.CenterHorizontally))

        item.imageLinks?.thumbnail?.let { imageLink ->
            CoverImage(
                coverUri = Uri.parse(imageLink.replace("http://", "https://")),
                modifier = Modifier
                    .size(250.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            val authors = item.authors.joinToString(", ")
            if (authors.isNotBlank()) {
                Text(
                    text = authors,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            item.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            item.publishedDate?.let {
                Text(
                    text = "Published on: $it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            item.infoLink?.let { infoLink ->
                val context = LocalContext.current
                val uri = Uri.parse(infoLink)
                val webIntent = Intent(Intent.ACTION_VIEW, uri)

                OutlinedButton(
                    onClick = { context.startActivity(webIntent) },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "",
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Visit ${uri.host}")
                }
            }
            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun Attribution(modifier: Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .wrapContentSize()
            .sizeIn(maxHeight = 20.dp),
    ) {

        Text(
            text = "Powered by",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterVertically)
                .padding(end = 6.dp),
        )
        Image(
            painterResource(R.drawable.google),
            "Powered by Google",
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Composable
fun ErrorView(t: Throwable, modifier: Modifier) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Attribution(modifier = Modifier.align(Alignment.CenterHorizontally))

        Column(modifier = modifier.padding(horizontal = 20.dp)) {
            Text("Error: ${t.message}")
        }
    }
}
