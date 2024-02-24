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

val MaterialIcons.NotStarted: ImageVector
    get() {
        if (_notStarted != null) {
            return _notStarted!!
        }
        _notStarted = materialIcon(name = "Outlined.NotStarted") {
            materialPath {
                moveTo(12.0f, 4.0f)
                curveToRelative(4.41f, 0.0f, 8.0f, 3.59f, 8.0f, 8.0f)
                reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
                reflectiveCurveToRelative(-8.0f, -3.59f, -8.0f, -8.0f)
                reflectiveCurveTo(7.59f, 4.0f, 12.0f, 4.0f)
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                curveToRelative(0.0f, 5.52f, 4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
                lineTo(12.0f, 2.0f)
                close()
                moveTo(11.0f, 8.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(17.0f, 12.0f)
                lineToRelative(-5.0f, -4.0f)
                verticalLineToRelative(8.0f)
                lineTo(17.0f, 12.0f)
                close()
            }
        }
        return _notStarted!!
    }

private var _notStarted: ImageVector? = null

