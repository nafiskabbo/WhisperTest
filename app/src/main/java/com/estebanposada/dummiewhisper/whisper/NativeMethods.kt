package com.estebanposada.dummiewhisper.whisper

external fun createTFLiteEngine(): Long
external fun loadModel(nativePtr: Long, modelPath: String, isMultilingual: Boolean): Int
external fun freeModel(nativePtr: Long)
external fun transcribeBuffer(nativePtr: Long, samples: FloatArray): String
external fun transcribeFile(nativePtr: Long, waveFile: String): String