package com.estebanposada.dummiewhisper

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.estebanposada.dummiewhisper.MainActivity.Companion.ENGLISH_ONLY_MODEL_FILE
import com.estebanposada.dummiewhisper.MainActivity.Companion.ENGLISH_ONLY_VOCAB_FILE
import com.estebanposada.dummiewhisper.ui.theme.DummieWhisperTheme
import com.estebanposada.dummiewhisper.whisper.SharedResource
import com.estebanposada.dummiewhisper.whisper.Whisper
import com.estebanposada.dummiewhisper.whisper.WhisperListener
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DummieWhisperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {
        const val ENGLISH_ONLY_MODEL_FILE: String = "whisper-tiny.en.tflite"
        const val ENGLISH_ONLY_VOCAB_FILE: String = "filters_vocab_en.bin"
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val transcription by remember { mutableStateOf("Transcribe text") }
    Column {
        Row(modifier = modifier) {
            Button(onClick = {
                setWhisper()
            }) {
                Text("Transcribe")
            }
        }
        Text(transcription)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DummieWhisperTheme {
        Greeting()
    }
}

private lateinit var whisper: Whisper

private val handler = Handler(Looper.getMainLooper())
private var startTime = 0L
private var loopTesting = false
private val transcriptionSync = SharedResource()

private fun setWhisper() {
//        val model = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "oniri_tc_model.bin")
//        val modelGG = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ggml-small.bin")
    val audioFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "jfk.wav"
    )
    val modelGG = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ENGLISH_ONLY_MODEL_FILE
    )
    val vocabModel = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ENGLISH_ONLY_VOCAB_FILE
    )
    initModel(modelGG, vocabModel)

    if (!whisper.isInProgress()) {
        Log.d("Whisper", "Start transcription...")
        startTranscription(audioFile.absolutePath)


        // only for loop testing
        if (loopTesting) {
            Thread {
                for (i in 0..999) {
                    if (!whisper.isInProgress()) startTranscription(audioFile.absolutePath)
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

private fun initModel(modelFile: File, modelGG: File) {
    val isMultilingualModel: Boolean = true

    whisper = Whisper()
    whisper.loadModel(modelFile, modelGG, isMultilingualModel)
    whisper.setListener(object : WhisperListener {
        override fun onUpdateReceived(message: String) {
            Log.d("Whisper", "Update is received, Message: $message")
            when (message) {
                Whisper.MSG_PROCESSING -> {
                    Log.i("Whisper", "I: Processing $message")
                }

                Whisper.MSG_PROCESSING_DONE -> {
                    if (loopTesting) transcriptionSync.sendSignal()
                }

                Whisper.MSG_FILE_NOT_FOUND -> {
                    Log.i("Whisper", "I: File not found error...!")
                }
            }
        }

        override fun onResultReceived(result: String) {
            val timeTaken = System.currentTimeMillis() - startTime
            handler.post {
                Log.i("Whisper", "I: Processing done in $timeTaken ms")
            }
//                    tvStatus.setText("Processing done in " + timeTaken + "ms") }

            Log.d("Whisper", "Result: $result")
            handler.post {
                Log.i("Whisper", "R: $result")
//                    tvResult.append(result)
            }
        }

    })
}


// Transcription calls
private fun startTranscription(waveFilePath: String) {
    whisper.setFilePath(waveFilePath)
    whisper.setAction(Whisper.Action.TRANSCRIBE)
    whisper.start()
}

private fun stopTranscription() {
    whisper.stop()
}