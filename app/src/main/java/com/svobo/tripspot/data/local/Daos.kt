package com.svobo.tripspot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.SpotPhotoEntity
import com.svobo.tripspot.data.model.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY updatedAt DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    fun observeTrip(id: String): Flow<TripEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<TripEntity>

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots WHERE tripId = :tripId ORDER BY orderIndex ASC, visitedAt ASC")
    fun observeSpots(tripId: String): Flow<List<SpotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(spot: SpotEntity)

    @Query("SELECT COUNT(*) FROM spots WHERE tripId = :tripId")
    suspend fun countForTrip(tripId: String): Int

    @Query("DELETE FROM spots WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM spots")
    suspend fun getAll(): List<SpotEntity>

    @Query("DELETE FROM spots")
    suspend fun deleteAll()
}

@Dao
interface SpotPhotoDao {
    @Query("SELECT * FROM spot_photos WHERE spotId = :spotId ORDER BY orderIndex ASC, createdAt ASC")
    fun observePhotos(spotId: String): Flow<List<SpotPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: SpotPhotoEntity)

    @Query("DELETE FROM spot_photos WHERE id = :photoId")
    suspend fun delete(photoId: String)

    @Query("DELETE FROM spot_photos WHERE spotId = :spotId")
    suspend fun deleteAllForSpot(spotId: String)

    @Query("SELECT COUNT(*) FROM spot_photos WHERE spotId = :spotId")
    suspend fun countForSpot(spotId: String): Int

    @Query("SELECT * FROM spot_photos WHERE spotId IN (SELECT id FROM spots WHERE tripId = :tripId)")
    fun observePhotosForTrip(tripId: String): Flow<List<SpotPhotoEntity>>
}
