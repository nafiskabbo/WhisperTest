package com.estebanposada.dummiewhisper.whisper

import android.util.Log
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class Whisper {
    private val whisperEngine = WhisperEngine()
    private var updateListener: WhisperListener? = null
    private lateinit var action: Action
    private var wavFilePath: String? = null
    private val inProgress = AtomicBoolean(false)
    private val taskLock = ReentrantLock()
    private var taskAvailable = false
    private val hasTask = taskLock.newCondition()
    private val threadTranscribeFile = Thread(this::transcribeFileLoop)
    private val threadTranscribeBuffer = Thread(this::transcribeBufferLoop)

    //    private val audioBufferQueue = LinkedList<FloatArray>()
    private val audioBufferQueue: Queue<FloatArray> = LinkedList()


    enum class Action {
        TRANSLATE, TRANSCRIBE
    }

    init {
        threadTranscribeFile.start()
        threadTranscribeBuffer.start()

    }

    fun setListener(listener: WhisperListener) {
        updateListener = listener
    }

    fun loadModel(modelPath: File, vocabPath: File, isMultilingual: Boolean) {
        loadModel(modelPath.absolutePath, vocabPath.absolutePath, isMultilingual)
    }

    fun loadModel(modelPath: String, vocabPath: String, isMultilingual: Boolean) {
        try {
            whisperEngine.initialize(modelPath, vocabPath, isMultilingual)
        } catch (e: IOException) {
            Log.e("Whisper", "Error initializing model...", e)
            sendUpdate("Model initialiation failed")
        }
    }

    fun unloadModel() {
        whisperEngine.deInitialize()
    }

    fun setAction(action: Action) {
        this.action = action
    }

    fun setFilePath(wavFile: String) {
        this.wavFilePath = wavFile
    }

    fun start() {
        if (!inProgress.compareAndSet(false, true)) {
            Log.d("Whisper", "Execution is already in progress...")
            return
        }
        taskLock.lock()
        try {
            taskAvailable = true
            hasTask.signal()
        } finally {
            taskLock.unlock()
        }
    }

    fun stop() {
        inProgress.set(false)
    }

    fun isInProgress() = inProgress.get()

    private fun transcribeFileLoop() {
        while (!Thread.currentThread().isInterrupted) {
            taskLock.lock()
            try {
                while (!taskAvailable) {
                    hasTask.await()
                }
                transcribeFile()
                taskAvailable = false
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                taskLock.unlock()
            }
        }
    }

    private fun transcribeFile() {
        try {
            wavFilePath?.let { file ->
                if (whisperEngine.isInitialized()) {
                    val waveFile = File(file)
                    if (waveFile.exists()) {
                        val startTime = System.currentTimeMillis()
                        sendUpdate(MSG_PROCESSING)

                        var result: String? = null
                        synchronized(whisperEngine) {
                            if (action == Action.TRANSCRIBE) {
                                result = whisperEngine.transcribeFile(file)
                                val s = ""
                            } else {
                                Log.d("Whisper", "Translate feature is not implemented")
                            }
                        }
                        result?.let { sendResult(it) }

                        val timeTaken = System.currentTimeMillis() - startTime
                        Log.d("Whisper", "Time Taken for transcription $timeTaken ms")
                        sendUpdate(MSG_PROCESSING_DONE)
                    } else {
                        sendUpdate(MSG_FILE_NOT_FOUND)
                    }
                } else {
                    sendUpdate("Engine not initialized or file path not set")
                }
            }
        } catch (e: Exception) {
            Log.e("Whisper", "Error during transcription", e)
            sendUpdate("Transcription failed: ${e.message}")
        } finally {
            inProgress.set(false)
        }
    }

    private fun sendUpdate(message: String) {
        updateListener?.onUpdateReceived(message)
    }

    private fun sendResult(message: String) {
        updateListener?.onResultReceived(message)
    }

    /////////////////////// Live MIC feed transcription calls /////////////////////////////////
    private fun transcribeBufferLoop() {
        while (!Thread.currentThread().isInterrupted) {
            val samples = readBuffer()
            if (samples != null) {
                synchronized(whisperEngine) {
                    sendResult(whisperEngine.transcribeBuffer(samples))
                }
            }
        }
    }

    private fun readBuffer(): FloatArray? {
        val lock = Object()
        synchronized(lock) {
            while (audioBufferQueue.isEmpty()) {
                try {
                    lock.wait()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
            return audioBufferQueue.poll()
        }
    }

    companion object {
        const val MSG_PROCESSING: String = "Processing..."
        const val MSG_PROCESSING_DONE: String = "Processing done...!"
        const val MSG_FILE_NOT_FOUND: String = "Input file doesn't exist..!"
    }
}