package com.svobo.tripspot.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val title: String,
    val description: String? = null,
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val remoteId: String? = null,
    val title: String,
    val note: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationSource: String? = null,
    val locationAccuracy: Double? = null,
    val visitedAt: Long? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)

@Entity(tableName = "spot_photos")
data class SpotPhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val spotId: String,
    val remoteId: String? = null,
    val thumbnail: ByteArray,
    val originalUri: String? = null,
    val remoteUrl: String? = null,
    val caption: String? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)

enum class SyncStatus {
    LOCAL_ONLY,
    PENDING_SYNC,
    SYNCED,
    SYNC_FAILED,
    DELETED_PENDING_SYNC,
}
