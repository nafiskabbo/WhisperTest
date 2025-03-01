package com.estebanposada.dummiewhisper.whisper;

class SharedResourceJ {
    // Synchronized method for Thread 1 to wait for a signal with a timeout
    public synchronized boolean waitForSignalWithTimeout(long timeoutMillis) {
        long startTime = System.currentTimeMillis();

        try {
            wait(timeoutMillis);  // Wait for the given timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Restore interrupt status
            return false;  // Thread interruption as timeout
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Check if wait returned due to notify or timeout
        // Returned due to timeout
        return elapsedTime < timeoutMillis;  // Returned due to notify
    }

    // Synchronized method for Thread 2 to send a signal
    public synchronized void sendSignal() {
        notify();  // Notifies the waiting thread
    }
}
