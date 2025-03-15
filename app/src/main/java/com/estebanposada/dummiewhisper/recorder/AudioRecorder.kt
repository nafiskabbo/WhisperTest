package io.oniri.oniri.ui.editdream.transcription.recorder

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}