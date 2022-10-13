package me.proton.core.pass.autofill.ui.autofill.select

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.pass.autofill.entities.AutofillItem
import me.proton.core.pass.domain.entity.PackageName

const val SELECT_ITEM_ROUTE = "autofill/item"

@Composable
fun SelectItemScreen(
    modifier: Modifier = Modifier,
    packageName: PackageName,
    onItemSelected: (AutofillItem) -> Unit
) {
    val viewModel: SelectItemViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    SelectItemScreenContent(
        modifier = modifier,
        state = uiState,
        onItemClicked = { viewModel.onItemClicked(it, packageName) },
        onItemSelected = onItemSelected
    )
}
