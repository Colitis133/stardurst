package com.stardust

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionExplanationActivity : AppCompatActivity() {

    private val REQUEST_CODE_RECORD_AUDIO = 101
    private val REQUEST_CODE_OVERLAY_PERMISSION = 102
    private val REQUEST_CODE_BATTERY_OPTIMIZATION = 103

    private lateinit var grantRecordAudioButton: Button
    private lateinit var grantOverlayButton: Button
    private lateinit var grantBatteryOptimizationButton: Button
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_explanation)

        grantRecordAudioButton = findViewById(R.id.grantRecordAudioButton)
        grantOverlayButton = findViewById(R.id.grantOverlayButton)
        grantBatteryOptimizationButton = findViewById(R.id.grantBatteryOptimizationButton)
        continueButton = findViewById(R.id.continueButton)

        grantRecordAudioButton.setOnClickListener { requestRecordAudioPermission() }
        grantOverlayButton.setOnClickListener { requestOverlayPermission() }
        grantBatteryOptimizationButton.setOnClickListener { requestBatteryOptimizationExemption() }
        continueButton.setOnClickListener { finish() }

        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val recordAudioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }

        grantRecordAudioButton.isEnabled = !recordAudioGranted
        grantOverlayButton.isEnabled = !overlayGranted
        grantBatteryOptimizationButton.isEnabled = !batteryOptimizationIgnored

        continueButton.isEnabled = recordAudioGranted && overlayGranted && batteryOptimizationIgnored
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_RECORD_AUDIO)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + packageName)
                startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            updatePermissionStatus()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION || requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            updatePermissionStatus()
        }
    }
}
