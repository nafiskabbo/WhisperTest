package com.estebanposada.dummiewhisper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import com.estebanposada.dummiewhisper.whisper.SharedResource
import com.estebanposada.dummiewhisper.whisper.Whisper
import com.estebanposada.dummiewhisper.whisper.WhisperListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainViewModel: ViewModel() {
    private val _status = MutableStateFlow<String>("")
    val status: StateFlow<String> = _status

    private val _transcription = MutableStateFlow<String>("Transcribe text")
    val transcription: StateFlow<String> = _transcription

    private var sdcardDataFolder: File? = null
    private var selectedTfliteFile: File? = null
    private var selectedWaveFile: File? = null

    // Whisper
    var whisper: Whisper? = null
    private val handler = Handler(Looper.getMainLooper())

    private var startTime = 0L
    private var loopTesting = false
    private val transcriptionSync = SharedResource()

    fun setWhisper() {
        if (whisper == null)
            initModel(selectedTfliteFile!!)

        if (whisper?.isInProgress() == false) {
            Log.d("Whisper", "Start transcription...")
            startTranscription(selectedWaveFile!!.absolutePath)

            // only for loop testing
            if (loopTesting) {
                Thread {
                    for (i in 0..999) {
                        if (!whisper!!.isInProgress()) startTranscription(selectedWaveFile!!.absolutePath)
                        else Log.d("Whisper", "Whisper is already in progress...!")

                        val wasNotified = transcriptionSync.waitForSignalWithTimeout(15000)
                        if (wasNotified)
                            Log.d("Whisper", "Transcription Notified...!")
                        else
                            Log.d("Whisper", "Transcription Timeout...!")
                    }
                }.start()
            }
        } else {
            Log.d("Whisper", "Whisper is already in progress...!")
            stopTranscription()
        }
    }


    private fun initModel(modelFile: File) {
        val isMultilingualModel = !(modelFile.name.endsWith(ENGLISH_ONLY_MODEL_EXTENSION))
        val vocabFileName = if (isMultilingualModel) MULTILINGUAL_VOCAB_FILE else ENGLISH_ONLY_VOCAB_FILE
        val vocabFile = File(sdcardDataFolder, vocabFileName)

        whisper = Whisper()
        whisper?.loadModel(modelFile, vocabFile, isMultilingualModel)
        whisper?.setListener(object : WhisperListener {
            override fun onUpdateReceived(message: String) {
                Log.d("Whisper", "Update is received, Message: $message")
                when (message) {
                    Whisper.MSG_PROCESSING -> {
                        Log.i("Whisper", "I: Processing $message")
                        _status.value = message
                        _transcription.value = ""
                        startTime = System.currentTimeMillis()
                    }

                    Whisper.MSG_PROCESSING_DONE -> {
                        if (loopTesting) transcriptionSync.sendSignal()
                    }

                    Whisper.MSG_FILE_NOT_FOUND -> {
                        Log.i("Whisper", "I: File not found error...!")
                        handler.post { _status.value = message }
                    }
                }
            }

            override fun onResultReceived(result: String) {
                val timeTaken = System.currentTimeMillis() - startTime
                handler.post {
                    Log.i("Whisper", "I: Processing done in $timeTaken ms")
                    _status.value = "Processing done in " + timeTaken + "ms"
                }

                Log.d("Whisper", "Result: $result")
                handler.post {
                    Log.i("Whisper", "R: $result")
                    _transcription.value = result
                }
            }

        })
    }


    // Transcription calls
    private fun startTranscription(waveFilePath: String) {
        whisper?.setFilePath(waveFilePath)
        whisper?.setAction(Whisper.Action.TRANSCRIBE)
        whisper?.start()
    }

    private fun stopTranscription() {
        whisper?.stop()
    }

    // Files
    fun loadFiles(context: Context) {
        val sdcardDataFolder = context.getExternalFilesDir(null)
        sdcardDataFolder ?: return
        copyAssetsToSdcard(context, sdcardDataFolder, EXTENSIONS_TO_COPY)

//        val tfliteFiles = getFilesWithExtension(sdcardDataFolder, ".tflite")
//        val waveFiles = getFilesWithExtension(sdcardDataFolder, ".wav")

        // Initialize default model to use
        // Initialize default model to use
        selectedTfliteFile = File(sdcardDataFolder, ENGLISH_ONLY_MODEL_FILE)
        selectedWaveFile = File(sdcardDataFolder, "coincidence.wav")
//        selectedTfliteFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ENGLISH_ONLY_MODEL_FILE)
//        selectedWaveFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "jfk.wav")
    }

    fun setFile(file: File){
        selectedWaveFile = file
    }


    private fun copyAssetsToSdcard(context: Context, destFolder: File, extensions: Array<String>) {
        val assetManager = context.assets

        try {
            // List all files in the assets folder once
            val assetFiles = assetManager.list("") ?: return

            for (assetFileName in assetFiles) {
                // Check if file matches any of the provided extensions
                for (extension in extensions) {
                    if (assetFileName.endsWith(".$extension")) {
                        val outFile = File(destFolder, assetFileName)

                        // Skip if file already exists
                        if (outFile.exists()) break

                        assetManager.open(assetFileName).use { inputStream ->
                            FileOutputStream(outFile).use { outputStream ->
                                val buffer = ByteArray(1024)
                                var bytesRead: Int
                                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        break // No need to check further extensions
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getFilesWithExtension(directory: File?, extension: String?): ArrayList<File> {
        val filteredFiles = ArrayList<File>()

        // Check if the directory is accessible
        if (directory != null && directory.exists()) {
            val files = directory.listFiles()

            // Filter files by the provided extension
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.name.endsWith(extension!!)) {
                        filteredFiles.add(file)
                    }
                }
            }
        }

        return filteredFiles
    }

    companion object {
        const val ENGLISH_ONLY_MODEL_FILE: String = "whisper-tiny.en.tflite"
        const val ENGLISH_ONLY_VOCAB_FILE: String = "filters_vocab_en.bin"
        private const val ENGLISH_ONLY_MODEL_EXTENSION: String = ".en.tflite"
        private const val MULTILINGUAL_VOCAB_FILE: String = "filters_vocab_multilingual.bin"
        private val EXTENSIONS_TO_COPY: Array<String> = arrayOf("tflite", "bin", "wav", "pcm")
    }
}