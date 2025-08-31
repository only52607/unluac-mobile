package com.unluac.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unluac.mobile.R
import com.unluac.mobile.UiState
import com.unluac.mobile.UnluacMode
import java.text.CharacterIterator
import java.text.StringCharacterIterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onModeChange: (UnluacMode) -> Unit,
    onUseRawStringChange: (Boolean) -> Unit,
    onSelectFileClick: () -> Unit,
    onRunClick: () -> Unit,
    onOpenFileClick: (String) -> Unit,
    onShareFileClick: (String) -> Unit,
    onSaveAsClick: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(id = R.string.about))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ModeSelector(selectedMode = uiState.mode, onModeChange = onModeChange)
            Spacer(modifier = Modifier.height(16.dp))
            Options(useRawString = uiState.useRawString, onUseRawStringChange = onUseRawStringChange)
            Spacer(modifier = Modifier.height(16.dp))
            FileSelector(fileName = uiState.inputFileName, onSelectFileClick = onSelectFileClick)
            Spacer(modifier = Modifier.height(32.dp))
            RunButton(isLoading = uiState.isLoading, onClick = onRunClick, enabled = uiState.inputUri != null)
            Spacer(modifier = Modifier.height(16.dp))
            ResultCard(
                uiState = uiState,
                onOpenFileClick = onOpenFileClick,
                onShareFileClick = onShareFileClick,
                onSaveAsClick = onSaveAsClick
            )
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismissRequest = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(selectedMode: UnluacMode, onModeChange: (UnluacMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        TextField(
            value = stringResource(id = when(selectedMode) {
                UnluacMode.DECOMPILE -> R.string.decompile
                UnluacMode.DISASSEMBLE -> R.string.disassemble
                UnluacMode.ASSEMBLE -> R.string.assemble
            }),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.mode)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UnluacMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = when(mode) {
                        UnluacMode.DECOMPILE -> R.string.decompile
                        UnluacMode.DISASSEMBLE -> R.string.disassemble
                        UnluacMode.ASSEMBLE -> R.string.assemble
                    })) },
                    onClick = {
                        onModeChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Options(useRawString: Boolean, onUseRawStringChange: (Boolean) -> Unit) {
    Column {
        Text(text = stringResource(id = R.string.options), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useRawString, onCheckedChange = onUseRawStringChange)
            Text(text = stringResource(id = R.string.raw_string))
        }
    }
}

@Composable
private fun FileSelector(fileName: String?, onSelectFileClick: () -> Unit) {
    Column {
        Button(onClick = onSelectFileClick) {
            Text(text = stringResource(id = R.string.select_file))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = fileName ?: stringResource(id = R.string.no_file_selected))
    }
}

@Composable
private fun RunButton(isLoading: Boolean, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.running))
        } else {
            Text(text = stringResource(id = R.string.run))
        }
    }
}

@Composable
private fun ResultCard(
    uiState: UiState,
    onOpenFileClick: (String) -> Unit,
    onShareFileClick: (String) -> Unit,
    onSaveAsClick: () -> Unit
) {
    uiState.result?.let { result ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (result.error.isNotEmpty()) {
                    Text(text = stringResource(id = R.string.error_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = result.error, color = MaterialTheme.colorScheme.error)
                } else if (result.outputFilePath != null) {
                    Text(text = stringResource(id = R.string.success_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoRow(label = stringResource(id = R.string.input_file), value = uiState.inputFileName ?: "")
                    InfoRow(label = stringResource(id = R.string.input_size), value = formatFileSize(uiState.inputFileSize))
                    InfoRow(label = stringResource(id = R.string.output_size), value = formatFileSize(uiState.outputFileSize))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = { onOpenFileClick(result.outputFilePath) }) {
                            Text(text = stringResource(id = R.string.open_file))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onShareFileClick(result.outputFilePath) }) {
                            Text(text = stringResource(id = R.string.share_file))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onSaveAsClick) {
                            Text(text = stringResource(id = R.string.save_as))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
        Text(text = value)
    }
}

private fun formatFileSize(size: Long?): String {
    if (size == null || size <= 0) return "N/A"
    val sb = StringBuilder(10)
    val ci = StringCharacterIterator("kMGTPE")
    var bytes = size
    while (bytes >= 1024) {
        bytes /= 1024
        ci.next()
    }
    sb.append(String.format("%.2f", bytes / 1024.0))
    sb.append(' ')
    sb.append(ci.current())
    sb.append("B")
    return sb.toString()
}

@Composable
private fun AboutDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(id = R.string.about)) },
        text = {
            Column {
                Text(stringResource(R.string.version, "1.0"))
                Text(stringResource(R.string.unluac_version, "1.2.3.530"))
                Text(stringResource(R.string.author, "OOOOONLY"))
                Text(stringResource(R.string.github, "https://github.com/wusy/unluac-mobile"))
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("OK")
            }
        }
    )
}