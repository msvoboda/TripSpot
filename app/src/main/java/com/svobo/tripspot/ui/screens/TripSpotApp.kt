package com.svobo.tripspot.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.content.ContentValues
import android.provider.MediaStore
import android.graphics.RectF
import kotlinx.coroutines.launch
import java.util.Locale
import com.svobo.tripspot.data.model.SpotEntity
import com.svobo.tripspot.data.model.SpotPhotoEntity
import com.svobo.tripspot.data.model.TripEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSpotApp(viewModel: TripViewModel) {
    val trips by viewModel.trips.collectAsState()
     val spotsWithPhotos by viewModel.spotsWithPhotos.collectAsState()
    var selectedTrip by remember { mutableStateOf<TripEntity?>(null) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var isCreatingTrip by remember { mutableStateOf(false) }
    var selectedSpot by remember { mutableStateOf<SpotEntity?>(null) }
    var editingSpot by remember { mutableStateOf<SpotEntity?>(null) }
    var showQuickSpotDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Po startu aplikace si jednorázově řekneme o povolení k poloze,
    // aby byla GPS připravená pro automatické doplňování spotů.
    val startupLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Bez povolení k poloze nebude možné automaticky doplňovat GPS",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFinePermission) {
            startupLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val canGoBack = isCreatingTrip || editingTrip != null || editingSpot != null || selectedSpot != null || selectedTrip != null

    val handleBack: () -> Unit = {
        when {
            isCreatingTrip || editingTrip != null -> {
                editingTrip = null
                isCreatingTrip = false
            }
            editingSpot != null -> editingSpot = null
            selectedSpot != null -> selectedSpot = null
            selectedTrip != null -> {
                selectedTrip = null
                viewModel.selectTrip(null)
            }
        }
    }

     BackHandler(enabled = canGoBack) {
        handleBack()
    }
    val addPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        val spot = selectedSpot
        if (uri != null && spot != null) {
            val bytes = loadThumbnailBytes(context, uri)
            if (bytes != null) {
                viewModel.addPhotoThumbnailForSpot(
                    spotId = spot.id,
                    thumbnail = bytes,
                    originalUri = uri.toString(),
                    caption = null,
                )
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportBackupJson()
                val output = context.contentResolver.openOutputStream(uri)
                if (output != null) {
                    output.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Záloha byla exportována", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nepodařilo se otevřít soubor pro export", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val input = context.contentResolver.openInputStream(uri)
                val json = input?.bufferedReader()?.use { it.readText() } ?: ""
                if (json.isNotBlank()) {
                    try {
                        viewModel.importBackupJson(json)
                        Toast.makeText(context, "Záloha byla importována", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Import zálohy se nezdařil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Soubor zálohy je prázdný", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        editingTrip != null && editingTrip?.id != null -> if (editingTrip!!.title.isNotEmpty()) "Upravit trip" else "Nový trip"
                        editingSpot != null -> "Upravit spot"
                        selectedSpot != null -> selectedSpot!!.title
                        selectedTrip != null -> selectedTrip!!.title
                        else -> "TripSpot"
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = handleBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isCreatingTrip && editingTrip == null && editingSpot == null && selectedSpot == null) {
                FloatingActionButton(onClick = {
                    if (selectedTrip == null) {
                        if (trips.isEmpty()) {
                            viewModel.createDemoTrip()
                         } else {
                             // Start creating a new trip
                             isCreatingTrip = true
                             editingTrip = null
                         }
                      } else {
                         if (selectedTrip != null) {
                             showQuickSpotDialog = true
                         }
                     }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Přidat")
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            isCreatingTrip || editingTrip != null -> {
                AddEditTripScreen(
                    initial = editingTrip,
                    modifier = contentModifier,
                    onSave = { title, description, coverImageUri ->
                        if (editingTrip == null) {
                            viewModel.createTrip(title, description, coverImageUri)
                        } else {
                            viewModel.updateTrip(
                                editingTrip!!.copy(
                                    title = title,
                                    description = description,
                                    coverImageUri = coverImageUri,
                                )
                            )
                        }
                        editingTrip = null
                        isCreatingTrip = false
                    },
                    onCancel = {
                        editingTrip = null
                        isCreatingTrip = false
                    }
                )
            }
            selectedTrip == null -> {
                TripListScreen(
                    trips = trips,
                    modifier = contentModifier,
                    onCreateDemo = { viewModel.createDemoTrip() },
                    onTripClick = { trip ->
                        selectedTrip = trip
                        viewModel.selectTrip(trip.id)
                    },
                    onEditTrip = { trip ->
                        editingTrip = trip
                        isCreatingTrip = false
                    },
                    onDeleteTrip = { trip -> viewModel.deleteTrip(trip.id) },
                    onExportBackup = {
                        exportBackupLauncher.launch("tripspot-backup.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                    },
                )
            }
            editingSpot != null -> {
                AddEditSpotScreen(
                    initial = editingSpot,
                    modifier = contentModifier,
                    onSave = { title, note, latitude, longitude, address ->
                        if (editingSpot == null) {
                            val trip = selectedTrip ?: return@AddEditSpotScreen
                            viewModel.createSpot(
                                tripId = trip.id,
                                title = title,
                                note = note,
                                address = address,
                                latitude = latitude,
                                longitude = longitude,
                                visitedAt = System.currentTimeMillis(),
                            )
                        } else {
                            viewModel.updateSpot(
                                editingSpot!!.copy(
                                    title = title,
                                    note = note,
                                    address = address,
                                    latitude = latitude,
                                    longitude = longitude,
                                )
                            )
                        }
                        editingSpot = null
                    },
                    onCancel = { editingSpot = null },
                )
            }
            selectedSpot != null -> {
                val photos by viewModel.observePhotos(selectedSpot!!.id).collectAsState(initial = emptyList())
                SpotDetailScreen(
                    spot = selectedSpot!!,
                    tripTitle = selectedTrip?.title,
                    modifier = contentModifier,
                    photos = photos,
                    onAddPhoto = { addPhotoLauncher.launch("image/*") },
                    onEdit = { spot -> editingSpot = spot },
                    onDelete = { spot ->
                        viewModel.deleteSpot(spot.id)
                        selectedSpot = null
                    },
                    onDeletePhoto = { photo -> viewModel.deletePhoto(photo.id) },
                )
            }
             else -> {
                 TripTimelineScreen(
                     spots = spotsWithPhotos,
                     modifier = contentModifier,
                     onAddSpot = {
                         if (selectedTrip != null) {
                             showQuickSpotDialog = true
                         }
                     },
                     onSpotClick = { spot -> selectedSpot = spot },
                     onEditSpot = { spot -> editingSpot = spot },
                     onDeleteSpot = { spot -> viewModel.deleteSpot(spot.id) },
                 )
             }
        }
        if (showQuickSpotDialog && selectedTrip != null) {
            QuickCreateSpotDialog(
                onConfirm = { title, note ->
                    val trip = selectedTrip ?: return@QuickCreateSpotDialog
                    val location = getLastKnownLocation(context)
                    val latitude = location?.latitude
                    val longitude = location?.longitude
                    val address = if (latitude != null && longitude != null) {
                        reverseGeocode(context, latitude, longitude)
                    } else {
                        null
                    }
                    if (location != null) {
                        Toast.makeText(
                            context,
                            "GPS byla automaticky doplněna k novému spotu",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Nový spot je bez GPS – povol prosím polohu",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    viewModel.createSpot(
                        tripId = trip.id,
                        title = title,
                        note = note,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        visitedAt = System.currentTimeMillis(),
                    )
                    if (spotsWithPhotos.size >= 3) {
                        Toast.makeText(
                            context,
                            "Spot byl přidán do roadbooku",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    showQuickSpotDialog = false
                },
                onDismiss = { showQuickSpotDialog = false },
            )
        }
    }
}

@Composable
private fun AddEditTripScreen(
    initial: TripEntity?,
    modifier: Modifier = Modifier,
    onSave: (String, String?, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var coverImageUri by remember(initial) { mutableStateOf(initial?.coverImageUri.orEmpty()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Název tripu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Popis") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            minLines = 3,
        )
        OutlinedTextField(
            value = coverImageUri,
            onValueChange = { coverImageUri = it },
            label = { Text("Cover image URI (dočasně text)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.ifBlank { null }, coverImageUri.ifBlank { null })
                    }
                },
            ) {
                Text("Uložit")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Zrušit")
            }
        }
    }
}

@Composable
private fun AddEditSpotScreen(
    initial: SpotEntity?,
    modifier: Modifier = Modifier,
    onSave: (String, String?, Double?, Double?, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var latText by remember(initial) { mutableStateOf(initial?.latitude?.toString().orEmpty()) }
    var lonText by remember(initial) { mutableStateOf(initial?.longitude?.toString().orEmpty()) }

    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val location = getLastKnownLocation(context)
            if (location != null) {
                latText = location.latitude.toString()
                lonText = location.longitude.toString()
                val resolved = reverseGeocode(context, location.latitude, location.longitude)
                if (resolved != null) {
                    address = resolved
                }
                Toast.makeText(context, "GPS pozice byla načtena", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "GPS pozici se nepodařilo načíst", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bez povolení k poloze nejde načíst GPS", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Název spotu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Poznámka") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Adresa (volitelné)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val latitude = latText.toDoubleOrNull()
                        val longitude = lonText.toDoubleOrNull()
                        onSave(title.trim(), note.ifBlank { null }, latitude, longitude, address.ifBlank { null })
                    }
                },
            ) {
                Text("Uložit")
            }
            OutlinedButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        val location = getLastKnownLocation(context)
                        if (location != null) {
                            latText = location.latitude.toString()
                            lonText = location.longitude.toString()
                            val resolved = reverseGeocode(context, location.latitude, location.longitude)
                            if (resolved != null) {
                                address = resolved
                            }
                            Toast.makeText(context, "GPS pozice byla načtena", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "GPS pozici se nepodařilo načíst", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
            ) {
                Text("Načíst aktuální GPS")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Zrušit")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotDetailScreen(
    spot: SpotEntity,
    tripTitle: String?,
    modifier: Modifier = Modifier,
    photos: List<SpotPhotoEntity>,
    onAddPhoto: () -> Unit,
    onEdit: (SpotEntity) -> Unit,
    onDelete: (SpotEntity) -> Unit,
    onDeletePhoto: (SpotPhotoEntity) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<SpotPhotoEntity?>(null) }
    var photoToDelete by remember { mutableStateOf<SpotPhotoEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(spot.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(spot.note.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            val coords = if (spot.latitude != null && spot.longitude != null) {
                String.format("%.4f, %.4f", spot.latitude, spot.longitude)
            } else {
                "GPS zatím není vyplněna"
            }
            Text(coords)
        }
        spot.address?.takeIf { it.isNotBlank() }?.let { addr ->
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(addr)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            val timeText = spot.visitedAt?.let { millis ->
                val date = java.util.Date(millis)
            android.text.format.DateFormat.format("HH:mm", date).toString()
            } ?: "Bez času"
            Text(timeText)
        }
        Spacer(Modifier.height(8.dp))
        Text("Fotky (${photos.size})", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(onClick = onAddPhoto) {
                Text("Přidat fotku")
            }
            OutlinedButton(onClick = {
                val firstPhoto = photos.firstOrNull()
                if (firstPhoto == null) {
                    Toast.makeText(context, "Nejprve přidejte fotku ke spotu", Toast.LENGTH_SHORT).show()
                } else {
                    shareSpotPostcard(context, spot, tripTitle, firstPhoto)
                }
            }) {
                Text("Postcard")
            }
        }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos) { photo ->
                    val bitmap = remember(photo.id, photo.thumbnail) {
                        BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.size)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = photo.caption,
                            modifier = Modifier
                                .size(80.dp)
                                .clickable { selectedPhoto = photo },
                        )
                    }
                }
            }
        }
        val fullPhoto = selectedPhoto
        if (fullPhoto != null) {
            Dialog(
                onDismissRequest = { selectedPhoto = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Foto spotu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val bigBitmap = remember(fullPhoto.id, fullPhoto.originalUri, fullPhoto.thumbnail) {
                                loadPhotoForDisplay(context, fullPhoto)
                            }
                            if (bigBitmap != null) {
                                Image(
                                    bitmap = bigBitmap,
                                    contentDescription = fullPhoto.caption,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextButton(onClick = { selectedPhoto = null }) {
                                    Text("Zavřít")
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { photoToDelete = fullPhoto }) {
                                    Text("Smazat fotku")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Správa spotu", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(onClick = { onEdit(spot) }) {
                Text("Upravit")
            }
            OutlinedButton(onClick = { showDeleteDialog = true }) {
                Text("Smazat")
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Smazat spot") },
                text = { Text("Opravdu chcete smazat tento spot?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(spot)
                        showDeleteDialog = false
                    }) {
                        Text("Smazat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Zrušit")
                    }
                }
            )
        }
        val deletePhoto = photoToDelete
        if (deletePhoto != null) {
            AlertDialog(
                onDismissRequest = { photoToDelete = null },
                title = { Text("Smazat fotku") },
                text = { Text("Opravdu chcete smazat tuto fotku?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeletePhoto(deletePhoto)
                        photoToDelete = null
                        if (selectedPhoto?.id == deletePhoto.id) {
                            selectedPhoto = null
                        }
                    }) {
                        Text("Smazat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { photoToDelete = null }) {
                        Text("Zrušit")
                    }
                },
            )
        }
    }
}

private fun loadThumbnailBytes(
    context: Context,
    uri: Uri,
    maxSize: Int = 512,
): ByteArray? {
    val resolver = context.contentResolver
    val input = resolver.openInputStream(uri) ?: return null
    input.use { stream ->
        val original = BitmapFactory.decodeStream(stream) ?: return null
        val (width, height) = original.width to original.height
        val scale = if (width > height) maxSize.toFloat() / width else maxSize.toFloat() / height
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        val resized: Bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        } else {
            original
        }
        val output = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, output)
        if (resized !== original) {
            resized.recycle()
        }
        return output.toByteArray()
    }
}

private fun loadPhotoForDisplay(
    context: Context,
    photo: SpotPhotoEntity,
    maxSize: Int = 2048,
): androidx.compose.ui.graphics.ImageBitmap? {
    val uriString = photo.originalUri
    return if (uriString != null) {
        try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            val input = resolver.openInputStream(uri) ?: return null
            input.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val (width, height) = original.width to original.height
                val scale = if (width > height) maxSize.toFloat() / width else maxSize.toFloat() / height
                val targetWidth = (width * scale).toInt().coerceAtLeast(1)
                val targetHeight = (height * scale).toInt().coerceAtLeast(1)
                val resized: Bitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                } else {
                    original
                }
                val imageBitmap = resized.asImageBitmap()
                if (resized !== original) {
                    resized.recycle()
                }
                imageBitmap
            }
        } catch (_: Exception) {
            // Fallback to thumbnail below
            BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.size)?.asImageBitmap()
        }
    } else {
        BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.size)?.asImageBitmap()
    }
}

private fun getLastKnownLocation(context: Context): android.location.Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

    val provider = when {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    } ?: return null

    return try {
        locationManager.getLastKnownLocation(provider)
    } catch (_: SecurityException) {
        null
    }
}

@Composable
private fun TripListScreen(
    trips: List<TripEntity>,
    modifier: Modifier = Modifier,
    onCreateDemo: () -> Unit,
    onTripClick: (TripEntity) -> Unit,
    onEditTrip: (TripEntity) -> Unit,
    onDeleteTrip: (TripEntity) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    var tripToDelete by remember { mutableStateOf<TripEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Moje tripy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Výlety, místa, fotky a poznámky v jednom roadbooku.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onExportBackup) {
                        Text("Exportovat zálohu")
                    }
                    TextButton(onClick = onImportBackup) {
                        Text("Importovat zálohu")
                    }
                }
            }
            if (trips.isEmpty()) {
                item {
                    Button(onClick = onCreateDemo, modifier = Modifier.fillMaxWidth()) {
                        Text("Vytvořit ukázkový trip")
                    }
                }
            }
            items(trips) { trip ->
                ElevatedCard(onClick = { onTripClick(trip) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(trip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(trip.description.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Sync: ${trip.syncStatus}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onEditTrip(trip) }) {
                                Text("Upravit")
                            }
                            TextButton(onClick = { tripToDelete = trip }) {
                                Text("Smazat")
                            }
                        }
                    }
                }
            }
        }

        val trip = tripToDelete
        if (trip != null) {
            AlertDialog(
                onDismissRequest = { tripToDelete = null },
                title = { Text("Smazat trip") },
                text = { Text("Opravdu chcete smazat tento trip?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteTrip(trip)
                        tripToDelete = null
                    }) {
                        Text("Smazat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tripToDelete = null }) {
                        Text("Zrušit")
                    }
                }
            )
        }
    }
}

