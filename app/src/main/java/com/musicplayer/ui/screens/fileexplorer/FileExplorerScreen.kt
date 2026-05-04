package com.musicplayer.ui.screens.fileexplorer

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.musicplayer.data.local.filemanager.FileItem
import com.musicplayer.ui.components.EmptyState
import com.musicplayer.util.TimeUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    initialPath: String = "",
    navController: NavController,
    viewModel: FileExplorerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renamePath by remember { mutableStateOf("") }
    var renameValue by remember { mutableStateOf("") }

    // ---- MANAGE_EXTERNAL_STORAGE permission gate ----
    var hasStoragePermission by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }
    var showPermissionRationale by remember { mutableStateOf(!hasStoragePermission) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        // Re-check after returning from Settings
        hasStoragePermission = Environment.isExternalStorageManager()
        showPermissionRationale = !hasStoragePermission
    }

    // Re-check permission when the screen resumes (e.g. user returns from Settings)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasStoragePermission = Environment.isExternalStorageManager()
            showPermissionRationale = !hasStoragePermission
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Storage Access Required") },
            text = {
                Text(
                    "Full storage access is required to browse, manage, and edit music files " +
                        "on your device. Please grant \"All files access\" in the next screen.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    settingsLauncher.launch(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Cancel") }
            },
        )
        return
    }

    // Show error messages as snackbars
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    LaunchedEffect(initialPath) { viewModel.init(initialPath) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Files") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.selectedPaths.isNotEmpty()) {
                        IconButton(onClick = { viewModel.copySelected() }) {
                            Icon(Icons.Default.CopyAll, "Copy")
                        }
                        IconButton(onClick = { viewModel.cutSelected() }) {
                            Icon(Icons.Default.ContentCut, "Cut")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Delete selected")
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Clear selection")
                        }
                    } else {
                        if (state.clipboardPath != null) {
                            IconButton(onClick = { viewModel.paste() }) {
                                Icon(Icons.Default.ContentPaste, "Paste")
                            }
                        }
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, "Select all")
                        }
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { viewModel.navigateUp() }) {
                    Icon(Icons.Default.ArrowUpward, "Up one level")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.items.size} items" +
                        if (state.selectedPaths.isNotEmpty()) " · ${state.selectedPaths.size} selected" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showNewFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, "New folder")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Breadcrumb navigation
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(state.breadcrumbs) { (label, path) ->
                    TextButton(onClick = { viewModel.navigateTo(path) }) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.breadcrumbs.last().second != path) {
                        Text("/", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            HorizontalDivider()

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    message = "This folder is empty",
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        FileListItem(
                            item = item,
                            isSelected = item.path in state.selectedPaths,
                            onClick = {
                                if (state.selectedPaths.isNotEmpty()) {
                                    viewModel.toggleSelection(item.path)
                                } else if (item.isDirectory) {
                                    viewModel.navigateTo(item.path)
                                } else if (item.isAudioFile) {
                                    viewModel.playFile(item.path)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(item.path) },
                            onRename = {
                                renamePath = item.path
                                renameValue = item.name
                                showRenameDialog = true
                            },
                            onDelete = { viewModel.delete(item.path) },
                            onEditTags = { viewModel.openTagEditor(item.path) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }

    // New folder dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName)
                        showNewFolderDialog = false
                        newFolderName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") } },
        )
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(renamePath, renameValue)
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
        )
    }

    // Tag editor bottom sheet
    if (state.tagEditorPath != null) {
        TagEditorBottomSheet(
            tagFields = state.tagFields,
            isLoading = state.isLoadingTags,
            isSaving = state.isSavingTags,
            onSave = { fields -> viewModel.saveTags(fields) },
            onDismiss = { viewModel.closeTagEditor() },
        )
    }

    // Tag save failure dialog
    state.tagSaveError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearTagSaveError() },
            title = { Text("Failed to Save Tags") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearTagSaveError() }) { Text("OK") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onEditTags: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surface

    Surface(color = containerColor) {
        ListItem(
            headlineContent = {
                Text(
                    item.name,
                    color = when {
                        item.isAudioFile -> MaterialTheme.colorScheme.primary
                        item.isDirectory -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            supportingContent = {
                if (!item.isDirectory) {
                    Text(TimeUtil.formatFileSize(item.size), style = MaterialTheme.typography.bodySmall)
                }
            },
            leadingContent = {
                Icon(
                    imageVector = when {
                        item.isDirectory -> Icons.Default.Folder
                        item.isAudioFile -> Icons.Default.AudioFile
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when {
                        item.isDirectory -> MaterialTheme.colorScheme.secondary
                        item.isAudioFile -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (item.isAudioFile) {
                            DropdownMenuItem(
                                text = { Text("Play") },
                                onClick = { showMenu = false; onClick() },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Tags") },
                                onClick = { showMenu = false; onEditTags() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                        )
                    }
                }
            },
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        )
    }
}
