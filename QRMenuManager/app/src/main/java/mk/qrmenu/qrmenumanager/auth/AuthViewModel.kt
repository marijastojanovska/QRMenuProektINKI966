package mk.qrmenu.qrmenumanager.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.qrmenu.qrmenumanager.R

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (!validateCommon(email, password)) return

        _state.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(
                    t.localizedMessage ?: string(R.string.error_login_failed)
                )
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (!validateCommon(email, password)) return

        if (password != confirmPassword) {
            _state.value = AuthUiState.Error(string(R.string.error_passwords_dont_match))
            return
        }

        _state.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(
                    t.localizedMessage ?: string(R.string.error_register_failed)
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _state.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(
                    t.localizedMessage ?: string(R.string.error_google_sign_in_failed)
                )
            }
        }
    }

    fun onGoogleSignInError(message: String?) {
        _state.value = AuthUiState.Error(
            message ?: string(R.string.error_google_sign_in_failed)
        )
    }

    fun signInWithFacebook(accessToken: String) {
        _state.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val credential = FacebookAuthProvider.getCredential(accessToken)
                auth.signInWithCredential(credential).await()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(
                    t.localizedMessage ?: string(R.string.error_facebook_sign_in_failed)
                )
            }
        }
    }

    fun onFacebookSignInError(message: String?) {
        _state.value = AuthUiState.Error(
            message ?: string(R.string.error_facebook_sign_in_failed)
        )
    }

    fun reset() {
        _state.value = AuthUiState.Idle
    }

    private fun validateCommon(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = AuthUiState.Error(string(R.string.error_email_invalid))
            return false
        }

        if (password.length < 6) {
            _state.value = AuthUiState.Error(string(R.string.error_password_short))
            return false
        }

        return true
    }

    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
