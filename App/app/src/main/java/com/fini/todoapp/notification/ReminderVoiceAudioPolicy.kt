package com.fini.todoapp.notification

internal enum class ReminderVoiceUsage {
    NavigationGuidance,
    Assistant,
    Media
}

internal data class ReminderVoiceCandidate(
    val name: String,
    val language: String,
    val country: String,
    val isDefault: Boolean,
    val requiresNetwork: Boolean
)

internal object ReminderVoiceAudioPolicy {
    fun usageFor(isAutomotive: Boolean): ReminderVoiceUsage {
        return usagesFor(isAutomotive).first()
    }

    fun usagesFor(isAutomotive: Boolean): List<ReminderVoiceUsage> {
        return if (isAutomotive) {
            // NavigationGuidance: chuẩn cho automotive nhưng có thể bị delay focus.
            // Assistant: route trực tiếp đến cabin speaker, ít bị chặn nhất trên AAOS emulator.
            // Media: fallback cuối cùng.
            listOf(ReminderVoiceUsage.NavigationGuidance, ReminderVoiceUsage.Assistant, ReminderVoiceUsage.Media)
        } else {
            listOf(ReminderVoiceUsage.Media)
        }
    }

    fun selectVoice(
        candidates: List<ReminderVoiceCandidate>,
        defaultLanguage: String,
        defaultCountry: String
    ): ReminderVoiceCandidate? {
        return candidates.firstOrNull { it.isVietnamese() && !it.requiresNetwork } ?:
            candidates.firstOrNull { it.language.equals("vi", ignoreCase = true) && !it.requiresNetwork } ?:
            candidates.firstOrNull {
                it.language.equals(defaultLanguage, ignoreCase = true) &&
                    it.country.equals(defaultCountry, ignoreCase = true) &&
                    !it.requiresNetwork
            } ?:
            candidates.firstOrNull { it.isDefault && !it.requiresNetwork } ?:
            candidates.firstOrNull { !it.requiresNetwork } ?:
            candidates.firstOrNull { it.isVietnamese() } ?:
            candidates.firstOrNull { it.language.equals("vi", ignoreCase = true) } ?:
            candidates.firstOrNull { it.isDefault } ?:
            candidates.firstOrNull()
    }

    private fun ReminderVoiceCandidate.isVietnamese(): Boolean {
        return language.equals("vi", ignoreCase = true) &&
            country.equals("VN", ignoreCase = true)
    }
}
