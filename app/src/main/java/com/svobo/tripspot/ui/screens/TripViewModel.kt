package com.svobo.tripspot.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.SpotPhotoEntity
import com.svobo.tripspot.data.model.TripEntity
import com.svobo.tripspot.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripViewModel(private val repository: TripRepository) : ViewModel() {
    val trips: StateFlow<List<TripEntity>> = repository.observeTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedTripId = MutableStateFlow<String?>(null)

    val spots: StateFlow<List<SpotEntity>> = selectedTripId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else repository.observeSpots(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class SpotWithPhotos(
        val spot: SpotEntity,
        val photoCount: Int,
    )

    val spotsWithPhotos: StateFlow<List<SpotWithPhotos>> = selectedTripId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else combine(
            repository.observeSpots(id),
            repository.observePhotosForTrip(id),
        ) { spots, photos ->
            val counts = photos.groupingBy { it.spotId }.eachCount()
            spots.map { spot ->
                SpotWithPhotos(
                    spot = spot,
                    photoCount = counts[spot.id] ?: 0,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTrip(id: String?) { selectedTripId.value = id }

    fun createTrip(title: String, description: String?, coverImageUri: String?) = viewModelScope.launch {
        repository.createTrip(title = title, description = description, coverImageUri = coverImageUri)
    }

    fun updateTrip(trip: TripEntity) = viewModelScope.launch {
        repository.updateTrip(trip)
    }

    fun deleteTrip(tripId: String) = viewModelScope.launch {
        repository.deleteTrip(tripId)
    }

    fun createSpot(
        tripId: String,
        title: String,
        note: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        visitedAt: Long?,
    ) = viewModelScope.launch {
        repository.addSpot(tripId, title, note, address, latitude, longitude, visitedAt)
    }

    fun updateSpot(spot: SpotEntity) = viewModelScope.launch {
        repository.updateSpot(spot)
    }

    fun deleteSpot(spotId: String) = viewModelScope.launch {
        repository.deleteSpot(spotId)
    }

    fun observePhotos(spotId: String) = repository.observePhotos(spotId)

    fun addPhotoThumbnailForSpot(
        spotId: String,
        thumbnail: ByteArray,
        originalUri: String?,
        caption: String?,
    ) = viewModelScope.launch {
        repository.addPhotoThumbnailForSpot(spotId, thumbnail, originalUri, caption)
    }

    fun deletePhoto(photoId: String) = viewModelScope.launch {
        repository.deletePhoto(photoId)
    }

    fun createDemoTrip() = viewModelScope.launch {
        val trip = repository.createTrip("Přechod Pálavy", "Ukázkový trip složený ze spotů.")
        repository.addSpot(trip.id, "Děvín", "Krásný výhled na Novomlýnské nádrže.")
        repository.addSpot(trip.id, "Dívčí hrady", "Zřícenina a výhledy do krajiny Pálavy.")
        repository.addSpot(trip.id, "Mikulov", "Procházka historickým centrem.")
    }

    suspend fun exportBackupJson(): String = repository.exportJson()

    suspend fun importBackupJson(json: String) {
        repository.importJson(json)
    }
}
