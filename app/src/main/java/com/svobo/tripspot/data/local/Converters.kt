package com.svobo.tripspot.data.local

import androidx.room.TypeConverter
import com.svobo.tripspot.data.model.SyncStatus

class Converters {
    @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
    @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
