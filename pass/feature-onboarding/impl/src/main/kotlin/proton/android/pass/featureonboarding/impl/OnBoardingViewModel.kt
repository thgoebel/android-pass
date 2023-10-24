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

package proton.android.pass.featureonboarding.impl

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import proton.android.pass.autofill.api.AutofillManager
import proton.android.pass.autofill.api.AutofillStatus
import proton.android.pass.autofill.api.AutofillSupportedStatus
import proton.android.pass.biometry.BiometryAuthError
import proton.android.pass.biometry.BiometryManager
import proton.android.pass.biometry.BiometryResult
import proton.android.pass.biometry.BiometryStatus
import proton.android.pass.biometry.BiometryType
import proton.android.pass.commonui.api.ClassHolder
import proton.android.pass.data.api.usecases.ObserveHasConfirmedInvite
import proton.android.pass.data.api.usecases.ObserveUserAccessData
import proton.android.pass.featureonboarding.impl.OnBoardingPageName.Autofill
import proton.android.pass.featureonboarding.impl.OnBoardingPageName.Fingerprint
import proton.android.pass.featureonboarding.impl.OnBoardingPageName.InvitePending
import proton.android.pass.featureonboarding.impl.OnBoardingPageName.Last
import proton.android.pass.featureonboarding.impl.OnBoardingSnackbarMessage.BiometryFailedToAuthenticateError
import proton.android.pass.featureonboarding.impl.OnBoardingSnackbarMessage.BiometryFailedToStartError
import proton.android.pass.featureonboarding.impl.OnBoardingSnackbarMessage.FingerprintLockEnabled
import proton.android.pass.log.api.PassLogger
import proton.android.pass.notifications.api.SnackbarDispatcher
import proton.android.pass.preferences.AppLockState
import proton.android.pass.preferences.AppLockTypePreference
import proton.android.pass.preferences.FeatureFlag
import proton.android.pass.preferences.FeatureFlagsPreferencesRepository
import proton.android.pass.preferences.HasAuthenticated
import proton.android.pass.preferences.HasCompletedOnBoarding
import proton.android.pass.preferences.UserPreferencesRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val autofillManager: AutofillManager,
    private val biometryManager: BiometryManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val ffRepo: FeatureFlagsPreferencesRepository,
    private val observeUserAccessData: ObserveUserAccessData,
    private val observeHasConfirmedInvite: ObserveHasConfirmedInvite
) : ViewModel() {

    private val _onBoardingUiState = MutableStateFlow(OnBoardingUiState.Initial)
    val onBoardingUiState: StateFlow<OnBoardingUiState> = _onBoardingUiState

    init {
        viewModelScope.launch {
            val showInvitePendingAcceptance = async { shouldShowInvitePendingAcceptance() }
            val autofillStatus = async { autofillManager.getAutofillStatus().firstOrNull() }
            val biometryStatus = async { biometryManager.getBiometryStatus() }
            val supportedPages = mutableListOf<OnBoardingPageName>()
            if (showInvitePendingAcceptance.await()) {
                supportedPages.add(InvitePending)
            }
            if (shouldShowAutofill(autofillStatus.await())) {
                supportedPages.add(Autofill)
            }
            if (shouldShowFingerprint(biometryStatus.await())) {
                supportedPages.add(Fingerprint)
            }
            supportedPages.add(Last)
            _onBoardingUiState.update { it.copy(enabledPages = supportedPages.toPersistentList()) }
        }

        viewModelScope.launch {
            observeHasConfirmedInvite()
                .collect { hasConfirmedInvite ->
                    if (hasConfirmedInvite) {
                        _onBoardingUiState.update {
                            it.copy(event = OnboardingEvent.ConfirmedInvite)
                        }
                        observeHasConfirmedInvite.clear()
                    }
                }
        }

    }

    private suspend fun shouldShowInvitePendingAcceptance(): Boolean {
        val isNewUserInviteEnabled = ffRepo.get<Boolean>(FeatureFlag.SHARING_NEW_USERS).first()
        if (!isNewUserInviteEnabled) {
            return false
        }

        val userAccessData = runCatching {
            observeUserAccessData().filterNotNull().first()
        }.getOrElse {
            PassLogger.w(TAG, it, "Error getting user access data")
            return false
        }
        return userAccessData.waitingNewUserInvites > 0
    }

    private fun shouldShowAutofill(autofillStatus: AutofillSupportedStatus?): Boolean =
        when (autofillStatus) {
            is AutofillSupportedStatus.Supported -> autofillStatus.status != AutofillStatus.EnabledByOurService
            AutofillSupportedStatus.Unsupported -> false
            else -> false
        }

    private fun shouldShowFingerprint(biometryStatus: BiometryStatus): Boolean =
        when (biometryStatus) {
            BiometryStatus.CanAuthenticate -> true
            BiometryStatus.NotAvailable,
            BiometryStatus.NotEnrolled -> false
        }

    fun onMainButtonClick(page: OnBoardingPageName, contextHolder: ClassHolder<Context>) {
        when (page) {
            Autofill -> onEnableAutofill()
            Fingerprint -> onEnableFingerprint(contextHolder)
            Last -> onFinishOnBoarding()
            InvitePending -> goToNextPage()
        }
    }

    private fun onFinishOnBoarding() {
        viewModelScope.launch {
            saveOnBoardingCompleteFlag()
        }
    }

    fun onSkipButtonClick(page: OnBoardingPageName) {
        when (page) {
            Autofill -> goToNextPage()
            Fingerprint -> goToNextPage()
            Last -> {}
            InvitePending -> {}
        }
    }

    fun onSelectedPageChanged(page: Int) {
        _onBoardingUiState.update { it.copy(selectedPage = page) }
    }

    fun clearEvent() {
        _onBoardingUiState.update { it.copy(event = OnboardingEvent.Unknown) }
    }

    private fun onEnableAutofill() {
        viewModelScope.launch {
            autofillManager.openAutofillSelector()
            delay(DELAY_AFTER_AUTOFILL_CLICK)
            if (_onBoardingUiState.value.enabledPages.count() > 1) {
                _onBoardingUiState.update { it.copy(selectedPage = 1) }
            }
        }
    }

    private fun onEnableFingerprint(contextHolder: ClassHolder<Context>) {
        viewModelScope.launch {
            biometryManager.launch(contextHolder, BiometryType.CONFIGURE)
                .collect { result ->
                    when (result) {
                        BiometryResult.Success -> onBiometrySuccess()
                        is BiometryResult.Error -> onBiometryError(result)
                        // User can retry
                        BiometryResult.Failed -> {}
                        is BiometryResult.FailedToStart -> onBiometryFailedToStart()
                    }
                    PassLogger.i(TAG, "Biometry result: $result")
                }
        }
    }

    private suspend fun onBiometryFailedToStart() {
        snackbarDispatcher(BiometryFailedToStartError)
    }

    private suspend fun onBiometryError(result: BiometryResult.Error) {
        when (result.cause) {
            // If the user has cancelled it, do nothing
            BiometryAuthError.Canceled -> {}
            BiometryAuthError.UserCanceled -> {}

            else -> snackbarDispatcher(BiometryFailedToAuthenticateError)
        }
    }

    private fun onBiometrySuccess() {
        viewModelScope.launch {
            userPreferencesRepository.setHasAuthenticated(HasAuthenticated.Authenticated)
            userPreferencesRepository.setAppLockTypePreference(AppLockTypePreference.Biometrics)
            userPreferencesRepository.setAppLockState(AppLockState.Enabled)
            snackbarDispatcher(FingerprintLockEnabled)
            _onBoardingUiState.update { it.copy(selectedPage = it.selectedPage + 1) }
        }
    }

    private fun goToNextPage() {
        viewModelScope.launch {
            _onBoardingUiState.update { it.copy(selectedPage = it.selectedPage + 1) }
        }
    }

    private fun saveOnBoardingCompleteFlag() {
        userPreferencesRepository.setHasCompletedOnBoarding(HasCompletedOnBoarding.Completed)
            .onSuccess {
                _onBoardingUiState.update { it.copy(event = OnboardingEvent.OnboardingCompleted) }
            }
            .onFailure {
                PassLogger.e(TAG, it, "Could not save HasCompletedOnBoarding preference")
            }
    }

    companion object {
        private const val TAG = "OnBoardingViewModel"
        private val DELAY_AFTER_AUTOFILL_CLICK = 300.milliseconds
    }
}
