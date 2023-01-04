package me.proton.pass.presentation.detail.login.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.collections.immutable.persistentListOf
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItem
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemIcon
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemList
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemTitle
import me.proton.core.compose.theme.ProtonTheme
import me.proton.pass.commonui.api.ThemePreviewProvider
import me.proton.pass.presentation.R

@Composable
fun LoginDetailBottomSheetContents(
    modifier: Modifier = Modifier,
    website: String,
    onCopyToClipboard: (String) -> Unit,
    onOpenWebsite: (String) -> Unit
) {
    Column(modifier) {
        BottomSheetItemList(
            items = persistentListOf(
                openWebsite(onClick = { onOpenWebsite(website) }),
                copyWebsite(onClick = { onCopyToClipboard(website) })
            )
        )
    }
}

private fun openWebsite(onClick: () -> Unit): BottomSheetItem =
    object : BottomSheetItem {
        override val title: @Composable () -> Unit
            get() = { BottomSheetItemTitle(text = stringResource(id = R.string.action_open)) }
        override val subtitle: @Composable (() -> Unit)?
            get() = null
        override val icon: @Composable (() -> Unit)
            get() = { BottomSheetItemIcon(iconId = me.proton.core.presentation.R.drawable.ic_proton_arrow_out_square) }
        override val onClick: () -> Unit
            get() = { onClick() }
    }

private fun copyWebsite(onClick: () -> Unit): BottomSheetItem =
    object : BottomSheetItem {
        override val title: @Composable () -> Unit
            get() = { BottomSheetItemTitle(text = stringResource(id = R.string.action_copy)) }
        override val subtitle: @Composable (() -> Unit)?
            get() = null
        override val icon: @Composable (() -> Unit)
            get() = { BottomSheetItemIcon(iconId = me.proton.core.presentation.R.drawable.ic_proton_squares) }
        override val onClick: () -> Unit
            get() = { onClick() }
    }

@Preview
@Composable
fun LoginDetailBottomSheetContentsPreview(
    @PreviewParameter(ThemePreviewProvider::class) isDark: Boolean
) {
    ProtonTheme(isDark = isDark) {
        Surface {
            LoginDetailBottomSheetContents(
                website = "https://example.local",
                onCopyToClipboard = {},
                onOpenWebsite = {}
            )
        }
    }
}
