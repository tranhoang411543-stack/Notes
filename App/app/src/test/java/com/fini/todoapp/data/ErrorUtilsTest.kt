package com.fini.todoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorUtilsTest {

    @Test
    fun networkErrorsShowNoInternetMessage() {
        assertEquals("Không có internet", ErrorUtils.getMessage(UnknownHostException()))
        assertEquals("Không có internet", ErrorUtils.getMessage(SocketException("Network is unreachable")))
        assertEquals("Không có internet", ErrorUtils.getMessage(SocketTimeoutException("timeout")))
        assertEquals(
            "Không có internet",
            ErrorUtils.getMessage(IOException("Unable to resolve host \"example.com\""))
        )
    }
}
