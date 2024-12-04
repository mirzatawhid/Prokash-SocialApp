package com.dcoder.prokash.data.api

import com.dcoder.prokash.data.model.NominatimResponse
import com.dcoder.prokash.data.model.NominatimReverseResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimReverseService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json"): NominatimReverseResponse
}