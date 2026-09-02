package com.example.eduhub20.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.ui.theme.EduHubAccentOrange
import com.example.eduhub20.ui.theme.EduHubPrimary
import com.example.eduhub20.R

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Header
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(EduHubPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_school),
                            contentDescription = "EduHub Logo",
                            tint = EduHubPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "EduHub",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Welcome to EduHub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Switcher: Student vs Lecturer
                    TabRow(
                        selectedTabIndex = uiState.selectedRole.ordinal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Tab(
                            selected = uiState.selectedRole == UserRole.STUDENT,
                            onClick = { viewModel.setSelectedRole(UserRole.STUDENT) },
                            text = { Text("Student", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = uiState.selectedRole == UserRole.LECTURER,
                            onClick = { viewModel.setSelectedRole(UserRole.LECTURER) },
                            text = { Text("Lecturer", fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auth Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            if (uiState.selectedRole == UserRole.STUDENT) {
                                // Student Sign In vs Sign Up toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.isSignUpMode) "Create Student Account" else "Student Login",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { viewModel.setSignUpMode(!uiState.isSignUpMode) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isSignUpMode) "Sign In" else "Sign Up",
                                            fontWeight = FontWeight.Bold,
                                            color = EduHubPrimary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Lecturer Portal Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EduHubAccentOrange
                                )
                                Text(
                                    text = "Sign in with your Supabase lecturer account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Error Message Banner (Simple & User Friendly)
                            if (uiState.errorMessage != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_info),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = uiState.errorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Email Field
                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = { viewModel.onEmailChanged(it) },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_email),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                              },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password Field
                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = { viewModel.onPasswordChanged(it) },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_lock),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                              },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                        Icon(
                                            painter = if (uiState.isPasswordVisible) {
                                                painterResource(id = R.drawable.ic_visibility)
                                            } else {
                                                painterResource(id = R.drawable.ic_visibility_off)
                                            },
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    viewModel.submitLogin()
                                }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Remember Me (Left) and Forgot Password (Right) Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.toggleRememberMe(!uiState.rememberMe) }
                                ) {
                                    Checkbox(
                                        checked = uiState.rememberMe,
                                        onCheckedChange = { viewModel.toggleRememberMe(it) },
                                        colors = CheckboxDefaults.colors(checkedColor = EduHubPrimary)
                                    )
                                    Text("Remember me", style = MaterialTheme.typography.bodySmall)
                                }

                                TextButton(
                                    onClick = { viewModel.showForgotPasswordDialog(true) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduHubPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.submitLogin()
                                },
                                enabled = !uiState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.selectedRole == UserRole.LECTURER) EduHubAccentOrange else EduHubPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = if (uiState.selectedRole == UserRole.LECTURER) "Lecturer Sign In" else if (uiState.isSignUpMode) "Create Account" else "Login",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Forgot Password Dialog
        if (uiState.showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showForgotPasswordDialog(false) },
                title = { Text("Reset Password") },
                text = {
                    Column {
                        Text("Enter your email address to receive password recovery instructions:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.forgotPasswordEmail,
                            onValueChange = { viewModel.onForgotPasswordEmailChanged(it) },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.sendPasswordReset() }) {
                        Text("Send Reset Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showForgotPasswordDialog(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}