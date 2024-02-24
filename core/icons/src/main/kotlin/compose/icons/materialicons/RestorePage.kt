/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compose.icons.materialicons

import compose.icons.MaterialIcons
import compose.icons.materialIcon
import compose.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val MaterialIcons.RestorePage: ImageVector
    get() {
        if (_restorePage != null) {
            return _restorePage!!
        }
        _restorePage = materialIcon(name = "Outlined.RestorePage") {
            materialPath {
                moveTo(14.0f, 2.0f)
                lineTo(6.0f, 2.0f)
                curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(4.0f, 20.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                lineTo(18.0f, 22.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(20.0f, 8.0f)
                lineToRelative(-6.0f, -6.0f)
                close()
                moveTo(18.0f, 20.0f)
                lineTo(6.0f, 20.0f)
                lineTo(6.0f, 4.0f)
                horizontalLineToRelative(7.17f)
                lineTo(18.0f, 8.83f)
                lineTo(18.0f, 20.0f)
                close()
                moveTo(8.45f, 10.57f)
                lineTo(7.28f, 9.4f)
                lineTo(7.28f, 13.0f)
                horizontalLineToRelative(3.6f)
                lineToRelative(-1.44f, -1.44f)
                curveToRelative(0.52f, -1.01f, 1.58f, -1.71f, 2.79f, -1.71f)
                curveToRelative(1.74f, 0.0f, 3.15f, 1.41f, 3.15f, 3.15f)
                reflectiveCurveToRelative(-1.41f, 3.15f, -3.15f, 3.15f)
                curveToRelative(-1.07f, 0.0f, -2.02f, -0.54f, -2.58f, -1.35f)
                lineTo(8.1f, 14.8f)
                curveToRelative(0.69f, 1.58f, 2.28f, 2.7f, 4.12f, 2.7f)
                curveToRelative(2.48f, 0.0f, 4.5f, -2.02f, 4.5f, -4.5f)
                reflectiveCurveToRelative(-2.02f, -4.5f, -4.5f, -4.5f)
                curveToRelative(-1.59f, 0.0f, -2.97f, 0.83f, -3.77f, 2.07f)
                close()
            }
        }
        return _restorePage!!
    }

private var _restorePage: ImageVector? = null

