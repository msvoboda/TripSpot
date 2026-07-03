package com.svobo.tripspot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.SpotPhotoEntity
import com.svobo.tripspot.data.model.TripEntity

@Database(entities = [TripEntity::class, SpotEntity::class, SpotPhotoEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TripSpotDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun spotDao(): SpotDao
    abstract fun spotPhotoDao(): SpotPhotoDao

    companion object {
        fun create(context: Context): TripSpotDatabase = Room.databaseBuilder(
            context.applicationContext,
            TripSpotDatabase::class.java,
            "tripspot.db"
        ).fallbackToDestructiveMigration().build()
    }
}
