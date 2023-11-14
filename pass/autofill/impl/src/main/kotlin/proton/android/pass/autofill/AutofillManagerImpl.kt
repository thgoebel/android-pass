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

package proton.android.pass.autofill

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import proton.android.pass.autofill.api.AutofillManager
import proton.android.pass.autofill.api.AutofillStatus
import proton.android.pass.autofill.api.AutofillSupportedStatus
import proton.android.pass.autofill.api.AutofillSupportedStatus.Supported
import proton.android.pass.autofill.api.AutofillSupportedStatus.Unsupported
import proton.android.pass.log.api.PassLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import android.view.autofill.AutofillManager as AndroidAutofillManager

@Singleton
class AutofillManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AutofillManager {

    override fun getAutofillStatus(): Flow<AutofillSupportedStatus> = flow {
        val autofillManager = context.getSystemService(AndroidAutofillManager::class.java)
        when {
            autofillManager == null -> emit(Unsupported)
            !autofillManager.isAutofillSupported -> emit(Unsupported)
            else -> while (currentCoroutineContext().isActive) {
                runCatching {
                    if (autofillManager.hasEnabledAutofillServices()) {
                        emit(Supported(AutofillStatus.EnabledByOurService))
                    } else if (autofillManager.isEnabled) {
                        emit(Supported(AutofillStatus.EnabledByOtherService))
                    } else {
                        emit(Supported(AutofillStatus.Disabled))
                    }
                }.onFailure {
                    PassLogger.w(TAG, it, "Exception while retrieving hasEnabledAutofillServices")
                    emit(Unsupported)
                }
                delay(UPDATE_TIME.inWholeMilliseconds)
            }
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override fun openAutofillSelector() {
        try {
            if (canOpenAutofillSelector()) {
                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                PassLogger.i(TAG, "Could not open autofill selector")
            }
        } catch (e: ActivityNotFoundException) {
            PassLogger.i(TAG, e, "Could not open autofill selector")
        }
    }

    private fun canOpenAutofillSelector(): Boolean {
        val autofillManager: AndroidAutofillManager? = context
            .getSystemService(AndroidAutofillManager::class.java)
        val hasEnabledAutofillServices = autofillManager?.hasEnabledAutofillServices() ?: false
        val isAutofillSupported = autofillManager?.isAutofillSupported ?: false
        return !hasEnabledAutofillServices && isAutofillSupported
    }

    override fun disableAutofill() {
        runCatching {
            val autofillManager = context.getSystemService(AndroidAutofillManager::class.java)
            autofillManager?.disableAutofillServices()
        }.onSuccess {
            PassLogger.i(TAG, "Disabled autofill services")
        }.onFailure {
            PassLogger.w(TAG, it, "Could not disable autofill services")
        }
    }

    companion object {
        private const val TAG = "AutofillManagerImpl"
        private val UPDATE_TIME = 2L.seconds
    }
}
