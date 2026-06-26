package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.BackHandler
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.PasswordStrength

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    com.example.security.KeepScreenSecure()
    var isSignInTab by remember { mutableStateOf(true) }
    
    // Inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    // Toggle passes Visibility
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Error states and loading
    val signInEmailError by viewModel.signInEmailError.collectAsState()
    val signInPasswordError by viewModel.signInPasswordError.collectAsState()
    val signUpEmailError by viewModel.signUpEmailError.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()

    // Reset validations on tab switch
    LaunchedEffect(isSignInTab) {
        viewModel.clearAuthErrors()
        emailInput = ""
        passwordInput = ""
        confirmPasswordInput = ""
        showPassword = false
        showConfirmPassword = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
        ) {
            // LOGO BLOCK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Convertible Logo",
                    tint = Color.Unspecified, // Retains gradient colors
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "convertible",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRACTICE. TALK. GROW.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextGray,
                    letterSpacing = 1.sp
                )
            }

            // AUTH CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
                ) {
                    // TAB ROW
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSignInTab = true }
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sign in",
                                fontSize = 14.sp,
                                fontWeight = if (isSignInTab) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSignInTab) SleekPrimary else SleekTextLightGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (isSignInTab) SleekPrimary else Color.Transparent)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSignInTab = false }
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sign up",
                                fontSize = 14.sp,
                                fontWeight = if (!isSignInTab) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isSignInTab) SleekPrimary else SleekTextLightGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (!isSignInTab) SleekPrimary else Color.Transparent)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = SleekBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // INPUT FORMS
                    if (isSignInTab) {
                        // Email field
                        Text(
                            text = "Email address",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("you@example.com", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        signInEmailError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                color = SleekWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password field
                        Text(
                            text = "Password",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("••••••••", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            trailingIcon = {
                                Text(
                                    text = if (showPassword) "Hide" else "Show",
                                    color = SleekPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showPassword = !showPassword }
                                        .padding(end = 12.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        signInPasswordError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                color = SleekWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Connect Button
                        Button(
                            onClick = {
                                viewModel.signInUser(emailInput, passwordInput)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            enabled = !isLoading
                        ) {
                            Text(
                                text = if (isLoading) "Signing in..." else "Sign in",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W500,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                    } else {
                        // SIGN UP TAB
                        Text(
                            text = "Email address",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("you@example.com", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // Real-time email format validation
                        val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim().lowercase()).matches()
                        if (emailInput.isNotEmpty() && !isEmailValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Invalid email format",
                                fontSize = 11.sp,
                                color = SleekWarning
                            )
                        }
                        signUpEmailError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                color = SleekWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Create password
                        Text(
                            text = "Create a password",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("••••••••", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            trailingIcon = {
                                Text(
                                    text = if (showPassword) "Hide" else "Show",
                                    color = SleekPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showPassword = !showPassword }
                                        .padding(end = 12.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Password strength bar
                        val strength = viewModel.calculatePasswordStrength(passwordInput)
                        if (passwordInput.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Strength:",
                                        fontSize = 11.sp,
                                        color = SleekTextLightGray
                                    )
                                    Text(
                                        text = strength.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (strength) {
                                            PasswordStrength.WEAK -> SleekWarning
                                            PasswordStrength.FAIR, PasswordStrength.GOOD -> SleekWarningAmber
                                            PasswordStrength.STRONG -> SleekSuccess
                                            else -> SleekTextLightGray
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (i in 1..4) {
                                        val barColor = when {
                                            i <= strength.level -> {
                                                when (strength) {
                                                    PasswordStrength.WEAK -> SleekWarning
                                                    PasswordStrength.FAIR, PasswordStrength.GOOD -> SleekWarningAmber
                                                    PasswordStrength.STRONG -> SleekSuccess
                                                    else -> SleekBorder
                                                }
                                            }
                                            else -> SleekBackground
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(barColor)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm password
                        Text(
                            text = "Confirm password",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            placeholder = { Text("••••••••", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            trailingIcon = {
                                Text(
                                    text = if (showConfirmPassword) "Hide" else "Show",
                                    color = SleekPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showConfirmPassword = !showConfirmPassword }
                                        .padding(end = 12.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Inline matching indicator
                        if (passwordInput.isNotEmpty() && confirmPasswordInput.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val isMatched = passwordInput == confirmPasswordInput
                            Text(
                                text = if (isMatched) "✓ Passwords match" else "Passwords don't match",
                                fontSize = 11.sp,
                                color = if (isMatched) SleekSuccess else SleekWarning,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign up Button (Create account)
                        val passwordsMatch = passwordInput == confirmPasswordInput
                        val isPasswordValid = passwordInput.length >= 12 &&
                                passwordInput.any { it.isUpperCase() } &&
                                passwordInput.any { it.isDigit() } &&
                                passwordInput.any { !it.isLetterOrDigit() }
                        val canCreateAccount = passwordsMatch && isPasswordValid && isEmailValid
                        Button(
                            onClick = {
                                viewModel.signUpUser(emailInput, passwordInput)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            enabled = !isLoading && canCreateAccount
                        ) {
                            Text(
                                text = if (isLoading) "Creating account..." else "Create account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W500,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Terms and info line
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "By signing up you agree to our Terms of Service",
                            fontSize = 11.sp,
                            color = SleekTextLightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableStateOf(1) } // 1 or 2 State

    // Input States
    var usernameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }

    val isLoading by viewModel.isProfileLoading.collectAsState()

    BackHandler {
        if (stepIndex == 2) {
            stepIndex = 1
        } else {
            viewModel.signOutUser()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            // STEP PROGRESS DOTS INDICATOR (Exactly two dots)
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot 1
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (stepIndex >= 1) SleekPrimary else SleekBackground)
                        .then(if (stepIndex < 1) Modifier.border(1.dp, SleekBorder, CircleShape) else Modifier)
                )
                // Dot 2
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (stepIndex >= 2) SleekPrimary else SleekBackground)
                        .then(if (stepIndex < 2) Modifier.border(1.dp, SleekBorder, CircleShape) else Modifier)
                )
            }

            // SETUP CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
                ) {
                    if (stepIndex == 1) {
                        // STEP 1: USERNAME
                        Text(
                            text = "Step 1 of 2",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextLightGray,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "What should we call you?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextDark,
                            letterSpacing = (-0.1).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This is your display name in Convertible.",
                            fontSize = 13.sp,
                            color = SleekTextLightGray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Text(
                            text = "Username",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = {
                                if (it.length <= 24) usernameInput = it
                            },
                            placeholder = { Text("e.g. alex_sharma", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "3-24 characters · letters, numbers, underscores only",
                            fontSize = 11.sp,
                            color = SleekTextLightGray
                        )

                        // Real-time validation indicator display
                        val usernameError = viewModel.validateUsername(usernameInput)
                        if (usernameInput.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (usernameError == null) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = SleekSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Looks good!",
                                        fontSize = 11.sp,
                                        color = SleekSuccess
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = SleekWarning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = usernameError,
                                        fontSize = 11.sp,
                                        color = SleekWarning
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { stepIndex = 2 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            enabled = usernameInput.isNotEmpty() && usernameError == null
                        ) {
                            Text(
                                text = "Continue →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W500,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                    } else if (stepIndex == 2) {
                        // STEP 2: AGE & GENDER
                        Text(
                            text = "Step 2 of 2",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextLightGray,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A bit more about you",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextDark,
                            letterSpacing = (-0.1).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This helps us tailor scenarios to feel more realistic for you.",
                            fontSize = 13.sp,
                            color = SleekTextLightGray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Age Field
                        Text(
                            text = "Your age",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = {
                                if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.length <= 2)) ageInput = it
                            },
                            placeholder = { Text("e.g. 21", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekBackground,
                                unfocusedContainerColor = SleekBackground,
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder,
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedPlaceholderColor = SleekTextLightGray,
                                unfocusedPlaceholderColor = SleekTextLightGray
                            ),
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val parsedAge = ageInput.toIntOrNull() ?: 0
                        val isAgeValid = parsedAge in 13..99
                        if (ageInput.isNotEmpty() && !isAgeValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (parsedAge < 13) "You must be at least 13 to use Convertible" else "Please enter a valid age",
                                fontSize = 11.sp,
                                color = SleekWarning
                            )
                        }

                        // Gender field
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Gender",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = SleekTextGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Wrap option pills in Row flow (Male, Female, Non-binary, Prefer not to say)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf("Male", "Female", "Non-binary", "Prefer not to say")
                            options.forEach { option ->
                                val isSelected = selectedGender == option
                                Surface(
                                    modifier = Modifier
                                        .clickable { selectedGender = option }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) SleekPrimary.copy(alpha = 0.4f) else SleekBorder
                                    ),
                                    color = if (isSelected) SleekPrimaryLight else SleekBackground,
                                    contentColor = if (isSelected) SleekPrimary else SleekTextGray
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your age and gender are never shown publicly.",
                            fontSize = 11.sp,
                            color = SleekTextLightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Finish Button
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (isAgeValid && selectedGender.isNotEmpty()) {
                                    viewModel.finishProfileSetup(usernameInput, parsedAge, selectedGender)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            enabled = !isLoading && isAgeValid && selectedGender.isNotEmpty()
                        ) {
                            Text(
                                text = if (isLoading) "Setting up your profile..." else "Finish setup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W500,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