@Composable
private fun TripTimelineScreen(
    spots: List<TripViewModel.SpotWithPhotos>,
    modifier: Modifier = Modifier,
    onAddSpot: () -> Unit,
    onSpotClick: (SpotEntity) -> Unit,
    onEditSpot: (SpotEntity) -> Unit,
    onDeleteSpot: (SpotEntity) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Roadbook", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Trip je složený ze spotů v pořadí cesty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onAddSpot) {
                    Text("Přidat spot")
                }
            }
        }
        itemsIndexed(spots) { index, item ->
            SpotCard(
                spot = item.spot,
                orderNumber = index + 1,
                photoCount = item.photoCount,
                onClick = onSpotClick,
                onEdit = onEditSpot,
                onDelete = onDeleteSpot,
            )
        }
    }
}

@Composable
private fun SpotCard(
    spot: SpotEntity,
    orderNumber: Int,
    photoCount: Int,
    onClick: (SpotEntity) -> Unit,
    onEdit: (SpotEntity) -> Unit,
    onDelete: (SpotEntity) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { onClick(spot) },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            AssistChip(onClick = {}, label = { Text(orderNumber.toString()) })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        spot.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Další akce")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Upravit") },
                            onClick = {
                                menuExpanded = false
                                onEdit(spot)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Smazat") },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(spot.note.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sync: ${spot.syncStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        val coords = if (spot.latitude != null && spot.longitude != null) {
                            String.format("%.4f, %.4f", spot.latitude, spot.longitude)
                        } else {
                            "Bez GPS"
                        }
                        Text(coords)
                    }
                    spot.address?.takeIf { it.isNotBlank() }?.let { addr ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(addr)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        val timeText = spot.visitedAt?.let { millis ->
                            val date = java.util.Date(millis)
                            android.text.format.DateFormat.format("HH:mm", date).toString()
                        } ?: "Bez času"
                        Text(timeText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("$photoCount fotek")
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Poznámka")
                        }
                    }
                }
            }
        }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat spot") },
            text = { Text("Opravdu chcete smazat tento spot?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(spot)
                    showDeleteDialog = false
                }) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Zrušit")
                }
            },
        )
    }
}

