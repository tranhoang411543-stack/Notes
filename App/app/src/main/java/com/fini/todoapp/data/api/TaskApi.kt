package com.fini.todoapp.data.api

import com.fini.todoapp.data.model.TaskRequest
import com.fini.todoapp.data.model.TaskResponse
import retrofit2.http.*

interface TaskApi {

    @GET("api/tasks")
    suspend fun getTasks(
        @Query("dateFilter") dateFilter: String = "ALL",
        @Query("categoryId") categoryId: String? = null,
        @Query("keyword") keyword: String? = null
    ): List<TaskResponse>

    @GET("api/tasks/{id}")
    suspend fun getTaskById(
        @Path("id") id: String
    ): TaskResponse

    @GET("api/tasks/trash")
    suspend fun getTrash(): List<TaskResponse>

    @POST("api/tasks")
    suspend fun createTask(
        @Body request: TaskRequest
    ): TaskResponse

    @PUT("api/tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body request: TaskRequest
    ): TaskResponse

    @PATCH("api/tasks/{id}/complete")
    suspend fun setCompleted(
        @Path("id") id: String,
        @Query("completed") completed: Boolean
    ): TaskResponse

    @PATCH("api/tasks/{id}/trash")
    suspend fun trashTask(
        @Path("id") id: String
    ): TaskResponse

    @PATCH("api/tasks/{id}/restore")
    suspend fun restoreTask(
        @Path("id") id: String
    ): TaskResponse

    @DELETE("api/tasks/trash")
    suspend fun clearTrash()
}
