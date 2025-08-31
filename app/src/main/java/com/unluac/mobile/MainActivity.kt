package com.unluac.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.unluac.mobile.ui.MainScreen
import com.unluac.mobile.ui.theme.UnluacMobileTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onInputFileSelected(uri)
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { destinationUri ->
            viewModel.uiState.outputFilePath?.let { sourcePath ->
                val sourceFile = File(sourcePath)
                contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnluacMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        uiState = viewModel.uiState,
                        onModeChange = viewModel::onModeChange,
                        onUseRawStringChange = viewModel::onUseRawStringChange,
                        onUseLuajChange = viewModel::onUseLuajChange,
                        onNoDebugChange = viewModel::onNoDebugChange,
                        onSelectFileClick = { openFileLauncher.launch(arrayOf("*/*")) },
                        onRunClick = viewModel::runUnluac,
                        onOpenFileClick = { filePath -> openOutputFile(filePath) },
                        onShareFileClick = { filePath -> shareOutputFile(filePath) },
                        onSaveAsClick = { viewModel.uiState.outputFilePath?.let { saveFileLauncher.launch(File(it).name) } }
                    )
                }
            }
        }
    }

    private fun openOutputFile(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.open_file)))
    }

    private fun shareOutputFile(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_file)))
    }
}