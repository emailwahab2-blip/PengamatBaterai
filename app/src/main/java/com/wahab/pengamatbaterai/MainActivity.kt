package com.wahab.pengamatbaterai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var editMaxPercent: EditText
    private lateinit var editMinPercent: EditText
    private lateinit var editMaxText: EditText
    private lateinit var editMinText: EditText
    private lateinit var switchEnabled: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editMaxPercent = findViewById(R.id.editMaxPercent)
        editMinPercent = findViewById(R.id.editMinPercent)
        editMaxText = findViewById(R.id.editMaxText)
        editMinText = findViewById(R.id.editMinText)
        switchEnabled = findViewById(R.id.switchEnabled)

        loadPrefs()

        findViewById<android.widget.Button>(R.id.buttonSave).setOnClickListener {
            savePrefsAndApply()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        sendBroadcast(
            Intent(BatteryMonitorService.ACTION_STOP_ALERT).setPackage(packageName)
        )
    }

    private fun loadPrefs() {
        editMaxPercent.setText(Prefs.getMaxPercent(this).toString())
        editMinPercent.setText(Prefs.getMinPercent(this).toString())
        editMaxText.setText(Prefs.getMaxText(this))
        editMinText.setText(Prefs.getMinText(this))
        switchEnabled.isChecked = Prefs.isEnabled(this)
    }

    private fun savePrefsAndApply() {
        val maxPercent = editMaxPercent.text.toString().toIntOrNull()
        val minPercent = editMinPercent.text.toString().toIntOrNull()

        if (maxPercent == null || maxPercent !in 0..100) {
            Toast.makeText(this, getString(R.string.error_max_percent), Toast.LENGTH_SHORT).show()
            return
        }
        if (minPercent == null || minPercent !in 0..100) {
            Toast.makeText(this, getString(R.string.error_min_percent), Toast.LENGTH_SHORT).show()
            return
        }
        if (minPercent >= maxPercent) {
            Toast.makeText(this, getString(R.string.error_min_lt_max), Toast.LENGTH_SHORT).show()
            return
        }

        val maxText = editMaxText.text.toString()
        val minText = editMinText.text.toString()
        val enabled = switchEnabled.isChecked

        Prefs.save(this, maxPercent, minPercent, maxText, minText, enabled)

        val serviceIntent = Intent(this, BatteryMonitorService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            stopService(serviceIntent)
        }

        Toast.makeText(this, getString(R.string.saved_message), Toast.LENGTH_SHORT).show()
    }
}
