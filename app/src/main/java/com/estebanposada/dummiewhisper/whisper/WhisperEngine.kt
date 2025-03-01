package com.estebanposada.dummiewhisper.whisper

import android.util.Log

class WhisperEngine : WhisperEngineService {
    companion object {
        private const val TAG = "WhisperEngineNative"
    }

    private var nativePtr: Long = 0
    private var mIsInitialized = false

    init {
        System.loadLibrary("audioEngine")
        nativePtr = createTFLiteEngine()
    }

    override fun isInitialized(): Boolean = mIsInitialized

    override fun initialize(
        modelPath: String,
        vocabPath: String,
        multilingual: Boolean
    ): Boolean {
        loadModel(modelPath, multilingual)
        Log.d("Whisper", "Model is loaded... $modelPath")
        mIsInitialized = true
        return true
    }

    override fun deInitialize() {
        freeModel()
    }

    override fun transcribeFile(wavePath: String): String =
        transcribeFile(nativePtr, wavePath)

    override fun transcribeBuffer(samples: FloatArray): String =
        transcribeBuffer(nativePtr, samples)

    private fun loadModel(modelPath: String, isMultilingual: Boolean) =
        loadModel(nativePtr, modelPath, isMultilingual)

    private fun freeModel() {
        freeModel(nativePtr)
    }
}