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

val MaterialIcons.Title: ImageVector
    get() {
        if (_title != null) {
            return _title!!
        }
        _title = materialIcon(name = "Outlined.Title") {
            materialPath {
                moveTo(5.0f, 4.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(5.5f)
                verticalLineToRelative(12.0f)
                horizontalLineToRelative(3.0f)
                verticalLineTo(7.0f)
                horizontalLineTo(19.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(5.0f)
                close()
            }
        }
        return _title!!
    }

private var _title: ImageVector? = null
