package com.unluac.mobile

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class UnluacMode(val argument: String) {
    DECOMPILE(""),
    DISASSEMBLE("--disassemble"),
    ASSEMBLE("--assemble")
}

data class UiState(
    val mode: UnluacMode = UnluacMode.DECOMPILE,
    val useRawString: Boolean = false,
    val inputUri: Uri? = null,
    val inputFileName: String? = null,
    val inputFileSize: Long? = null,
    val isLoading: Boolean = false,
    val result: UnluacResult? = null,
    val outputFilePath: String? = null,
    val outputFileSize: Long? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(UiState())
        private set

    fun onModeChange(mode: UnluacMode) {
        uiState = uiState.copy(mode = mode)
    }

    fun onUseRawStringChange(useRawString: Boolean) {
        uiState = uiState.copy(useRawString = useRawString)
    }

    fun onInputFileSelected(uri: Uri?) {
        if (uri == null) return
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            val fileName = it.getString(nameIndex)
            val fileSize = it.getLong(sizeIndex)
            uiState = uiState.copy(inputUri = uri, inputFileName = fileName, inputFileSize = fileSize)
        }
    }

    fun runUnluac() {
        val currentInputUri = uiState.inputUri ?: return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, result = null, outputFilePath = null, outputFileSize = null)

            val result = withContext(Dispatchers.IO) {
                val context = getApplication<Application>().applicationContext
                val tempInputFile = File(context.cacheDir, uiState.inputFileName ?: "temp.luac")
                val outputFileName = (uiState.inputFileName ?: "output") + ".lua"
                val tempOutputFile = File(context.cacheDir, outputFileName)

                context.contentResolver.openInputStream(currentInputUri)?.use { inputStream ->
                    tempInputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val args = mutableListOf<String>()
                if (uiState.mode.argument.isNotEmpty()) {
                    args.add(uiState.mode.argument)
                }
                if (uiState.useRawString) {
                    args.add("--rawstring")
                }
                args.add("-o")
                args.add(tempOutputFile.absolutePath)
                args.add(tempInputFile.absolutePath)

                UnluacWrapper.run(args.toTypedArray())
            }

            val outputFileSize = if (result.outputFilePath != null) File(result.outputFilePath).length() else null
            uiState = uiState.copy(isLoading = false, result = result, outputFilePath = result.outputFilePath, outputFileSize = outputFileSize)
        }
    }
}