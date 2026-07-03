package com.svobo.tripspot.data.remote

import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.TripEntity

interface TripSpotApi {
    suspend fun syncTrip(trip: TripEntity): Result<Unit>
    suspend fun syncSpot(spot: SpotEntity): Result<Unit>
}

class FakeTripSpotApi : TripSpotApi {
    override suspend fun syncTrip(trip: TripEntity): Result<Unit> = Result.success(Unit)
    override suspend fun syncSpot(spot: SpotEntity): Result<Unit> = Result.success(Unit)
}
