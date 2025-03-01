package com.estebanposada.dummiewhisper.whisper


class SharedResource {
    private val lock = Object()

    @Synchronized
    fun waitForSignalWithTimeout(timeoutMillis: Long): Boolean {
        val startTime = System.currentTimeMillis()

        try {
            lock.wait(timeoutMillis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        }
        val elapsedTime = System.currentTimeMillis() - startTime
        return (elapsedTime < timeoutMillis)
    }

    @Synchronized
    fun sendSignal() {
            lock.notify()
    }
}

