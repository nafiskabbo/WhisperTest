package io.oniri.oniri.ui.editdream.transcription.recorder

import android.content.Context
import android.media.AudioFormat
import com.github.squti.androidwaverecorder.WaveRecorder
import java.io.File

class AudioRecorderImpl(private val context: Context) : AudioRecorder {

    private var recorder: WaveRecorder? = null

    override fun start(outputFile: File) {
        WaveRecorder(outputFile.absolutePath).apply {
            configureWaveSettings {
                sampleRate = 16000
                channels = AudioFormat.CHANNEL_IN_MONO
                audioEncoding = AudioFormat.ENCODING_PCM_16BIT
            }
            startRecording()

            recorder = this
        }
    }

    override fun stop() {
        recorder?.stopRecording()
        recorder = null
    }
}