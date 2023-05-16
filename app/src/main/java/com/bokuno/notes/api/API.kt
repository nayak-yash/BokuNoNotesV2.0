package com.bokuno.notes.api

import com.bokuno.notes.models.HelpResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface API {

    @POST("/v1/completions")
    suspend fun getPrompt(
        @Header("Content-Type") contentType: String,
        @Header("Authorization") authorization: String,
        @Body requestBody: RequestBody) : HelpResponse
}