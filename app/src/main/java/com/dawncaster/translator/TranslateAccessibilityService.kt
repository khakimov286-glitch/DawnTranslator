package com.dawncaster.translator

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * Фоновый сервис, который читает текст с экрана.
 * Работает в любой игре/приложении, где есть текст.
 */
class TranslateAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastTranslatedText = ""
    private var lastEventTime = 0L
    private val debounceMs = 1500L  // не дёргать перевод чаще раза в 1.5 сек

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 500  // агрегация событий в течение 500 мс
        }
        setServiceInfo(info)
        Log.d(TAG, "Сервис перевода запущен")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - lastEventTime < debounceMs) return  // дебаунс
        lastEventTime = now

        val text = extractEnglishText(event?.source) ?: return
        if (text == lastTranslatedText) return  // не повторять одно и то же
        if (text.length < 3) return             // мусор (1-2 символа) пропускаем
        if (!containsEnglish(text)) return        // только если есть английские буквы

        lastTranslatedText = text

        scope.launch {
            val translation = Translator.translate(this@TranslateAccessibilityService, text)
            if (translation != null && translation != text) {
                FloatingWindow.show(this@TranslateAccessibilityService, text, translation)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Сервис прерван")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        FloatingWindow.hide(this)
        Log.d(TAG, "Сервис остановлен")
    }

    /**
     * Рекурсивно собирает весь видимый текст из accessibility-дерева.
     */
    private fun extractEnglishText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val sb = StringBuilder()
        collectText(node, sb)
        return sb.toString().trim().take(500)  // ограничиваем длину
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            sb.append(text).append(" ")
        }
        val contentDesc = node.contentDescription?.toString()?.trim()
        if (!contentDesc.isNullOrEmpty() && contentDesc != text) {
            sb.append(contentDesc).append(" ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, sb)
            child.recycle()
        }
    }

    /** Проверяет, есть ли в строке английские буквы */
    private fun containsEnglish(text: String): Boolean {
        return text.any { it in 'A'..'Z' || it in 'a'..'z' }
    }

    companion object {
        private const val TAG = "DawnTranslator"
    }
}
