package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

sealed interface AuthAppState {
    object Checking : AuthAppState
    object AuthScreen : AuthAppState
    object ProfileSetupScreen : AuthAppState
    object OnboardingScreen : AuthAppState
    object MainAppScreen : AuthAppState
}

enum class PasswordStrength(val level: Int, val label: String) {
    EMPTY(0, ""),
    WEAK(1, "Weak"),
    FAIR(2, "Fair"),
    GOOD(3, "Good"),
    STRONG(4, "Strong")
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sharedPrefs = context.getSharedPreferences("conversable_prefs", Context.MODE_PRIVATE)

    private val _appState = MutableStateFlow<AuthAppState>(AuthAppState.Checking)
    val appState: StateFlow<AuthAppState> = _appState.asStateFlow()

    // Auth screen errors
    private val _signInEmailError = MutableStateFlow<String?>(null)
    val signInEmailError: StateFlow<String?> = _signInEmailError.asStateFlow()

    private val _signInPasswordError = MutableStateFlow<String?>(null)
    val signInPasswordError: StateFlow<String?> = _signInPasswordError.asStateFlow()

    private val _signUpEmailError = MutableStateFlow<String?>(null)
    val signUpEmailError: StateFlow<String?> = _signUpEmailError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Session / Profile Data
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userAge = MutableStateFlow(0)
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    private val _userGender = MutableStateFlow("")
    val userGender: StateFlow<String> = _userGender.asStateFlow()

    // Profile Setup Form State
    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        val loggedIn = sharedPrefs.getString("user_logged_in", "") == "true"
        val profileComp = sharedPrefs.getString("profile_complete", "") == "true"
        val rawEmail = sharedPrefs.getString("user_email", "") ?: ""
        val activeEmail = if (rawEmail.isNotEmpty()) {
            com.example.security.CryptoHelper.decrypt(rawEmail) ?: rawEmail
        } else ""

