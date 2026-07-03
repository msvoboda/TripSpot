package com.svobo.tripspot

import android.app.Application
import com.svobo.tripspot.data.local.TripSpotDatabase
import com.svobo.tripspot.data.remote.FakeTripSpotApi
import com.svobo.tripspot.data.repository.TripRepository

class TripSpotApplication : Application() {
    val database: TripSpotDatabase by lazy { TripSpotDatabase.create(this) }
    val repository: TripRepository by lazy {
        TripRepository(
            tripDao = database.tripDao(),
            spotDao = database.spotDao(),
            spotPhotoDao = database.spotPhotoDao(),
            api = FakeTripSpotApi(),
        )
    }
}
