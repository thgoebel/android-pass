/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.featureselectitem.previewproviders

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.datetime.Clock
import proton.android.pass.commonuimodels.api.ItemUiModel
import proton.android.pass.domain.HiddenState
import proton.android.pass.domain.ItemContents
import proton.android.pass.domain.ItemId
import proton.android.pass.domain.ShareId

class SuggestionsPreviewProvider : PreviewParameterProvider<SuggestionsInput> {
    override val values: Sequence<SuggestionsInput>
        get() = sequence {
            for (showUpgrade in listOf(true, false)) {
                yield(
                    SuggestionsInput(
                        items = listOf(
                            item("item 1", "some username"),
                            item("item 2", "other username")
                        ),
                        showUpgradeMessage = showUpgrade,
                        canUpgrade = false
                    )
                )
            }
            yield(
                SuggestionsInput(
                    items = listOf(
                        item("item 1", "some username"),
                        item("item 2", "other username")
                    ),
                    showUpgradeMessage = true,
                    canUpgrade = true
                )
            )
        }


    private fun item(name: String, username: String): ItemUiModel = ItemUiModel(
        id = ItemId(name),
        shareId = ShareId(name),
        contents = ItemContents.Login(
            title = name,
            note = "",
            itemEmail = username,
            password = HiddenState.Concealed(""),
            urls = emptyList(),
            packageInfoSet = emptySet(),
            primaryTotp = HiddenState.Concealed(""),
            customFields = emptyList(),
            passkeys = emptyList()
        ),
        state = 0,
        createTime = Clock.System.now(),
        modificationTime = Clock.System.now(),
        lastAutofillTime = Clock.System.now(),
        isPinned = false
    )
}

data class SuggestionsInput(
    val items: List<ItemUiModel>,
    val showUpgradeMessage: Boolean,
    val canUpgrade: Boolean
)
