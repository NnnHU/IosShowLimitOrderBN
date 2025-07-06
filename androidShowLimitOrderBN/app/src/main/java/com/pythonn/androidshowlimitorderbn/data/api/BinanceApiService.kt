package com.pythonn.androidshowlimitorderbn.data.api

import com.pythonn.androidshowlimitorderbn.data.models.OrderBookSnapshot
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {

    @GET("depth")
    suspend fun getSpotOrderBookSnapshot(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 1000
    ): OrderBookSnapshot

    @GET("depth")
    suspend fun getFuturesOrderBookSnapshot(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 1000
    ): OrderBookSnapshot
}