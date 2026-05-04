package com.example.aasha.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.R
import com.example.aasha.ui.components.AashaButton
import com.example.aasha.ui.components.AashaButtonStyle
import com.example.aasha.ui.components.AashaTextField
import com.example.aasha.viewmodel.LoginUiState
import com.example.aasha.viewmodel.LoginViewModel

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

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.RegistrationSuccess -> screenMode = ScreenMode.SetupMpin
            is LoginUiState.MPinSetupSuccess -> {
                Toast.makeText(context, context.getString(R.string.account_setup_success), Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, (uiState as LoginUiState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = when (screenMode) {
                ScreenMode.Selection -> stringResource(R.string.welcome_back)
                ScreenMode.Login -> stringResource(R.string.worker_login)
                ScreenMode.Register -> stringResource(R.string.create_account)
                ScreenMode.SetupMpin -> stringResource(R.string.setup_secure_mpin)
                ScreenMode.LoginMpin -> stringResource(R.string.enter_mpin)
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        when (screenMode) {
            ScreenMode.Selection -> SelectionMode(
                viewModel = viewModel,
                onModeChange = { screenMode = it }
            )
            ScreenMode.Login -> LoginMode(
                uiState = uiState,
                onLogin = { id, pw -> viewModel.loginWithAashaId(id, pw) },
                onModeChange = { screenMode = it }
            )
            ScreenMode.Register -> RegisterMode(
                uiState = uiState,
                onRegister = { n, id, e, p, l -> viewModel.signUp(n, id, e, p, l) },
                onModeChange = { screenMode = it }
            )
            ScreenMode.SetupMpin -> MpinMode(
                uiState = uiState,
                title = stringResource(R.string.mpin_setup_msg),
                buttonText = stringResource(R.string.set_mpin),
                onSubmit = { viewModel.setupMPin(it) }
            )
            ScreenMode.LoginMpin -> MpinMode(
                uiState = uiState,
                title = stringResource(R.string.mpin_entry_msg),
                buttonText = stringResource(R.string.unlock_app),
                onSubmit = { viewModel.loginWithMPin(it) },
                onModeChange = { screenMode = it },
                onReset = { viewModel.resetToIdle() }
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SelectionMode(
    viewModel: LoginViewModel,
    onModeChange: (ScreenMode) -> Unit
) {
    val hasMpin by viewModel.hasMpin.collectAsState()
    AashaButton(
        text = stringResource(R.string.login_with_mpin),
        onClick = { onModeChange(ScreenMode.LoginMpin) },
        enabled = hasMpin,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    AashaButton(
        text = stringResource(R.string.login_with_aasha_id),
        onClick = { onModeChange(ScreenMode.Login) },
        style = AashaButtonStyle.Secondary,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(40.dp))
    TextButton(onClick = { onModeChange(ScreenMode.Register) }) {
        Text(
            text = stringResource(R.string.new_user_signup),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ColumnScope.LoginMode(
    uiState: LoginUiState,
    onLogin: (String, String) -> Unit,
    onModeChange: (ScreenMode) -> Unit
) {
    var aashaId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var aashaIdError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val idEmptyError = stringResource(R.string.id_cannot_empty)
    val passwordEmptyError = stringResource(R.string.password_cannot_empty)

    AashaTextField(
        value = aashaId,
        onValueChange = { aashaId = it; aashaIdError = null },
        label = stringResource(R.string.aasha_id),
        isError = aashaIdError != null,
        modifier = Modifier.fillMaxWidth()
    )
    if (aashaIdError != null) {
        Text(text = aashaIdError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp).align(Alignment.Start))
    }
    Spacer(modifier = Modifier.height(20.dp))
    AashaTextField(
        value = password,
        onValueChange = { password = it; passwordError = null },
        label = stringResource(R.string.password),
        isError = passwordError != null,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    if (passwordError != null) {
        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp).align(Alignment.Start))
    }
    Spacer(modifier = Modifier.height(40.dp))
    AashaButton(
        text = stringResource(R.string.login),
        onClick = {
            if (aashaId.isBlank()) aashaIdError = idEmptyError
            if (password.isBlank()) passwordError = passwordEmptyError
            if (aashaId.isNotBlank() && password.isNotBlank()) onLogin(aashaId, password)
        },
        enabled = uiState !is LoginUiState.Loading,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = { onModeChange(ScreenMode.Register) }) {
        Text(stringResource(R.string.new_user_signup), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RegisterMode(
    uiState: LoginUiState,
    onRegister: (String, String, String, String, String) -> Unit,
    onModeChange: (ScreenMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var aashaId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }

    AashaTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.full_name), modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    AashaTextField(value = aashaId, onValueChange = { aashaId = it }, label = stringResource(R.string.aasha_id), modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    AashaTextField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(R.string.email_address),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    AashaTextField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.set_password),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    AashaTextField(value = locality, onValueChange = { locality = it }, label = stringResource(R.string.serving_area), modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(40.dp))
    AashaButton(
        text = stringResource(R.string.signup),
        onClick = { onRegister(name, aashaId, email, password, locality) },
        enabled = uiState !is LoginUiState.Loading,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = { onModeChange(ScreenMode.Login) }) {
        Text(stringResource(R.string.already_have_account), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MpinMode(
    uiState: LoginUiState,
    title: String,
    buttonText: String,
    onSubmit: (String) -> Unit,
    onModeChange: ((ScreenMode) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    var mPin by remember { mutableStateOf("") }
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 32.dp)
    )
    
    AashaTextField(
        value = mPin,
        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) mPin = it },
        label = stringResource(R.string.mpin_label),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.width(200.dp) // Focused size for PIN
    )
    
    Spacer(modifier = Modifier.height(48.dp))
    AashaButton(
        text = buttonText,
        onClick = { if (mPin.length == 4) onSubmit(mPin) },
        enabled = uiState !is LoginUiState.Loading && mPin.length == 4,
        modifier = Modifier.fillMaxWidth()
    )
    if (onModeChange != null) {
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { onReset?.invoke(); onModeChange(ScreenMode.Login) }) {
            Text(stringResource(R.string.use_id_instead), fontWeight = FontWeight.Bold)
        }
    }
}
