package mk.qrmenu.qrmenumanager.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

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
                _state.value = AuthUiState.Error(t.localizedMessage ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (!validateCommon(email, password)) return

        if (password != confirmPassword) {
            _state.value = AuthUiState.Error("Passwords don't match")
            return
        }

        _state.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(t.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun reset() {
        _state.value = AuthUiState.Idle
    }

    private fun validateCommon(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = AuthUiState.Error("Invalid email")
            return false
        }

        if (password.length < 6) {
            _state.value = AuthUiState.Error("Password must be at least 6 characters")
            return false
        }

        return true
    }
}
