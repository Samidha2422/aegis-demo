package com.aegis.demo.data.api

import com.aegis.demo.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiService {
    @GET("personas")
    suspend fun getPersonas(): List<Persona>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): LoginResult

    @GET("engine/status")
    suspend fun getEngineStatus(): EngineStatus

    @GET("chats")
    suspend fun getChats(@Query("actor_id") actorId: String = "meera"): List<ChatThread>

    @GET("chats/{id}/messages")
    suspend fun getMessages(@Path("id") id: String): List<SafeMessage>

    @POST("chats/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body body: Map<String, String>): SafeMessage

    @Multipart
    @POST("chats/{id}/media")
    suspend fun uploadMedia(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @Part("kind") kind: RequestBody,
        @Part("sender_id") sender: RequestBody
    ): SafeMessage

    @POST("users/{id}/block")
    suspend fun blockUser(@Path("id") id: String, @Body body: Map<String, String>): Response<Unit>

    @POST("users/{id}/unblock")
    suspend fun unblockUser(@Path("id") id: String, @Body body: Map<String, String>): Response<Unit>

    @POST("users/{id}/mute")
    suspend fun muteUser(@Path("id") id: String, @Body body: Map<String, String>): Response<Unit>

    @POST("reports")
    suspend fun submitReport(@Body body: Map<String, String>): Response<Unit>

    @POST("nudge")
    suspend fun checkNudge(@Body body: Map<String, String>): NudgeResult

    @GET("feed/posts")
    suspend fun getPosts(): List<Post>

    @POST("feed/posts/{id}/comments")
    suspend fun addComment(@Path("id") id: String, @Body body: Map<String, String>): Comment

    @GET("calls/log")
    suspend fun getCalls(): List<CallRecord>

    @POST("calls/simulate")
    suspend fun simulateCall(@Body body: Map<String, String>): CallRecord

    @POST("calls/log/import")
    suspend fun importCallLog(@Body body: RequestBody): Response<Unit>

    @GET("demo/baseline")
    suspend fun getBaseline(@Query("conversation") conversation: String): BaselineComparison

    @GET("evidence/export")
    suspend fun exportEvidence(@Query("format") format: String): okhttp3.ResponseBody

    @POST("demo/scenarios/{n}/play")
    suspend fun playScenario(@Path("n") n: Int, @Query("speed") speed: Int): Response<Unit>

    @POST("demo/reset")
    suspend fun resetDemo(): Response<Unit>
}
