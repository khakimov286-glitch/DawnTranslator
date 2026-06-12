package com.dawncaster.translator

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var statusText: TextView
    private lateinit var btnOverlay: Button
    private lateinit var btnAccessibility: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        btnOverlay = findViewById(R.id.btn_overlay_permission)
        btnAccessibility = findViewById(R.id.btn_accessibility)

        btnOverlay.setOnClickListener { requestOverlayPermission() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }

        // Инициализация ML Kit (скачивает модель при первом запуске)
        scope.launch {
            statusText.text = "Скачиваю модель перевода… (~30 МБ)"
            val ok = Translator.init(this@MainActivity)
            statusText.text = if (ok) {
                "✅ Модель загружена. Включите Accessibility и откройте игру."
            } else {
                "❌ Не удалось скачать модель. Проверьте интернет."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayStatus()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Разрешение на оверлей уже есть", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Найдите DawnTranslator и включите службу", Toast.LENGTH_LONG).show()
    }

    private fun updateOverlayStatus() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        btnOverlay.text = if (hasPermission) "✅ Оверлей разрешён" else "Дать разрешение на оверлей"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
