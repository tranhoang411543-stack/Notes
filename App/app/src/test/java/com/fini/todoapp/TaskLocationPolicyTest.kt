package com.fini.todoapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLocationPolicyTest {

    @Test
    fun savedAddressLabelUsesNamedAddressInsteadOfCoordinates() {
        assertEquals(
            "Nhà anh xxx",
            TaskLocationPolicy.savedAddressLabel(
                locationName = " Nhà anh xxx ",
                address = "10.12345, 106.12345"
            )
        )
    }

    @Test
    fun savedAddressLabelFallsBackToAddressName() {
        assertEquals(
            "Công ty",
            TaskLocationPolicy.savedAddressLabel(
                locationName = null,
                address = " Công ty "
            )
        )
    }

    @Test
    fun savedAddressLabelDoesNotExposeCoordinateAddress() {
        assertEquals(
            "Địa chỉ đã lưu",
            TaskLocationPolicy.savedAddressLabel(
                locationName = null,
                address = "10.12345, 106.12345"
            )
        )
    }

    @Test
    fun directionsOnlyShowForReopenedSavedTaskLocation() {
        assertFalse(
            TaskLocationPolicy.shouldShowDirections(
                isExistingTask = false,
                hasStoredLocation = true,
                locationChangedThisSession = false
            )
        )
        assertFalse(
            TaskLocationPolicy.shouldShowDirections(
                isExistingTask = true,
                hasStoredLocation = true,
                locationChangedThisSession = true
            )
        )
        assertTrue(
            TaskLocationPolicy.shouldShowDirections(
                isExistingTask = true,
                hasStoredLocation = true,
                locationChangedThisSession = false
            )
        )
    }
}
