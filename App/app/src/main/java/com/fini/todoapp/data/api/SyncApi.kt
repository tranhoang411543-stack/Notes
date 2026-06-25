package com.fini.todoapp.data.api

import com.fini.todoapp.data.model.SyncRequest
import com.fini.todoapp.data.model.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncApi {
    @POST("api/sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>
}
