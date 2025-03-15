package io.oniri.oniri.ui.editdream.transcription.player

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
}