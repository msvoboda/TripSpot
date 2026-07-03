package com.svobo.tripspot.data.repository

import com.svobo.tripspot.data.local.SpotDao
import com.svobo.tripspot.data.local.SpotPhotoDao
import com.svobo.tripspot.data.local.TripDao
import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.SpotPhotoEntity
import com.svobo.tripspot.data.model.TripEntity
import com.svobo.tripspot.data.model.SyncStatus
import com.svobo.tripspot.data.remote.TripSpotApi
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class TripRepository(
    private val tripDao: TripDao,
    private val spotDao: SpotDao,
    private val spotPhotoDao: SpotPhotoDao,
    private val api: TripSpotApi,
) {
    fun observeTrips(): Flow<List<TripEntity>> = tripDao.observeTrips()
    fun observeTrip(id: String): Flow<TripEntity?> = tripDao.observeTrip(id)
    fun observeSpots(tripId: String): Flow<List<SpotEntity>> = spotDao.observeSpots(tripId)
    fun observePhotos(spotId: String): Flow<List<SpotPhotoEntity>> = spotPhotoDao.observePhotos(spotId)
    fun observePhotosForTrip(tripId: String): Flow<List<SpotPhotoEntity>> = spotPhotoDao.observePhotosForTrip(tripId)

    suspend fun createTrip(
        title: String,
        description: String? = null,
        coverImageUri: String? = null,
    ): TripEntity {
        val now = System.currentTimeMillis()
        val trip = TripEntity(
            title = title,
            description = description,
            coverImageUri = coverImageUri,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        tripDao.upsert(trip)
        // TODO: When real API exists, mark as PENDING_SYNC and handle Result.
        api.syncTrip(trip)
        return trip
    }

    suspend fun updateTrip(
        trip: TripEntity,
    ) {
        val updated = trip.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        tripDao.upsert(updated)
        api.syncTrip(updated)
    }

    suspend fun deleteTrip(tripId: String) {
        // TODO: Consider cascading delete of spots and photos or marking as DELETED_PENDING_SYNC.
        tripDao.delete(tripId)
    }

    suspend fun addSpot(
        tripId: String,
        title: String,
        note: String? = null,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        visitedAt: Long? = System.currentTimeMillis(),
    ) {
        val order = spotDao.countForTrip(tripId) + 1
        val spot = SpotEntity(
            tripId = tripId,
            title = title,
            note = note,
            address = address,
            latitude = latitude,
            longitude = longitude,
            visitedAt = visitedAt,
            orderIndex = order,
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        spotDao.upsert(spot)
        api.syncSpot(spot)
    }

    suspend fun updateSpot(spot: SpotEntity) {
        val updated = spot.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        spotDao.upsert(updated)
        api.syncSpot(updated)
    }

    suspend fun deleteSpot(spotId: String) {
        // TODO: Consider marking as DELETED_PENDING_SYNC and deleting photos.
        spotDao.delete(spotId)
    }

    suspend fun addPhotoThumbnailForSpot(
        spotId: String,
        thumbnail: ByteArray,
        originalUri: String?,
        caption: String?,
    ): SpotPhotoEntity {
        val order = spotPhotoDao.countForSpot(spotId) + 1
        val now = System.currentTimeMillis()
        val photo = SpotPhotoEntity(
            spotId = spotId,
            thumbnail = thumbnail,
            originalUri = originalUri,
            remoteUrl = null,
            caption = caption,
            orderIndex = order,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        spotPhotoDao.upsert(photo)
        // TODO: Call API when photo upload and sync are available.
        return photo
    }

    suspend fun deletePhoto(photoId: String) {
        spotPhotoDao.delete(photoId)
    }

    suspend fun exportJson(): String {
        val trips = tripDao.getAll()
        val spots = spotDao.getAll()

        val root = JSONObject()
        root.put("version", 1)

        val tripsArray = JSONArray()
        trips.forEach { trip ->
            val obj = JSONObject()
            obj.put("id", trip.id)
            obj.put("remoteId", trip.remoteId)
            obj.put("title", trip.title)
            obj.put("description", trip.description)
            obj.put("coverImageUri", trip.coverImageUri)
            obj.put("createdAt", trip.createdAt)
            obj.put("updatedAt", trip.updatedAt)
            obj.put("syncStatus", trip.syncStatus.name)
            tripsArray.put(obj)
        }
        root.put("trips", tripsArray)

        val spotsArray = JSONArray()
        spots.forEach { spot ->
            val obj = JSONObject()
            obj.put("id", spot.id)
            obj.put("tripId", spot.tripId)
            obj.put("remoteId", spot.remoteId)
            obj.put("title", spot.title)
            obj.put("note", spot.note)
            obj.put("address", spot.address)
            obj.put("latitude", spot.latitude)
            obj.put("longitude", spot.longitude)
            obj.put("locationSource", spot.locationSource)
            obj.put("locationAccuracy", spot.locationAccuracy)
            obj.put("visitedAt", spot.visitedAt)
            obj.put("orderIndex", spot.orderIndex)
            obj.put("createdAt", spot.createdAt)
            obj.put("updatedAt", spot.updatedAt)
            obj.put("syncStatus", spot.syncStatus.name)
            spotsArray.put(obj)
        }
        root.put("spots", spotsArray)

        return root.toString()
    }

    suspend fun importJson(json: String) {
        val root = JSONObject(json)
        val tripsArray = root.optJSONArray("trips") ?: JSONArray()
        val spotsArray = root.optJSONArray("spots") ?: JSONArray()

        tripDao.deleteAll()
        spotDao.deleteAll()

        for (i in 0 until tripsArray.length()) {
            val obj = tripsArray.getJSONObject(i)
            val syncStatusName = obj.optString("syncStatus", SyncStatus.LOCAL_ONLY.name)
            val trip = TripEntity(
                id = obj.optString("id"),
                remoteId = obj.optString("remoteId", null),
                title = obj.optString("title"),
                description = obj.optString("description", null),
                coverImageUri = obj.optString("coverImageUri", null),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                syncStatus = runCatching { SyncStatus.valueOf(syncStatusName) }.getOrElse { SyncStatus.LOCAL_ONLY },
            )
            tripDao.upsert(trip)
        }

        for (i in 0 until spotsArray.length()) {
            val obj = spotsArray.getJSONObject(i)
            val syncStatusName = obj.optString("syncStatus", SyncStatus.LOCAL_ONLY.name)
            val latitude: Double? = if (!obj.isNull("latitude")) obj.optDouble("latitude") else null
            val longitude: Double? = if (!obj.isNull("longitude")) obj.optDouble("longitude") else null
            val locationAccuracy: Double? = if (!obj.isNull("locationAccuracy")) obj.optDouble("locationAccuracy") else null
            val visitedAt: Long? = if (!obj.isNull("visitedAt")) obj.optLong("visitedAt") else null

            val spot = SpotEntity(
                id = obj.optString("id"),
                tripId = obj.optString("tripId"),
                remoteId = obj.optString("remoteId", null),
                title = obj.optString("title"),
                note = obj.optString("note", null),
                address = obj.optString("address", null),
                latitude = latitude,
                longitude = longitude,
                locationSource = obj.optString("locationSource", null),
                locationAccuracy = locationAccuracy,
                visitedAt = visitedAt,
                orderIndex = obj.optInt("orderIndex", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                syncStatus = runCatching { SyncStatus.valueOf(syncStatusName) }.getOrElse { SyncStatus.LOCAL_ONLY },
            )
            spotDao.upsert(spot)
        }
    }
}
