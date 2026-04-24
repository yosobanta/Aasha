package com.example.aasha.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.viewmodel.LoginUiState
import com.example.aasha.viewmodel.LoginViewModel

sealed class ScreenMode {
    object Login : ScreenMode()
    object Register : ScreenMode()
    object SetupMpin : ScreenMode()
    object LoginMpin : ScreenMode()
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    showMpinInitially: Boolean = true,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var screenMode by remember { 
        mutableStateOf<ScreenMode>(
            if (showMpinInitially && viewModel.hasMpin()) ScreenMode.LoginMpin else ScreenMode.Login
        ) 
    }

    // Form states
    var name by remember { mutableStateOf("") }
    var aashaId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var mPin by remember { mutableStateOf("") }
    
    // Error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var aashaIdError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var localityError by remember { mutableStateOf<String?>(null) }
    var mPinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.RegistrationSuccess -> screenMode = ScreenMode.SetupMpin
            is LoginUiState.MPinSetupSuccess -> {
                Toast.makeText(context, "Account and MPIN set successfully!", Toast.LENGTH_SHORT).show()
                onLoginSuccess() // Go to Dashboard immediately after setup
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, (uiState as LoginUiState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Helper functions for validation
    fun validateLogin(): Boolean {
        var isValid = true
        if (aashaId.isBlank()) {
            aashaIdError = "Aasha ID cannot be empty"
            isValid = false
        } else {
            aashaIdError = null
        }

        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else {
            passwordError = null
        }
        return isValid
    }

    fun validateRegistration(): Boolean {
        var isValid = true
        if (name.isBlank()) {
            nameError = "Name cannot be empty"
            isValid = false
        } else {
            nameError = null
        }

        if (aashaId.isBlank()) {
            aashaIdError = "Aasha ID cannot be empty"
            isValid = false
        } else if (aashaId.length < 4) {
            aashaIdError = "Aasha ID must be at least 4 characters"
            isValid = false
        } else {
            aashaIdError = null
        }

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (locality.isBlank()) {
            localityError = "Locality cannot be empty"
            isValid = false
        } else {
            localityError = null
        }
        return isValid
    }

    fun validateMPin(): Boolean {
        if (mPin.length == 4 && mPin.all { it.isDigit() }) {
            mPinError = null
            return true
        } else {
            mPinError = "MPIN must be exactly 4 digits"
            return false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "Aasha",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = when(screenMode) {
                ScreenMode.Login -> "Worker Login"
                ScreenMode.Register -> "Create Account"
                ScreenMode.SetupMpin -> "Setup Secure MPIN"
                ScreenMode.LoginMpin -> "Enter MPIN"
            },
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (screenMode) {
            ScreenMode.Login -> {
                OutlinedTextField(
                    value = aashaId,
                    onValueChange = { 
                        aashaId = it
                        aashaIdError = null
                    },
                    label = { Text("Aasha ID") },
                    isError = aashaIdError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (aashaIdError != null) {
                    Text(text = aashaIdError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    label = { Text("Password") },
                    isError = passwordError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (passwordError != null) {
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (validateLogin()) {
                            viewModel.loginWithAashaId(aashaId, password) 
                        }
                    },
                    enabled = uiState !is LoginUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Login", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    screenMode = ScreenMode.Register 
                    aashaIdError = null
                    passwordError = null
                }) {
                    Text("New User? Sign Up Here")
                }
            }

            ScreenMode.Register -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Full Name") },
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (nameError != null) {
                    Text(text = nameError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = aashaId,
                    onValueChange = { 
                        aashaId = it
                        aashaIdError = null
                    },
                    label = { Text("Aasha ID") },
                    isError = aashaIdError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (aashaIdError != null) {
                    Text(text = aashaIdError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null
                    },
                    label = { Text("Email Address") },
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(text = emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    label = { Text("Set Password") },
                    isError = passwordError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (passwordError != null) {
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = locality,
                    onValueChange = { 
                        locality = it
                        localityError = null
                    },
                    label = { Text("Serving Area / Locality") },
                    isError = localityError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (localityError != null) {
                    Text(text = localityError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (validateRegistration()) {
                            viewModel.signUp(name, aashaId, email, password, locality)
                        }
                    },
                    enabled = uiState !is LoginUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Sign Up", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    screenMode = ScreenMode.Login 
                    nameError = null
                    aashaIdError = null
                    emailError = null
                    localityError = null
                }) {
                    Text("Already have an account? Login")
                }
            }

            ScreenMode.SetupMpin -> {
                Text("Set a 4-digit PIN for quick access", modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = mPin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) mPin = it
                        mPinError = null
                    },
                    label = { Text("4-Digit MPIN") },
                    isError = mPinError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (mPinError != null) {
                    Text(text = mPinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (validateMPin()) {
                            viewModel.setupMPin(mPin) 
                        }
                    },
                    enabled = uiState !is LoginUiState.Loading && mPin.length == 4,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Set MPIN", fontSize = 18.sp)
                    }
                }
            }

            ScreenMode.LoginMpin -> {
                Text("Enter your MPIN to continue", modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = mPin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) mPin = it
                        mPinError = null
                    },
                    label = { Text("MPIN") },
                    isError = mPinError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (mPinError != null) {
                    Text(text = mPinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        if (validateMPin()) {
                            viewModel.loginWithMPin(mPin) 
                        }
                    },
                    enabled = uiState !is LoginUiState.Loading && mPin.length == 4,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Unlock App", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    screenMode = ScreenMode.Login
                    viewModel.resetToIdle()
                    mPinError = null
                }) {
                    Text("Use Aasha ID instead")
                }
                TextButton(onClick = {
                    screenMode = ScreenMode.Register
                    viewModel.resetToIdle()
                    mPinError = null
                }) {
                    Text("New User? Sign Up Here")
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp)) // Bottom spacing for scrollability
    }
}

