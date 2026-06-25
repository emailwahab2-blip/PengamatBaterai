package com.wahab.pengamatbaterai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

class BatteryMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "pengamat_baterai_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_ALERT = "com.wahab.pengamatbaterai.STOP_ALERT"
        private const val UTTERANCE_ID = "pengamat_baterai_utterance"
        private const val REPEAT_COUNT = 20
        private const val REPEAT_DELAY_MS = 350L
    }

    private enum class AlertKind { MAX, MIN }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var maxAlerted = false
    private var minAlerted = false
    private var isPlugged = false

    private var activeAlert: AlertKind? = null
    private var activeText = ""
    private var remainingRepeats = 0
    private val handler = Handler(Looper.getMainLooper())

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_STOP_ALERT) {
                stopAlert()
                return
            }
            handleBatteryIntent(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale("id", "ID")
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                handler.postDelayed({ speakNext() }, REPEAT_DELAY_MS)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handler.postDelayed({ speakNext() }, REPEAT_DELAY_MS)
            }
        })
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(ACTION_STOP_ALERT)
        val initialIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
        initialIntent?.let { handleBatteryIntent(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALERT) {
            stopAlert()
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun handleBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val percent = level * 100 / scale

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (plugged != isPlugged) {
            isPlugged = plugged
            checkStopConditionDueToPlugChange()
        }

        checkThresholds(percent)
    }

    private fun checkStopConditionDueToPlugChange() {
        when (activeAlert) {
            AlertKind.MIN -> if (isPlugged) stopAlert()
            AlertKind.MAX -> if (!isPlugged) stopAlert()
            null -> {}
        }
    }

    private fun checkThresholds(percent: Int) {
        val maxPercent = Prefs.getMaxPercent(this)
        val minPercent = Prefs.getMinPercent(this)

        if (percent >= maxPercent) {
            if (!maxAlerted) {
                maxAlerted = true
                startAlert(AlertKind.MAX, Prefs.getMaxText(this))
            }
        } else {
            maxAlerted = false
        }

        if (percent <= minPercent) {
            if (!minAlerted) {
                minAlerted = true
                startAlert(AlertKind.MIN, Prefs.getMinText(this))
            }
        } else {
            minAlerted = false
        }
    }

    private fun startAlert(kind: AlertKind, text: String) {
        stopAlert()
        if (text.isBlank() || !ttsReady) return
        activeAlert = kind
        activeText = text
        remainingRepeats = REPEAT_COUNT
        speakNext()
    }

    private fun speakNext() {
        val kind = activeAlert ?: return
        if (remainingRepeats <= 0) {
            activeAlert = null
            return
        }
        val shouldContinue = when (kind) {
            AlertKind.MIN -> !isPlugged
            AlertKind.MAX -> isPlugged
        }
        if (!shouldContinue) {
            stopAlert()
            return
        }
        remainingRepeats--
        tts?.speak(activeText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun stopAlert() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        activeAlert = null
        remainingRepeats = 0
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running_text))
            .setSmallIcon(R.drawable.ic_battery)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(batteryReceiver)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
