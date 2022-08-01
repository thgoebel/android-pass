package me.proton.android.pass.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import me.proton.core.pass.domain.ItemId
import me.proton.core.pass.domain.ShareId

sealed class NavItem(
    val baseRoute: String,
    val navArgs: List<NavArg> = emptyList()
) {
    val route = run {
        val argKeys = navArgs.map { "{${it.key}}" }
        listOf(baseRoute).plus(argKeys).joinToString("/")
    }

    val args = navArgs.map {
        navArgument(it.key) { type = it.navType }
    }

    object Launcher : NavItem("auth")
    object CreateLogin : NavItem("createLogin", listOf(NavArg.ShareId)) {
        fun createNavRoute(shareId: ShareId) = "$baseRoute/${shareId.id}"
    }
    object ViewItem : NavItem("viewItem", listOf(NavArg.ShareId, NavArg.ItemId)) {
        fun createNavRoute(shareId: ShareId, itemId: ItemId) = "$baseRoute/${shareId.id}/${itemId.id}"
    }
    object EditLogin : NavItem("editLogin", listOf(NavArg.ItemId)) {
        fun createNavRoute(itemId: String?) = "$baseRoute/$itemId"
    }
}

enum class NavArg(val key: String, val navType: NavType<*>) {
    ItemId("itemId", NavType.StringType),
    ShareId("shareId", NavType.StringType),
}
