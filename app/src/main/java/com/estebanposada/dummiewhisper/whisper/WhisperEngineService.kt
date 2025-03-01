package com.estebanposada.dummiewhisper.whisper

import java.io.IOException

interface WhisperEngineService {
    fun isInitialized(): Boolean
    @Throws(IOException::class)
    fun initialize(modelPath: String, vocabPath: String, multilingual: Boolean): Boolean
    fun deInitialize()
    fun transcribeFile(wavePath: String): String
    fun transcribeBuffer(samples: FloatArray): String
}