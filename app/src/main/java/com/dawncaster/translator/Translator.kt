package com.dawncaster.translator

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Офлайн-переводчик английский → русский с Dawncaster-словарём.
 * При первом запуске скачивает ML-модель (~30 МБ).
 */
object Translator {

    private var mlkTranslator: com.google.mlkit.nl.translate.Translator? = null
    private var modelReady = false

    /** Инициализация: скачивает офлайн-модель англ→рус если ещё не скачана */
    suspend fun init(context: Context): Boolean {
        if (modelReady) return true

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build()

        mlkTranslator = Translation.getClient(options)

        return suspendCancellableCoroutine { cont ->
            mlkTranslator!!.downloadModelIfNeeded()
                .addOnSuccessListener {
                    modelReady = true
                    cont.resume(true)
                }
                .addOnFailureListener { e ->
                    modelReady = false
                    cont.resume(false)
                }
        }
    }

    /** Перевести текст: сначала словарь Dawncaster, потом ML Kit */
    suspend fun translate(context: Context, text: String): String? {
        // 1) Проверяем словарь — приоритет у игровых терминов
        val glossaryResult = translateGlossary(text)
        if (glossaryResult != null) return glossaryResult

        // 2) Иначе — ML Kit
        if (!modelReady) {
            val ok = init(context)
            if (!ok) return null
        }

        return suspendCancellableCoroutine { cont ->
            mlkTranslator!!.translate(text)
                .addOnSuccessListener { result ->
                    // 3) Пост-обработка: применяем словарь Dawncaster к ML-переводу
                    cont.resume(applyGlossaryToTranslation(result))
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }

    // ──────────────────────────────────────────
    //  СЛОВАРЬ DAWNCASTER
    //  (дополняется по мере игры)
    // ──────────────────────────────────────────

    /** Точное совпадение фразы → перевод Dawncaster */
    private val exactGlossary = mapOf(
        "Bleed" to "Кровоток",
        "Bleeding" to "Кровоточащий",
        "Barrier" to "Барьер",
        "Fury" to "Ярость",
        "Anger" to "Гнев",
        "Blessed" to "Благословлённый",
        "Burn" to "Горение",
        "Burning" to "Горящий",
        "Charm" to "Очарование",
        "Charmed" to "Очарован",
        "Corruption" to "Скверна",
        "Corrupted" to "Осквернённый",
        "Critical" to "Крит",
        "Dazed" to "Оглушение",
        "Dodge" to "Уклонение",
        "Doom" to "Погибель",
        "Energized" to "Заряженный",
        "Evasion" to "Увёртка",
        "Focus" to "Концентрация",
        "Fortify" to "Укрепление",
        "Frozen" to "Замороженный",
        "Haste" to "Ускорение",
        "Immune" to "Иммунитет",
        "Impair" to "Ослабление",
        "Memorized" to "Запомнено",
        "Momentum" to "Инерция",
        "Poison" to "Яд",
        "Poisoned" to "Отравлен",
        "Resistance" to "Сопротивление",
        "Sanctify" to "Освящение",
        "Shield" to "Щит",
        "Shocked" to "Шокирован",
        "Silence" to "Безмолвие",
        "Siphon" to "Вытягивание",
        "Stun" to "Оглушение",
        "Stunned" to "Оглушён",
        "Sunder" to "Раскол",
        "Vulnerable" to "Уязвимость",
        "Ward" to "Оберег",
        "Weaken" to "Ослабление",
        "Weakened" to "Ослаблен",
        "Wet" to "Промокший",
        // Карточные термины
        "Basic Attack" to "Базовая атака",
        "Equipment" to "Снаряжение",
        "Affliction" to "Порча",
        "Action" to "Действие",
        "Talent" to "Талант",
        "Enchantment" to "Чары",
        "Invocation" to "Призыв",
        "Conjuration" to "Заклинание",
        "Imbue" to "Насыщение",
    )

    /** Замена слов внутри ML-перевода */
    private val partialGlossary = mapOf(
        "bleed" to "кровоток",
        "bleeding" to "кровоточащий",
        "barrier" to "барьер",
        "fury" to "ярость",
        "anger" to "гнев",
        "blessed" to "благословлённый",
        "burn" to "горение",
        "burning" to "горящий",
        "charm" to "очарование",
        "charmed" to "очарован",
        "corruption" to "скверна",
        "corrupted" to "осквернённый",
        "critical" to "крит",
        "dazed" to "оглушение",
        "dodge" to "уклонение",
        "doom" to "погибель",
        "energized" to "заряженный",
        "evasion" to "увёртка",
        "focus" to "концентрация",
        "fortify" to "укрепление",
        "frozen" to "замороженный",
        "haste" to "ускорение",
        "immune" to "иммунитет",
        "impair" to "ослабление",
        "memorized" to "запомнено",
        "momentum" to "инерция",
        "poison" to "яд",
        "poisoned" to "отравлен",
        "resistance" to "сопротивление",
        "sanctify" to "освящение",
        "shield" to "щит",
        "shocked" to "шокирован",
        "silence" to "безмолвие",
        "siphon" to "вытягивание",
        "stun" to "оглушение",
        "stunned" to "оглушён",
        "sunder" to "раскол",
        "vulnerable" to "уязвимость",
        "ward" to "оберег",
        "weaken" to "ослабление",
        "weakened" to "ослаблен",
        "wet" to "промокший",
        "basic attack" to "базовая атака",
        "equipment" to "снаряжение",
        "affliction" to "порча",
        "action" to "действие",
        "talent" to "талант",
        "enchantment" to "чары",
        "invocation" to "призыв",
        "conjuration" to "заклинание",
        "imbue" to "насыщение",
    )

    /**
     * Проверяет точное совпадение всей фразы со словарём.
     * Возвращает перевод, если фраза — известный игровой термин.
     */
    private fun translateGlossary(text: String): String? {
        return exactGlossary[text.trim()]
    }

    /**
     * Применяет замену терминов к ML-переводу.
     * Например «применить 2 кровотечения» → «накладывает 2 Кровотока»
     */
    private fun applyGlossaryToTranslation(translation: String): String {
        var result = translation
        for ((eng, rus) in partialGlossary) {
            // Игнорируем регистр для замены внутри ML-перевода
            result = result.replace(eng, rus, ignoreCase = true)
        }
        return result
    }
}