private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = geocoder.getFromLocation(latitude, longitude, 1)
        val address = results?.firstOrNull() ?: return null
        address.getAddressLine(0)
    } catch (_: Exception) {
        null
    }
}

private fun shareSpotPostcard(
    context: Context,
    spot: SpotEntity,
    tripTitle: String?,
    photo: SpotPhotoEntity,
) {
    val uriString = photo.originalUri
    val baseBitmap: Bitmap? = try {
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val input = context.contentResolver.openInputStream(uri)
            input.use { stream ->
                if (stream != null) {
                    BitmapFactory.decodeStream(stream)
                } else {
                    null
                }
            }
        } else {
            BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.size)
        }
    } catch (_: Exception) {
        BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.size)
    }

    if (baseBitmap == null) {
        Toast.makeText(context, "Nelze načíst fotku pro postcard", Toast.LENGTH_SHORT).show()
        return
    }

    val width = 1080
    val height = 1920
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val bgPaint = Paint().apply {
        color = Color.parseColor("#E5E7EB")
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val cardMargin = 64f
    val cardRect = RectF(
        cardMargin,
        cardMargin,
        width.toFloat() - cardMargin,
        height.toFloat() - cardMargin,
    )
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5F5")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)
    canvas.drawRoundRect(cardRect, 40f, 40f, framePaint)

    val imageAreaHeight = (cardRect.height() * 0.55f).toInt()
    val scale = minOf(
        (cardRect.width() - 80f) / baseBitmap.width.toFloat(),
        imageAreaHeight.toFloat() / baseBitmap.height.toFloat(),
    )
    val scaledW = (baseBitmap.width * scale).toInt().coerceAtLeast(1)
    val scaledH = (baseBitmap.height * scale).toInt().coerceAtLeast(1)
    val left = cardRect.left + (cardRect.width() - scaledW) / 2f
    val top = cardRect.top + 48f
    val scaledBitmap = Bitmap.createScaledBitmap(baseBitmap, scaledW, scaledH, true)
    canvas.drawBitmap(scaledBitmap, left, top, null)

    val textTop = top + scaledH + 64f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 52f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4B5563")
        textSize = 38f
    }

    var y = textTop
    val marginX = cardRect.left + 48f

    val fullTitle = buildString {
        if (!tripTitle.isNullOrBlank()) {
            append(tripTitle.trim())
            append(" – ")
        }
        append(spot.title.trim())
    }
    canvas.drawText(fullTitle, marginX, y, titlePaint)
    y += 72f

    spot.address?.takeIf { it.isNotBlank() }?.let { addr ->
        canvas.drawText(addr.trim(), marginX, y, bodyPaint)
        y += 56f
    }

    if (spot.latitude != null && spot.longitude != null) {
        val gpsLine = "GPS: " + String.format("%.5f, %.5f", spot.latitude, spot.longitude)
        canvas.drawText(gpsLine, marginX, y, bodyPaint)
        y += 56f
    }

    spot.note?.takeIf { it.isNotBlank() }?.let { note ->
        val maxChars = 60
        val trimmed = note.trim()
        val lines = trimmed.chunked(maxChars)
        lines.take(3).forEach { line ->
            canvas.drawText(line, marginX, y, bodyPaint)
            y += 50f
        }
    }

    y += 40f
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        textSize = 36f
    }
    canvas.drawText("#TripSpot #Roadbook", marginX, y, tagPaint)

    val resolver = context.contentResolver
    val fileName = "tripspot_postcard_" + System.currentTimeMillis() + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TripSpot")
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                result.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            Toast.makeText(context, "Postcard byla uložena do galerie (Pictures/TripSpot)", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Nepodařilo se uložit postcard", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Nepodařilo se vytvořit soubor pro postcard", Toast.LENGTH_SHORT).show()
    }

    if (scaledBitmap !== baseBitmap) {
        scaledBitmap.recycle()
    }
}

@Composable
private fun QuickCreateSpotDialog(
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Krátká poznámka z cesty.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový spot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název spotu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Poznámka (volitelné)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onConfirm(title.trim(), note.ifBlank { null })
                }
            }) {
                Text("Vytvořit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        },
    )
}
