package me.proton.pass.presentation.create.alias

import androidx.compose.runtime.Immutable
import me.proton.pass.domain.ShareId
import me.proton.pass.presentation.uievents.AliasDraftSavedState
import me.proton.pass.presentation.uievents.AliasSavedState
import me.proton.pass.presentation.uievents.IsButtonEnabled
import me.proton.android.pass.composecomponents.impl.uievents.IsLoadingState

@Immutable
data class CreateUpdateAliasUiState(
    val shareId: ShareId?,
    val aliasItem: AliasItem,
    val isDraft: Boolean,
    val errorList: Set<AliasItemValidationErrors>,
    val isLoadingState: IsLoadingState,
    val isAliasSavedState: AliasSavedState,
    val isAliasDraftSavedState: AliasDraftSavedState,
    val isApplyButtonEnabled: IsButtonEnabled
) {
    companion object {
        val Initial = CreateUpdateAliasUiState(
            shareId = null,
            aliasItem = AliasItem.Empty,
            isDraft = false,
            errorList = emptySet(),
            isLoadingState = IsLoadingState.Loading,
            isAliasSavedState = AliasSavedState.Unknown,
            isAliasDraftSavedState = AliasDraftSavedState.Unknown,
            isApplyButtonEnabled = IsButtonEnabled.Disabled
        )
    }
}
