package com.arkus.shoppyjuan.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arkus.shoppyjuan.R
import com.arkus.shoppyjuan.domain.settings.AppLanguage
import com.arkus.shoppyjuan.domain.settings.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRadiusDialog by remember { mutableStateOf(false) }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Password change success
    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            snackbarHostState.showSnackbar("Contrasena actualizada correctamente")
            viewModel.clearPasswordChangeSuccess()
        }
    }

    // Handle sign out
    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading && uiState.profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.profile?.let { profile ->
                    // Profile header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 48.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile options
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CUENTA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Edit,
                            title = "Editar perfil",
                            subtitle = "Cambia tu nombre y foto",
                            onClick = { viewModel.startEditing() }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Lock,
                            title = "Cambiar contrasena",
                            subtitle = "Actualiza tu contrasena",
                            onClick = { viewModel.startChangingPassword() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "APLICACION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Notifications,
                            title = "Notificaciones",
                            subtitle = "Gestiona tus notificaciones",
                            onClick = { showNotificationsDialog = true }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.theme),
                            subtitle = when (uiState.theme) {
                                AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                AppTheme.DARK -> stringResource(R.string.theme_dark)
                                AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                            },
                            onClick = { showThemeDialog = true }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.language),
                            subtitle = uiState.language.displayName,
                            onClick = { showLanguageDialog = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.location_settings).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.MyLocation,
                            title = stringResource(R.string.search_radius),
                            subtitle = stringResource(R.string.search_radius_desc, uiState.searchRadiusKm),
                            onClick = { showRadiusDialog = true }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.about),
                            subtitle = stringResource(R.string.version) + " 1.0.0",
                            onClick = { showAboutDialog = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign out button
                        OutlinedButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar sesion")
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // Edit profile dialog
        if (uiState.isEditing) {
            EditProfileDialog(
                currentName = uiState.profile?.name ?: "",
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.cancelEditing() },
                onSave = { newName -> viewModel.updateProfile(newName) }
            )
        }

        // Change password dialog
        if (uiState.isChangingPassword) {
            ChangePasswordDialog(
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.cancelChangingPassword() },
                onChangePassword = { current, new -> viewModel.changePassword(current, new) },
                onSendResetEmail = { viewModel.sendPasswordResetEmail() }
            )
        }

        // Sign out confirmation dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Cerrar sesion") },
                text = { Text("Estas seguro de que quieres cerrar sesion?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            showSignOutDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cerrar sesion")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // About dialog
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        // Notifications dialog
        if (showNotificationsDialog) {
            NotificationsSettingsDialog(onDismiss = { showNotificationsDialog = false })
        }

        // Theme dialog
        if (showThemeDialog) {
            ThemeSettingsDialog(
                currentTheme = uiState.theme,
                onThemeSelected = { theme ->
                    viewModel.updateTheme(theme)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Language dialog
        if (showLanguageDialog) {
            LanguageSettingsDialog(
                currentLanguage = uiState.language,
                onLanguageSelected = { language ->
                    viewModel.updateLanguage(language)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Radius dialog
        if (showRadiusDialog) {
            RadiusSettingsDialog(
                currentRadiusKm = uiState.searchRadiusKm,
                onRadiusChanged = { radius ->
                    viewModel.updateSearchRadius(radius)
                    showRadiusDialog = false
                },
                onDismiss = { showRadiusDialog = false }
            )
        }
    }
}

@Composable
fun ProfileOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Editar perfil") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onSendResetEmail: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword
    val isValid = currentPassword.isNotBlank() && newPassword.length >= 6 && passwordsMatch

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Cambiar contrasena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Contrasena actual") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva contrasena") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    supportingText = {
                        if (newPassword.isNotEmpty() && newPassword.length < 6) {
                            Text("Minimo 6 caracteres", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contrasena") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                            Text("Las contrasenas no coinciden", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        onSendResetEmail()
                        onDismiss()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enviar email de recuperacion")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChangePassword(currentPassword, newPassword) },
                enabled = isValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Cambiar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("ShoppyJuan")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu lista de compras colaborativa",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Desarrollado con Kotlin + Jetpack Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "2026 Arkus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun NotificationsSettingsDialog(onDismiss: () -> Unit) {
    var pushEnabled by remember { mutableStateOf(true) }
    var itemAddedEnabled by remember { mutableStateOf(true) }
    var listSharedEnabled by remember { mutableStateOf(true) }
    var noteAddedEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notificaciones") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notificaciones push")
                    Switch(checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Tipos de notificaciones",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Articulo anadido", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = itemAddedEnabled,
                        onCheckedChange = { itemAddedEnabled = it },
                        enabled = pushEnabled
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lista compartida", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = listSharedEnabled,
                        onCheckedChange = { listSharedEnabled = it },
                        enabled = pushEnabled
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nota anadida", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = noteAddedEnabled,
                        onCheckedChange = { noteAddedEnabled = it },
                        enabled = pushEnabled
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ThemeSettingsDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = theme }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                                AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                AppTheme.DARK -> stringResource(R.string.theme_dark)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeSelected(selectedTheme) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LanguageSettingsDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = language }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == language,
                            onClick = { selectedLanguage = language }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onLanguageSelected(selectedLanguage) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RadiusSettingsDialog(
    currentRadiusKm: Int,
    onRadiusChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentRadiusKm.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_radius)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.km_format, sliderValue.toInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..50f,
                    steps = 48,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 km", style = MaterialTheme.typography.bodySmall)
                    Text("50 km", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.search_radius_desc, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onRadiusChanged(sliderValue.toInt()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