        if (loggedIn && activeEmail.isNotEmpty()) {
            _userEmail.value = activeEmail
            val userKey = activeEmail.lowercase()
            _username.value = sharedPrefs.getString("user_profile_username_$userKey", "") ?: ""
            _userAge.value = sharedPrefs.getInt("user_profile_age_$userKey", 0)
            _userGender.value = sharedPrefs.getString("user_profile_gender_$userKey", "") ?: ""
            
            if (profileComp) {
                val onboardingComplete = sharedPrefs.getString("onboarding_complete", "") == "true"
                if (onboardingComplete) {
                    _appState.value = AuthAppState.MainAppScreen
                } else {
                    _appState.value = AuthAppState.OnboardingScreen
                }
            } else {
                _appState.value = AuthAppState.ProfileSetupScreen
            }
        } else {
            _appState.value = AuthAppState.AuthScreen
        }
    }

    fun signUpUser(email: String, password: String) {
        _signUpEmailError.value = null
        val emailClean = email.trim().lowercase()

        if (emailClean.isEmpty()) {
            _signUpEmailError.value = "Please fill in all fields"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailClean).matches()) {
            _signUpEmailError.value = "Invalid email format"
            return
        }

        if (password.length < 12 ||
            !password.any { it.isUpperCase() } ||
            !password.any { it.isDigit() } ||
            !password.any { !it.isLetterOrDigit() }
        ) {
            _signUpEmailError.value = "Password must be at least 12 characters, with 1 uppercase, 1 number, and 1 symbol"
            return
        }

        val storedHashKey = "user_pwd_$emailClean"
        if (sharedPrefs.contains(storedHashKey)) {
            _signUpEmailError.value = "An account with this email already exists"
            return
        }

        _isAuthLoading.value = true
        val hash = hashPassword(password, emailClean)
        val encryptedEmail = com.example.security.CryptoHelper.encrypt(emailClean)
        
        sharedPrefs.edit()
            .putString(storedHashKey, hash)
            .putString("user_email", encryptedEmail)
            .putString("user_logged_in", "true")
            .putString("profile_complete", "false")
            .apply()

        _userEmail.value = emailClean
        _username.value = ""
        _userAge.value = 0
        _userGender.value = ""
        _isAuthLoading.value = false

        _appState.value = AuthAppState.ProfileSetupScreen
    }

    fun signInUser(email: String, password: String) {
        _signInEmailError.value = null
        _signInPasswordError.value = null

        val emailClean = email.trim().lowercase()

        if (emailClean.isEmpty() && password.isEmpty()) {
            _signInEmailError.value = "Please fill in all fields"
            _signInPasswordError.value = "Please fill in all fields"
            return
        }
        if (emailClean.isEmpty()) {
            _signInEmailError.value = "Please fill in all fields"
            return
        }
        if (password.isEmpty()) {
            _signInPasswordError.value = "Please fill in all fields"
            return
        }

        val storedHashKey = "user_pwd_$emailClean"
        if (!sharedPrefs.contains(storedHashKey)) {
            _signInEmailError.value = "No account with this email. Try signing up."
            return
        }

        val expectedHash = sharedPrefs.getString(storedHashKey, "") ?: ""
        val actualHash = hashPassword(password, emailClean)
        if (expectedHash != actualHash) {
            _signInPasswordError.value = "Incorrect password"
            return
        }

        _isAuthLoading.value = true
        val hasProfile = sharedPrefs.getString("user_profile_complete_$emailClean", "") == "true"
        val encryptedEmail = com.example.security.CryptoHelper.encrypt(emailClean)
        
        sharedPrefs.edit()
            .putString("user_email", encryptedEmail)
            .putString("user_logged_in", "true")
            .putString("profile_complete", if (hasProfile) "true" else "false")
            .putString("onboarding_complete", "true")
            .putBoolean("show_welcome_back_toast", true)
            .apply()

        _userEmail.value = emailClean
        _username.value = sharedPrefs.getString("user_profile_username_$emailClean", "") ?: ""
        _userAge.value = sharedPrefs.getInt("user_profile_age_$emailClean", 0)
        _userGender.value = sharedPrefs.getString("user_profile_gender_$emailClean", "") ?: ""
        _isAuthLoading.value = false

        if (hasProfile) {
            _appState.value = AuthAppState.MainAppScreen
        } else {
            _appState.value = AuthAppState.ProfileSetupScreen
        }
    }

    fun finishProfileSetup(username: String, age: Int, gender: String) {
        val email = _userEmail.value.lowercase()
        if (email.isEmpty()) {
            _appState.value = AuthAppState.AuthScreen
            return
        }

        _isProfileLoading.value = true

        sharedPrefs.edit()
            .putString("user_profile_username_$email", username)
            .putInt("user_profile_age_$email", age)
            .putString("user_profile_gender_$email", gender)
            .putString("user_profile_complete_$email", "true")
            .putString("user_logged_in", "true")
            .putString("profile_complete", "true")
            .apply()

        _username.value = username
        _userAge.value = age
        _userGender.value = gender
        _isProfileLoading.value = false

        val onboardingComplete = sharedPrefs.getString("onboarding_complete", "") == "true"
        if (onboardingComplete) {
            _appState.value = AuthAppState.MainAppScreen
        } else {
            _appState.value = AuthAppState.OnboardingScreen
        }
    }

    fun completeOnboarding(preferredCategory: String?) {
        val editor = sharedPrefs.edit()
        editor.putString("onboarding_complete", "true")
        if (preferredCategory != null) {
            editor.putString("preferred_category", preferredCategory)
        } else {
            editor.remove("preferred_category")
        }
        editor.apply()
        _appState.value = AuthAppState.MainAppScreen
    }

    fun signOutUser() {
        sharedPrefs.edit()
            .remove("user_logged_in")
            .remove("profile_complete")
            .remove("user_email")
            .apply()
        _userEmail.value = ""
        _username.value = ""
        _userAge.value = 0
        _userGender.value = ""
        _appState.value = AuthAppState.AuthScreen
    }

    fun clearAuthErrors() {
        _signInEmailError.value = null
        _signInPasswordError.value = null
        _signUpEmailError.value = null
    }

    // Utility methods
    fun validateUsername(username: String): String? {
        if (username.isEmpty()) return "Please enter a username"
        if (username.length < 3) return "Too short: At least 3 characters"
        if (username.length > 24) return "Too long: Max 24 characters"
        val regex = "^[a-zA-Z0-9_]+$".toRegex()
        if (!regex.matches(username)) return "Bad chars: Only letters, numbers, underscores"
        return null
    }

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        val len = password.length
        if (len < 12) return PasswordStrength.WEAK
        
        val hasUpper = password.any { it.isUpperCase() }
        val hasNumbers = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        
        if (hasUpper && hasNumbers && hasSymbol) {
            return PasswordStrength.STRONG
        }
        if (hasUpper && hasNumbers) {
            return PasswordStrength.GOOD
        }
        return PasswordStrength.FAIR
    }

    private fun hashPassword(password: String, email: String): String {
        return try {
            val salt = email.lowercase().toByteArray(Charsets.UTF_8)
            val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 12000, 256)
            val f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = f.generateSecret(spec).encoded
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Salted SHA-256 backup fallback
            val salt = email.lowercase().toByteArray(Charsets.UTF_8)
            val md = java.security.MessageDigest.getInstance("SHA-256")
            var hash = password.toByteArray(Charsets.UTF_8)
            for (i in 0 until 5000) {
                md.reset()
                md.update(salt)
                md.update(hash)
                hash = md.digest()
            }
            hash.joinToString("") { "%02x".format(it) }
        }
    }
}
