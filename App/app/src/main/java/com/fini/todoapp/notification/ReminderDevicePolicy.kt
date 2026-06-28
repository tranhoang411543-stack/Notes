package com.fini.todoapp.notification

internal object ReminderDevicePolicy {
    private const val UI_MODE_TYPE_CAR = 3

    fun isAutomotiveDevice(
        hasAutomotiveFeature: Boolean,
        uiModeType: Int?,
        device: String,
        model: String,
        fingerprint: String,
        screenWidthDp: Int,
        screenHeightDp: Int
    ): Boolean {
        return hasAutomotiveFeature ||
            uiModeType == UI_MODE_TYPE_CAR ||
            device.isCarBuildValue() ||
            model.isCarBuildValue() ||
            fingerprint.isCarBuildValue()
    }

    private fun String.isCarBuildValue(): Boolean {
        return contains("car", ignoreCase = true) ||
            contains("automotive", ignoreCase = true)
    }
}
