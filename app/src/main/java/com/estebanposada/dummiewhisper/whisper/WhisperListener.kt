package com.estebanposada.dummiewhisper.whisper

interface WhisperListener {
    fun onUpdateReceived(message: String)
    fun onResultReceived(result: String)
}