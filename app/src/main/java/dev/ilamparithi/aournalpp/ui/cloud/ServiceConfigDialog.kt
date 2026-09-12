package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.backup.security.GoogleOAuthManager
import dev.ilamparithi.aournalpp.backup.security.NextcloudQrParser
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceConfigDialog(
    initialService: ServiceConfig? = null,
    existingServices: List<ServiceConfig> = emptyList(),
    onDismissRequest: () -> Unit,
    onSaveService: (ServiceConfig) -> Unit
) {
    val context = LocalContext.current
    val serviceKey = initialService?.id ?: "new_service"
    var name by remember(serviceKey) { mutableStateOf(initialService?.name ?: "") }
    var selectedType by remember(serviceKey) { mutableStateOf(initialService?.providerType ?: StorageProviderType.NEXTCLOUD) }
    var serverUrl by remember(serviceKey) { mutableStateOf(initialService?.serverUrl ?: "") }
    var host by remember(serviceKey) { mutableStateOf(initialService?.host ?: "") }
    var port by remember(serviceKey) { mutableIntStateOf(initialService?.port ?: (selectedType.defaultPort ?: 443)) }
    var username by remember(serviceKey) { mutableStateOf(initialService?.username ?: "") }
    var passwordOrSecret by remember(serviceKey) { mutableStateOf(initialService?.passwordOrSecret ?: "") }
    var privateKey by remember(serviceKey) { mutableStateOf(initialService?.privateKey ?: "") }
    var privateKeyPassphrase by remember(serviceKey) { mutableStateOf(initialService?.privateKeyPassphrase ?: "") }
    var authToken by remember(serviceKey) { mutableStateOf(initialService?.authToken ?: "") }
    var refreshToken by remember(serviceKey) { mutableStateOf(initialService?.refreshToken ?: "") }
    var tokenExpiryEpochMs by remember(serviceKey) { mutableStateOf(initialService?.tokenExpiryEpochMs ?: 0L) }
    var accountIdentifier by remember(serviceKey) { mutableStateOf(initialService?.accountIdentifier ?: "") }
    var shareName by remember(serviceKey) { mutableStateOf(initialService?.shareName ?: "") }
    var domain by remember(serviceKey) { mutableStateOf(initialService?.domain ?: "") }
    var remoteBasePath by remember(serviceKey) { mutableStateOf(initialService?.remoteBasePath ?: "") }
    var isFtpsExplicit by remember(serviceKey) { mutableStateOf(initialService?.isFtpsExplicit ?: true) }
    var isFtpsImplicit by remember(serviceKey) { mutableStateOf(initialService?.isFtpsImplicit ?: false) }
    var isCompleteBackupEnabled by remember(serviceKey) { mutableStateOf(initialService?.isCompleteBackupEnabled ?: true) }
    var isEnabled by remember(serviceKey) { mutableStateOf(initialService?.isEnabled ?: true) }

    var isPasswordVisible by remember(serviceKey) { mutableStateOf(false) }
    var sftpAuthMode by remember(serviceKey) { mutableIntStateOf(if (initialService?.privateKey?.isNotBlank() == true) 1 else 0) }

    var isTestingConnection by remember(serviceKey) { mutableStateOf(false) }
    var testResultSuccess by remember(serviceKey) { mutableStateOf<Boolean?>(null) }
    var testResultMessage by remember(serviceKey) { mutableStateOf<String?>(null) }
    var uniquenessErrorMessage by remember(serviceKey) { mutableStateOf<String?>(null) }

    var isTypeDropdownExpanded by remember(serviceKey) { mutableStateOf(false) }
    var showQrScannerDialog by remember(serviceKey) { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(serviceKey) {
        onDispose {
            GoogleOAuthManager.clearAuthResponse()
        }
    }

    val latestOAuthResponse by GoogleOAuthManager.authResponseFlow.collectAsState()
    LaunchedEffect(latestOAuthResponse) {
        latestOAuthResponse?.let { response ->
            if (selectedType == StorageProviderType.GOOGLE_DRIVE) {
                authToken = response.accessToken
                if (!response.refreshToken.isNullOrBlank()) {
                    refreshToken = response.refreshToken
                }
                tokenExpiryEpochMs = System.currentTimeMillis() + (response.expiresInSeconds * 1000L)
                if (!response.userEmail.isNullOrBlank()) {
                    accountIdentifier = response.userEmail
                    if (name.isBlank() || name == "Google Drive") {
                        name = "Google Drive (${response.userEmail})"
                    }
                }
                testResultSuccess = true
                testResultMessage = "Signed in as ${response.userEmail ?: "Google Account"}"
            }
        }
    }


    fun buildCurrentConfig(): ServiceConfig {
        val id = initialService?.id ?: UUID.randomUUID().toString()
        val defaultName = if (name.isBlank()) selectedType.displayName else name.trim()
        return ServiceConfig(
            id = id,
            name = defaultName,
            providerType = selectedType,
            serverUrl = serverUrl.trim(),
            host = host.trim(),
            port = port,
            username = username.trim(),
            passwordOrSecret = passwordOrSecret,
            privateKey = if (sftpAuthMode == 1) privateKey.trim() else "",
            privateKeyPassphrase = if (sftpAuthMode == 1) privateKeyPassphrase else "",
            authToken = authToken.trim(),
            refreshToken = refreshToken.trim(),
            tokenExpiryEpochMs = tokenExpiryEpochMs,
            accountIdentifier = accountIdentifier.trim(),
            shareName = shareName.trim(),
            domain = domain.trim(),
            remoteBasePath = remoteBasePath.trim(),
            isFtpsImplicit = isFtpsImplicit,
            isFtpsExplicit = isFtpsExplicit,
            isCompleteBackupEnabled = isCompleteBackupEnabled,
            isEnabled = isEnabled,
            lastSyncedAtEpochMs = initialService?.lastSyncedAtEpochMs ?: 0L,
            lastSyncStatus = initialService?.lastSyncStatus,
            customMappings = initialService?.customMappings ?: emptyList()
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = if (initialService == null) "Add Cloud Service" else "Configure ${initialService.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Provider Type Selector with clean ordering (Services first, then Protocols)
                ExposedDropdownMenuBox(
                    expanded = isTypeDropdownExpanded,
                    onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Storage Provider / Protocol") },
                        leadingIcon = {
                            CloudProviderIcon(providerType = selectedType, modifier = Modifier.size(24.dp))
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeDropdownExpanded,
                        onDismissRequest = { isTypeDropdownExpanded = false }
                    ) {
                        val orderedTypes = StorageProviderType.getOrderedTypes()
                        var hasRenderedProtocolHeader = false

                        orderedTypes.forEach { type ->
                            if (!type.isDedicatedService && !hasRenderedProtocolHeader) {
                                hasRenderedProtocolHeader = true
                                Text(
                                    text = "Standard Storage Protocols",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                leadingIcon = {
                                    CloudProviderIcon(providerType = type, modifier = Modifier.size(22.dp))
                                },
                                onClick = {
                                    selectedType = type
                                    port = type.defaultPort ?: 443
                                    if (name.isBlank() || StorageProviderType.entries.any { it.displayName == name }) {
                                        name = type.displayName
                                    }
                                    isTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Service Nickname
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    placeholder = { Text(selectedType.displayName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider specific form fields
                when (selectedType) {
                    StorageProviderType.NEXTCLOUD -> {
                        // QR Code Scanner Action Card
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("App Password QR Code", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Scan Nextcloud Security settings QR to auto-fill credentials", style = MaterialTheme.typography.bodySmall)
                                }
                                FilledTonalButton(onClick = { showQrScannerDialog = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan QR")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Nextcloud Server URL") },
                            placeholder = { Text("https://cloud.example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = passwordOrSecret,
                            onValueChange = { passwordOrSecret = it },
                            label = { Text("Password or App Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StorageProviderType.WEBDAV -> {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("WebDAV Server URL") },
                            placeholder = { Text("https://dav.example.com/remote.php/webdav") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username (Optional if Bearer Token used)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = passwordOrSecret,
                            onValueChange = { passwordOrSecret = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = authToken,
                            onValueChange = { authToken = it },
                            label = { Text("Bearer Auth Token (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StorageProviderType.GOOGLE_DRIVE -> {
                        val isGoogleLoggedIn = authToken.isNotBlank() || refreshToken.isNotBlank() || accountIdentifier.isNotBlank()
                        val isAuthFailed = (initialService?.lastSyncStatus != null && (
                            initialService.lastSyncStatus.contains("401") ||
                            initialService.lastSyncStatus.contains("auth", ignoreCase = true) ||
                            initialService.lastSyncStatus.contains("UNAUTHENTICATED", ignoreCase = true) ||
                            initialService.lastSyncStatus.contains("invalid_grant", ignoreCase = true)
                        )) || (testResultSuccess == false && testResultMessage != null && (
                            testResultMessage?.contains("401") == true ||
                            testResultMessage?.contains("auth", ignoreCase = true) == true ||
                            testResultMessage?.contains("UNAUTHENTICATED", ignoreCase = true) == true
                        ))
                        val isTokenExpired = isGoogleLoggedIn && isAuthFailed

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                !isGoogleLoggedIn -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                isTokenExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                !isGoogleLoggedIn -> Icons.Default.AccountCircle
                                                isTokenExpired -> Icons.Default.ErrorOutline
                                                else -> Icons.Default.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                !isGoogleLoggedIn -> MaterialTheme.colorScheme.secondary
                                                isTokenExpired -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = when {
                                                    !isGoogleLoggedIn -> "Google OAuth2 Sign-In"
                                                    isTokenExpired -> "Google Token Expired"
                                                    else -> "Google Account Connected"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (isGoogleLoggedIn && accountIdentifier.isNotBlank()) {
                                                Text(
                                                    text = accountIdentifier,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isGoogleLoggedIn) {
                                        OutlinedButton(
                                            onClick = { GoogleOAuthManager.startOAuthFlow(context) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isTokenExpired) "Renew Sign-In" else "Switch Account")
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { GoogleOAuthManager.startOAuthFlow(context) }
                                        ) {
                                            Text("Sign in with Google")
                                        }
                                    }
                                }

                                if (!isGoogleLoggedIn) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap above to authorize access to your Google Drive backup files.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else if (isTokenExpired) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Your authorization token has expired. Tap \"Renew Sign-In\" to restore connection.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Hide manual token entry when logged in via OAuth
                        if (!isGoogleLoggedIn) {
                            OutlinedTextField(
                                value = authToken.ifBlank { passwordOrSecret },
                                onValueChange = {
                                    authToken = it
                                    passwordOrSecret = it
                                },
                                label = { Text("Access Token / Token String") },
                                placeholder = { Text("ya29.a0...") },
                                supportingText = { Text("Sign in above or paste an OAuth token") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    StorageProviderType.SFTP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Host") },
                                placeholder = { Text("192.168.1.100") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                            OutlinedTextField(
                                value = if (port == 0) "" else port.toString(),
                                onValueChange = { port = it.toIntOrNull() ?: 22 },
                                label = { Text("Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.3f)
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = sftpAuthMode == 0,
                                onClick = { sftpAuthMode = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Password")
                            }
                            SegmentedButton(
                                selected = sftpAuthMode == 1,
                                onClick = { sftpAuthMode = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("SSH Key")
                            }
                        }

                        if (sftpAuthMode == 0) {
                            OutlinedTextField(
                                value = passwordOrSecret,
                                onValueChange = { passwordOrSecret = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = privateKey,
                                onValueChange = { privateKey = it },
                                label = { Text("Private Key (PEM format)") },
                                placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = privateKeyPassphrase,
                                onValueChange = { privateKeyPassphrase = it },
                                label = { Text("Key Passphrase (Optional)") },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = remoteBasePath,
                            onValueChange = { remoteBasePath = it },
                            label = { Text("Remote Base Path (Optional)") },
                            placeholder = { Text("/home/user/backups") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StorageProviderType.SMB3 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Host") },
                                placeholder = { Text("192.168.1.100") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                            OutlinedTextField(
                                value = if (port == 0) "" else port.toString(),
                                onValueChange = { port = it.toIntOrNull() ?: 445 },
                                label = { Text("Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.3f)
                            )
                        }

                        OutlinedTextField(
                            value = shareName,
                            onValueChange = { shareName = it },
                            label = { Text("Share Name") },
                            placeholder = { Text("Backups") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it },
                            label = { Text("Domain / Workgroup (Optional)") },
                            placeholder = { Text("WORKGROUP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordOrSecret,
                            onValueChange = { passwordOrSecret = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = remoteBasePath,
                            onValueChange = { remoteBasePath = it },
                            label = { Text("Remote Subfolder (Optional)") },
                            placeholder = { Text("Aournalpp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StorageProviderType.FTP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Host") },
                                placeholder = { Text("ftp.example.com") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                            OutlinedTextField(
                                value = if (port == 0) "" else port.toString(),
                                onValueChange = { port = it.toIntOrNull() ?: 21 },
                                label = { Text("Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.3f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Use FTPS (Explicit TLS)", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isFtpsExplicit,
                                onCheckedChange = {
                                    isFtpsExplicit = it
                                    if (it) isFtpsImplicit = false
                                }
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordOrSecret,
                            onValueChange = { passwordOrSecret = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = remoteBasePath,
                            onValueChange = { remoteBasePath = it },
                            label = { Text("Remote Base Directory (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Complete Backup Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Complete Backup Mirroring",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mirror ~/Notes (with emergency saves) & ~/.config to /Aournalpp/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCompleteBackupEnabled,
                        onCheckedChange = { isCompleteBackupEnabled = it }
                    )
                }

                // Enable Service Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Service for Sync",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                // Uniqueness Collision Error
                if (uniquenessErrorMessage != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uniquenessErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Connection Test Feedback
                if (testResultSuccess != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testResultSuccess == true) Icons.Default.Check else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (testResultSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = testResultMessage ?: (if (testResultSuccess == true) "Connection successful!" else "Connection failed"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResultSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = buildCurrentConfig()
                    val key = config.getAccountKey()
                    val collision = existingServices.firstOrNull { it.id != config.id && it.getAccountKey() == key }

                    if (collision != null) {
                        uniquenessErrorMessage = "An account for this server and user is already configured as '${collision.name}'. Each service must target a unique account."
                    } else {
                        uniquenessErrorMessage = null
                        onSaveService(config)
                        onDismissRequest()
                    }
                }
            ) {
                Text("Save Service")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        testResultSuccess = null
                        testResultMessage = null
                        coroutineScope.launch {
                            val tempConfig = buildCurrentConfig()
                            val provider = StorageProviderFactory.createProvider(tempConfig)
                            try {
                                val result = provider.testConnection()
                                if (result.isSuccess && result.getOrNull() == true) {
                                    testResultSuccess = true
                                    testResultMessage = "Connection verified successfully"
                                } else {
                                    testResultSuccess = false
                                    testResultMessage = result.exceptionOrNull()?.message ?: "Connection failed"
                                }
                            } catch (e: Exception) {
                                testResultSuccess = false
                                testResultMessage = e.message ?: "Connection failed"
                            } finally {
                                provider.disconnect()
                                isTestingConnection = false
                            }
                        }
                    },
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Test Connection")
                }

                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )

    // QR Code Scanner Dialog
    if (showQrScannerDialog) {
        QrCodeScannerDialog(
            title = "Scan Nextcloud QR Code",
            onQrCodeScanned = { rawQrText ->
                val creds = NextcloudQrParser.parse(rawQrText)
                if (creds != null) {
                    if (creds.serverUrl.isNotBlank()) serverUrl = creds.serverUrl
                    if (creds.username.isNotBlank()) username = creds.username
                    if (creds.appPassword.isNotBlank()) passwordOrSecret = creds.appPassword
                } else if (rawQrText.isNotBlank()) {
                    passwordOrSecret = rawQrText.trim()
                }
                showQrScannerDialog = false
            },
            onDismissRequest = { showQrScannerDialog = false }
        )
    }
}
