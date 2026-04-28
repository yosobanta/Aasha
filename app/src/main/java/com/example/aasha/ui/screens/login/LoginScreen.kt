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
import androidx.compose.ui.res.stringResource
import com.example.aasha.R

sealed class ScreenMode {
    object Selection : ScreenMode()
    object Login : ScreenMode()
    object Register : ScreenMode()
    object SetupMpin : ScreenMode()
    object LoginMpin : ScreenMode()
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    showMpinInitially: Boolean = true,
    initialMode: ScreenMode? = null,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var screenMode by remember {
        mutableStateOf(initialMode ?: if (showMpinInitially) ScreenMode.LoginMpin else ScreenMode.Selection)
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

    val idEmptyError = stringResource(R.string.id_cannot_empty)
    val passwordEmptyError = stringResource(R.string.password_cannot_empty)
    val nameEmptyError = stringResource(R.string.name_cannot_empty)
    val idShortError = stringResource(R.string.id_too_short)
    val emailInvalidError = stringResource(R.string.email_invalid)
    val passwordShortError = stringResource(R.string.password_too_short)
    val localityEmptyError = stringResource(R.string.locality_cannot_empty)
    val mpinInvalidError = stringResource(R.string.mpin_invalid)

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.RegistrationSuccess -> screenMode = ScreenMode.SetupMpin
            is LoginUiState.MPinSetupSuccess -> {
                Toast.makeText(context, context.getString(R.string.account_setup_success), Toast.LENGTH_SHORT).show()
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
            aashaIdError = idEmptyError
            isValid = false
        } else {
            aashaIdError = null
        }

        if (password.isBlank()) {
            passwordError = passwordEmptyError
            isValid = false
        } else {
            passwordError = null
        }
        return isValid
    }

    fun validateRegistration(): Boolean {
        var isValid = true
        if (name.isBlank()) {
            nameError = nameEmptyError
            isValid = false
        } else {
            nameError = null
        }

        if (aashaId.isBlank()) {
            aashaIdError = idEmptyError
            isValid = false
        } else if (aashaId.length < 4) {
            aashaIdError = idShortError
            isValid = false
        } else {
            aashaIdError = null
        }

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = emailInvalidError
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = passwordEmptyError
            isValid = false
        } else if (password.length < 6) {
            passwordError = passwordShortError
            isValid = false
        } else {
            passwordError = null
        }

        if (locality.isBlank()) {
            localityError = localityEmptyError
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
            mPinError = mpinInvalidError
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
            text = stringResource(R.string.app_name),
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = when(screenMode) {
                ScreenMode.Selection -> stringResource(R.string.welcome_back)
                ScreenMode.Login -> stringResource(R.string.worker_login)
                ScreenMode.Register -> stringResource(R.string.create_account)
                ScreenMode.SetupMpin -> stringResource(R.string.setup_secure_mpin)
                ScreenMode.LoginMpin -> stringResource(R.string.enter_mpin)
            },
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (screenMode) {
            ScreenMode.Selection -> {
                val hasMpin by viewModel.hasMpin.collectAsState()
                Button(
                    onClick = { screenMode = ScreenMode.LoginMpin },
                    enabled = hasMpin,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.login_with_mpin), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { screenMode = ScreenMode.Login },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.login_with_aasha_id), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = { screenMode = ScreenMode.Register }) {
                    Text(stringResource(R.string.new_user_signup))
                }
            }

            ScreenMode.Login -> {
                OutlinedTextField(
                    value = aashaId,
                    onValueChange = { 
                        aashaId = it
                        aashaIdError = null
                    },
                    label = { Text(stringResource(R.string.aasha_id)) },
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
                    label = { Text(stringResource(R.string.password)) },
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
                        Text(stringResource(R.string.login), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    screenMode = ScreenMode.Register 
                    aashaIdError = null
                    passwordError = null
                }) {
                    Text(stringResource(R.string.new_user_signup))
                }
            }

            ScreenMode.Register -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.full_name)) },
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
                    label = { Text(stringResource(R.string.aasha_id)) },
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
                    label = { Text(stringResource(R.string.email_address)) },
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
                    label = { Text(stringResource(R.string.set_password)) },
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
                    label = { Text(stringResource(R.string.serving_area)) },
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
                        Text(stringResource(R.string.signup), fontSize = 18.sp)
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
                    Text(stringResource(R.string.already_have_account))
                }
            }

            ScreenMode.SetupMpin -> {
                Text(stringResource(R.string.mpin_setup_msg), modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = mPin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) mPin = it
                        mPinError = null
                    },
                    label = { Text(stringResource(R.string.mpin_label)) },
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
                        Text(stringResource(R.string.set_mpin), fontSize = 18.sp)
                    }
                }
            }

            ScreenMode.LoginMpin -> {
                Text(stringResource(R.string.mpin_entry_msg), modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = mPin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) mPin = it
                        mPinError = null
                    },
                    label = { Text(stringResource(R.string.mpin_label)) },
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
                        Text(stringResource(R.string.unlock_app), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    screenMode = ScreenMode.Login
                    viewModel.resetToIdle()
                    mPinError = null
                }) {
                    Text(stringResource(R.string.use_id_instead))
                }
                TextButton(onClick = {
                    screenMode = ScreenMode.Register
                    viewModel.resetToIdle()
                    mPinError = null
                }) {
                    Text(stringResource(R.string.new_user_signup))
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp)) // Bottom spacing for scrollability
    }
}
