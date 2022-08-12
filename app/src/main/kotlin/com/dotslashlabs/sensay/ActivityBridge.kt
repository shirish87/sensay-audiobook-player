package com.dotslashlabs.sensay

import androidx.media3.session.MediaController

interface ActivityBridge {

    val mediaController: () -> MediaController?
}
