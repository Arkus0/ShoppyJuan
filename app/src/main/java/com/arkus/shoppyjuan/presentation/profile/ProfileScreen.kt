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
import com.arkus.shoppyjuan.presentation.components.AboutDialog
import com.arkus.shoppyjuan.presentation.components.ChangePasswordDialog
import com.arkus.shoppyjuan.presentation.components.EditProfileDialog
import com.arkus.shoppyjuan.presentation.components.FeedbackDialog
import com.arkus.shoppyjuan.presentation.components.LanguageSettingsDialog
import com.arkus.shoppyjuan.presentation.components.NotificationsSettingsDialog
import com.arkus.shoppyjuan.presentation.components.ProfileOptionCard
import com.arkus.shoppyjuan.presentation.components.RadiusSettingsDialog
import com.arkus.shoppyjuan.presentation.components.ThemeSettingsDialog

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
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Feedback sent snackbar
    LaunchedEffect(uiState.feedbackSent) {
        if (uiState.feedbackSent) {
            snackbarHostState.showSnackbar("Gracias por tu feedback!")
            viewModel.clearFeedbackSent()
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showFeedbackDialog = true },
                icon = { Icon(Icons.Default.Feedback, contentDescription = null) },
                text = { Text("Feedback") }
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

        // Feedback Dialog
        if (showFeedbackDialog) {
            FeedbackDialog(
                onDismiss = { showFeedbackDialog = false },
                onSubmit = { type, rating, description ->
                    viewModel.sendFeedback(type, rating, description)
                    showFeedbackDialog = false
                }
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
