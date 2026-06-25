package com.fini.todoapp.data.api

import com.fini.todoapp.data.model.CategoryRequest
import com.fini.todoapp.data.model.CategoryResponse
import retrofit2.http.*

interface CategoryApi {

    @GET("api/categories")
    suspend fun getCategories(): List<CategoryResponse>

    @POST("api/categories")
    suspend fun createCategory(
        @Body request: CategoryRequest
    ): CategoryResponse

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: CategoryRequest
    ): CategoryResponse

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: String
    )
}