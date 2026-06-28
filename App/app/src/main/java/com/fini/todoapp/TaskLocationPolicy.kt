package com.fini.todoapp

object TaskLocationPolicy {
    private const val DEFAULT_ADDRESS_LABEL = "Địa chỉ đã lưu"
    private val COORDINATE_PAIR = Regex("""^-?\d+(\.\d+)?\s*,\s*-?\d+(\.\d+)?$""")

    fun savedAddressLabel(locationName: String?, address: String?): String {
        return listOf(locationName, address)
            .firstOrNull { !it.isNullOrBlank() && !COORDINATE_PAIR.matches(it.trim()) }
            ?.trim()
            ?: DEFAULT_ADDRESS_LABEL
    }

    fun shouldShowDirections(
        isExistingTask: Boolean,
        hasStoredLocation: Boolean,
        locationChangedThisSession: Boolean
    ): Boolean {
        return isExistingTask && hasStoredLocation && !locationChangedThisSession
    }
}
