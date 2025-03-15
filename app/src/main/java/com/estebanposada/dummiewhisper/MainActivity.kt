package com.estebanposada.dummiewhisper

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebanposada.dummiewhisper.ui.theme.DummieWhisperTheme
import io.oniri.oniri.ui.editdream.transcription.player.AudioPlayerImpl
import io.oniri.oniri.ui.editdream.transcription.recorder.AudioRecorderImpl
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    private var file: File? = null

    private val recorder by lazy {
        AudioRecorderImpl(applicationContext)
    }
    private val player by lazy {
        AudioPlayerImpl(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (checkSelfPermission(
                permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission.RECORD_AUDIO), 0)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            DummieWhisperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding), viewModel, onRecord = {
                        val recordingId = UUID.randomUUID().toString()
                        val dreamId = UUID.randomUUID().toString()
                        val directory =
                            File(
                                context.filesDir.absolutePath +
                                        "/recordings/OniriUser.user?.id/$dreamId",
                            )
                        if (!directory.exists()) directory.mkdirs()
                        file = File(directory, "$recordingId.m4a")
                        file?.let { recorder.start(it) }
                    }, onPlay = {
                        recorder.stop()
                        file?.let {
                            player.playFile(it)
                            viewModel.setFile(it)
                            viewModel.setWhisper()
                        }
                    })
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
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    onRecord: () -> Unit,
    onPlay: () -> Unit
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val transcription by viewModel.transcription.collectAsStateWithLifecycle()

    // Load files when the screen is first composed
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadFiles(context)
    }
    Column {
        Spacer(Modifier.height(16.dp))
        Row(modifier = modifier) {
            Button(onClick = {
                viewModel.setWhisper()
            }) {
                Text("Transcribe")
            }
        }
        Button(onClick = onRecord) {
            Text("Record")
        }
        Button(onClick = onPlay) {
            Text("Play")
        }
        Spacer(Modifier.height(16.dp))
        Text("Status: $status")
        Spacer(Modifier.height(16.dp))
        Text(transcription)
    }
}

@Composable
fun MyPreview(modifier: Modifier = Modifier) {
    Column {
        Spacer(Modifier.height(16.dp))
        Row(modifier = modifier) {
            Button(onClick = {
            }) {
                Text("Transcribe")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Status: $")
        Spacer(Modifier.height(16.dp))
        Text("transcription")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DummieWhisperTheme {
        MyPreview()
    }
}