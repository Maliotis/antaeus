package io.pleo.antaeus.core.tasks

import java.util.*

class DelayTimerTask(private val runnable: () -> Unit): TimerTask() {
    override fun run() {
        runnable()
    }
}