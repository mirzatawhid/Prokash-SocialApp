package com.dcoder.prokash.data.api

import com.dcoder.prokash.data.model.NominatimResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {
    @GET("search")
    suspend fun searchAddress(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("limit") limit: Int = 5
    ): List<NominatimResponse>
}