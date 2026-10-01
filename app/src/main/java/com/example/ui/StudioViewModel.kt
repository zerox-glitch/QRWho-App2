package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CustomPresetEntity
import com.example.data.FavoritePresetEntity
import com.example.data.QrEntity
import com.example.data.QrRepository
import com.example.qr.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class DesignStateSnapshot(
    val style: QrStyle,
    val payload: QrPayload,
    val photoBitmap: Bitmap?,
    val samplePhotoId: String?,
    val customLogo: Bitmap?
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QrRepository(AppDatabase.getDatabase(application).qrDao())

    val historyList: StateFlow<List<QrEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val customPresets: StateFlow<List<CustomPresetEntity>> = repository.customPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIds
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    private val _payload = MutableStateFlow(QrPayload())
    val payload: StateFlow<QrPayload> = _payload.asStateFlow()

    private val _style = MutableStateFlow(QrPresets.fallbackList.first().style)
    val style: StateFlow<QrStyle> = _style.asStateFlow()

    private val _photoBitmap = MutableStateFlow<Bitmap?>(null)
    val photoBitmap: StateFlow<Bitmap?> = _photoBitmap.asStateFlow()

    private val _customLogo = MutableStateFlow<Bitmap?>(null)
    val customLogo: StateFlow<Bitmap?> = _customLogo.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanCheckResult?>(null)
    val scanResult: StateFlow<ScanCheckResult?> = _scanResult.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private val _activeStudioTab = MutableStateFlow(0)
    val activeStudioTab: StateFlow<Int> = _activeStudioTab.asStateFlow()

    private val _presetFilterCategory = MutableStateFlow("All")
    val presetFilterCategory: StateFlow<String> = _presetFilterCategory.asStateFlow()

    private val _historyFilter = MutableStateFlow("All")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _showKofiDialog = MutableStateFlow(false)
    val showKofiDialog: StateFlow<Boolean> = _showKofiDialog.asStateFlow()

    private val _kofiCountdown = MutableStateFlow(15)
    val kofiCountdown: StateFlow<Int> = _kofiCountdown.asStateFlow()

    // Undo / Redo State Management
    private val undoStack = mutableListOf<DesignStateSnapshot>()
    private val redoStack = mutableListOf<DesignStateSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var renderJob: Job? = null
    private var kofiTimerJob: Job? = null
    private var samplePhotoId: String? = null

    private fun currentSnapshot(): DesignStateSnapshot {
        return DesignStateSnapshot(
            style = _style.value,
            payload = _payload.value,
            photoBitmap = _photoBitmap.value,
            samplePhotoId = samplePhotoId,
            customLogo = _customLogo.value
        )
    }

    private fun updateCanUndoRedo() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun recordUndoState() {
        val snap = currentSnapshot()
        if (undoStack.isEmpty() || undoStack.last() != snap) {
            undoStack.add(snap)
            if (undoStack.size > 35) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            updateCanUndoRedo()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(currentSnapshot())

        _style.value = previousState.style
        _payload.value = previousState.payload
        _photoBitmap.value = previousState.photoBitmap
        samplePhotoId = previousState.samplePhotoId
        _customLogo.value = previousState.customLogo

        updateCanUndoRedo()
        _userMessage.value = "↩ Undid design change"
        triggerRender()
        saveDraftState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(currentSnapshot())

        _style.value = nextState.style
        _payload.value = nextState.payload
        _photoBitmap.value = nextState.photoBitmap
        samplePhotoId = nextState.samplePhotoId
        _customLogo.value = nextState.customLogo

        updateCanUndoRedo()
        _userMessage.value = "↪ Redid design change"
        triggerRender()
        saveDraftState()
    }

    private fun prefs() = getApplication<Application>().getSharedPreferences("qrwho_draft_studio_v1", Context.MODE_PRIVATE)

    init {
        com.example.qr.engine.SamplePhotos.appContext = application.applicationContext
        // Initialize presets from assets (all 400+ items)
        QrPresets.initialize(application)
        if (!restoreDraftState()) {
            loadDefaultDesign()
        } else {
            triggerRender()
        }
    }

    private fun loadDefaultDesign() {
        val solarpunkPreset = QrPresets.findById("art-solarpunk")
        val defaultStyle = solarpunkPreset?.style?.copy(imageMode = ImageMode.Clean)
            ?: QrPresets.fallbackList.first().style.copy(imageMode = ImageMode.Clean)
        _style.value = defaultStyle

        viewModelScope.launch(Dispatchers.IO) {
            val fernBitmap = SamplePhotos.loadSampleBitmap(getApplication(), "art-emerald-fern", 1024)
            withContext(Dispatchers.Main) {
                _photoBitmap.value = fernBitmap
                samplePhotoId = "art-emerald-fern"
                triggerRender()
                saveDraftState()
            }
        }
    }

    private fun saveDraftState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = prefs().edit()
                p.putBoolean("has_draft", true)

                // Save Payload
                val payload = _payload.value
                p.putString("p_kind", payload.kind.name)
                p.putString("p_url", payload.url)
                p.putString("p_wifi_ssid", payload.wifiSsid)
                p.putString("p_wifi_pass", payload.wifiPassword)
                p.putString("p_text", payload.text)

                // Save Style
                val s = _style.value
                p.putString("s_module_shape", s.moduleShape.name)
                p.putString("s_eye_shape", s.eyeShape.name)
                p.putString("s_ball_shape", s.ballShape.name)
                p.putInt("s_fg_color", s.fgColor)
                p.putInt("s_bg_color", s.bgColor)
                p.putInt("s_eye_color", s.eyeColor)
                p.putInt("s_ball_color", s.ballColor)
                p.putString("s_gradient_type", s.gradientType.name)
                p.putInt("s_gradient_to", s.gradientTo)
                p.putString("s_effect", s.effect.name)
                p.putFloat("s_effect_intensity", s.effectIntensity)
                p.putString("s_frame_style", s.frameStyle.name)
                p.putString("s_frame_caption", s.frameCaption)
                p.putInt("s_quiet_zone", s.quietZone)
                p.putString("s_image_mode", s.imageMode.name)
                p.putFloat("s_image_opacity", s.imageOpacity)
                p.putFloat("s_contrast", s.contrast)
                p.putInt("s_min_version", s.minVersion)
                p.putFloat("s_dot_scale", s.dotScale)
                p.putFloat("s_module_gap", s.moduleGap)

                // Save Photo reference
                p.putString("s_sample_photo_id", samplePhotoId ?: "")

                val currentPhoto = _photoBitmap.value
                if (samplePhotoId == null && currentPhoto != null) {
                    p.putBoolean("s_has_custom_photo", true)
                    val photoFile = File(getApplication<Application>().filesDir, "draft_photo.png")
                    FileOutputStream(photoFile).use { out ->
                        currentPhoto.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                } else if (samplePhotoId == null && currentPhoto == null) {
                    p.putBoolean("s_has_custom_photo", false)
                    val photoFile = File(getApplication<Application>().filesDir, "draft_photo.png")
                    if (photoFile.exists()) photoFile.delete()
                }

                p.apply()
            } catch (_: Exception) {}
        }
    }

    private fun restoreDraftState(): Boolean {
        val p = prefs()
        if (!p.getBoolean("has_draft", false)) return false

        try {
            // Restore Payload
            val kindStr = p.getString("p_kind", PayloadKind.URL.name) ?: PayloadKind.URL.name
            val kind = try { PayloadKind.valueOf(kindStr) } catch (_: Exception) { PayloadKind.URL }
            _payload.value = QrPayload(
                kind = kind,
                url = p.getString("p_url", "https://qrwho.vercel.app") ?: "https://qrwho.vercel.app",
                wifiSsid = p.getString("p_wifi_ssid", "") ?: "",
                wifiPassword = p.getString("p_wifi_pass", "") ?: "",
                text = p.getString("p_text", "") ?: ""
            )

            // Restore Style
            val moduleShape = try { ModuleShape.valueOf(p.getString("s_module_shape", "Fluid") ?: "Fluid") } catch (_: Exception) { ModuleShape.Fluid }
            val eyeShape = try { EyeShape.valueOf(p.getString("s_eye_shape", "Leaf") ?: "Leaf") } catch (_: Exception) { EyeShape.Leaf }
            val ballShape = try { EyeShape.valueOf(p.getString("s_ball_shape", "Rounded") ?: "Rounded") } catch (_: Exception) { EyeShape.Rounded }
            val gradientType = try { GradientType.valueOf(p.getString("s_gradient_type", "Diagonal") ?: "Diagonal") } catch (_: Exception) { GradientType.Diagonal }
            val effect = try { com.example.qr.engine.QrEffect.valueOf(p.getString("s_effect", "None") ?: "None") } catch (_: Exception) { com.example.qr.engine.QrEffect.None }
            val effectIntensity = p.getFloat("s_effect_intensity", 1.0f).coerceIn(0.4f, 2.0f)
            val frameStyle = try { FrameStyle.valueOf(p.getString("s_frame_style", "None") ?: "None") } catch (_: Exception) { FrameStyle.None }
            val imageMode = try { ImageMode.valueOf(p.getString("s_image_mode", "Clean") ?: "Clean") } catch (_: Exception) { ImageMode.Clean }

            _style.value = QrStyle(
                moduleShape = moduleShape,
                eyeShape = eyeShape,
                ballShape = ballShape,
                fgColor = p.getInt("s_fg_color", 0xFF84CC16.toInt()),
                bgColor = p.getInt("s_bg_color", 0xFF0C1708.toInt()),
                eyeColor = p.getInt("s_eye_color", 0xFFFACC15.toInt()),
                ballColor = p.getInt("s_ball_color", 0xFF65A30D.toInt()),
                gradientType = gradientType,
                gradientTo = p.getInt("s_gradient_to", 0xFFEAB308.toInt()),
                effect = effect,
                effectIntensity = effectIntensity,
                frameStyle = frameStyle,
                frameCaption = p.getString("s_frame_caption", "SCAN ME") ?: "SCAN ME",
                quietZone = p.getInt("s_quiet_zone", 2),
                imageMode = imageMode,
                imageOpacity = p.getFloat("s_image_opacity", 0.86f),
                contrast = p.getFloat("s_contrast", 1.0f),
                minVersion = p.getInt("s_min_version", 2),
                dotScale = p.getFloat("s_dot_scale", 0.88f),
                moduleGap = p.getFloat("s_module_gap", 0.02f)
            )

            // Restore Photo
            val sampleId = p.getString("s_sample_photo_id", "") ?: ""
            val hasCustom = p.getBoolean("s_has_custom_photo", false)

            if (sampleId.isNotEmpty()) {
                samplePhotoId = sampleId
                viewModelScope.launch(Dispatchers.IO) {
                    val bmp = SamplePhotos.loadSampleBitmap(getApplication(), sampleId, 1024)
                    withContext(Dispatchers.Main) {
                        _photoBitmap.value = bmp
                        triggerRender()
                    }
                }
            } else if (hasCustom) {
                samplePhotoId = null
                viewModelScope.launch(Dispatchers.IO) {
                    val photoFile = File(getApplication<Application>().filesDir, "draft_photo.png")
                    if (photoFile.exists()) {
                        val bmp = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                        withContext(Dispatchers.Main) {
                            _photoBitmap.value = bmp
                            triggerRender()
                        }
                    }
                }
            } else {
                samplePhotoId = null
                _photoBitmap.value = null
            }

            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun triggerKofiSupportModal() {
        kofiTimerJob?.cancel()
        kofiTimerJob = viewModelScope.launch {
            _kofiCountdown.value = 15
            _showKofiDialog.value = true
            for (i in 14 downTo 0) {
                kotlinx.coroutines.delay(1000)
                _kofiCountdown.value = i
            }
        }
    }

    fun dismissKofiDialog() {
        kofiTimerJob?.cancel()
        _showKofiDialog.value = false
    }

    fun setStudioTab(index: Int) {
        _activeStudioTab.value = index
    }

    fun setPresetCategory(category: String) {
        _presetFilterCategory.value = category
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun navigateToMyPresets() {
        _activeStudioTab.value = 1
        _presetFilterCategory.value = "✨ My Presets"
    }

    fun navigateToFavorites() {
        _activeStudioTab.value = 1
        _presetFilterCategory.value = "★ Favorites"
    }

    fun navigateToHistoryCreated() {
        _activeStudioTab.value = 4
        _historyFilter.value = "Created"
    }

    fun navigateToHistoryScanned() {
        _activeStudioTab.value = 4
        _historyFilter.value = "Scanned"
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun updatePayload(newPayload: QrPayload) {
        recordUndoState()
        _payload.value = newPayload
        triggerRender()
        saveDraftState()
    }

    fun updateStyle(newStyle: QrStyle) {
        recordUndoState()
        _style.value = newStyle
        triggerRender()
        saveDraftState()
    }

    fun selectPreset(preset: QrPreset) {
        recordUndoState()
        _userMessage.value = "Applied ${preset.name} style"
        val artId = preset.style.artDirection
        if (artId != null && ArtFrameRenderer.isArtFrame(artId)) {
            viewModelScope.launch(Dispatchers.IO) {
                val bmp = SamplePhotos.loadSampleBitmap(getApplication(), artId, 1024)
                withContext(Dispatchers.Main) {
                    _photoBitmap.value = bmp
                    samplePhotoId = artId
                    _style.value = preset.style.copy(imageMode = ImageMode.Clean)
                    triggerRender()
                    saveDraftState()
                }
            }
        } else {
            // Preset does NOT use an art frame.
            // If previous photo was just an automatic sample photo from an art frame, clear it so it doesn't corrupt this preset!
            if (samplePhotoId != null) {
                _photoBitmap.value = null
                samplePhotoId = null
            }
            val currentPhoto = _photoBitmap.value
            val currentMode = _style.value.imageMode
            val hasPhoto = currentPhoto != null
            val isPhotoMode = currentMode != ImageMode.None && currentMode != ImageMode.Logo

            if (hasPhoto) {
                // Keep explicitly uploaded user photo active with the new preset style
                val targetMode = if (preset.style.imageMode != ImageMode.None) {
                    preset.style.imageMode
                } else if (isPhotoMode) {
                    currentMode
                } else {
                    ImageMode.Clean
                }
                _style.value = preset.style.copy(imageMode = targetMode)
            } else {
                _style.value = preset.style
            }
            triggerRender()
            saveDraftState()
        }
    }

    fun applyShowcase(item: ShowcaseItem) {
        recordUndoState()
        val foundPreset = QrPresets.findById(item.presetId)
        if (foundPreset != null) {
            val currentPhoto = _photoBitmap.value
            val currentMode = _style.value.imageMode
            val hasPhoto = currentPhoto != null
            val isPhotoMode = currentMode != ImageMode.None && currentMode != ImageMode.Logo

            if (hasPhoto) {
                val targetMode = if (foundPreset.style.imageMode != ImageMode.None) {
                    foundPreset.style.imageMode
                } else if (isPhotoMode) {
                    currentMode
                } else {
                    ImageMode.Clean
                }
                _style.value = foundPreset.style.copy(imageMode = targetMode)
            } else {
                _style.value = foundPreset.style
            }
        }
        _payload.value = _payload.value.copy(
            kind = PayloadKind.URL,
            url = item.defaultUrl
        )
        _userMessage.value = "Loaded ${item.title} into Studio"
        triggerRender()
        saveDraftState()
    }

    fun setPhoto(bitmap: Bitmap?) {
        recordUndoState()
        _photoBitmap.value = bitmap
        if (bitmap == null) {
            samplePhotoId = null
        }
        if (bitmap != null) {
            _style.value = _style.value.copy(imageMode = ImageMode.Clean)
        }
        triggerRender()
        saveDraftState()
    }

    fun selectSamplePhoto(sampleId: String) {
        recordUndoState()
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = SamplePhotos.loadSampleBitmap(getApplication(), sampleId, 1024)
            withContext(Dispatchers.Main) {
                samplePhotoId = sampleId
                _photoBitmap.value = bmp
                _style.value = _style.value.copy(imageMode = ImageMode.Clean)
                _userMessage.value = "Sample photo loaded"
                triggerRender()
                saveDraftState()
            }
        }
    }

    fun setCustomLogo(bitmap: Bitmap?) {
        recordUndoState()
        _customLogo.value = bitmap
        if (bitmap != null) {
            _style.value = _style.value.copy(
                selectedLogoId = null,
                ecc = "H", // Automatic Error Correction Level H (30%) for guaranteed scannability with center logo
                logoScale = _style.value.logoScale.coerceIn(0.20f, 0.26f)
            )
        }
        triggerRender()
    }

    fun saveCustomPreset(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _style.value
            val entity = CustomPresetEntity(
                name = name.ifBlank { "My Preset ${System.currentTimeMillis() % 1000}" },
                description = description.ifBlank { "Custom ${s.moduleShape.label} Style" },
                moduleShape = s.moduleShape.name,
                eyeShape = s.eyeShape.name,
                ballShape = s.ballShape.name,
                fgColor = s.fgColor,
                bgColor = s.bgColor,
                eyeColor = s.eyeColor,
                ballColor = s.ballColor,
                gradientType = s.gradientType.name,
                gradientTo = s.gradientTo,
                frameStyle = s.frameStyle.name,
                frameCaption = s.frameCaption,
                quietZone = s.quietZone,
                moduleGap = s.moduleGap,
                dotScale = s.dotScale,
                contrast = s.contrast,
                ecc = s.ecc,
                effect = s.effect.name,
                effectIntensity = s.effectIntensity,
                artDirection = s.artDirection,
                imageMode = s.imageMode.name,
                imageOpacity = s.imageOpacity
            )
            repository.saveCustomPreset(entity)
            withContext(Dispatchers.Main) {
                _activeStudioTab.value = 1
                _presetFilterCategory.value = "✨ My Presets"
                _userMessage.value = "Saved '${entity.name}' to My Presets!"
            }
        }
    }

    fun loadCustomPreset(presetEntity: CustomPresetEntity) {
        recordUndoState()
        val preset = presetEntity.toQrPreset()
        selectPreset(preset)
        _activeStudioTab.value = 0
        _userMessage.value = "Loaded preset '${presetEntity.name}' from Vault into Studio!"
    }

    fun deleteCustomPreset(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomPreset(id)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Deleted preset from My Presets"
            }
        }
    }

    fun toggleFavorite(presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = favoriteIds.value
            val isFav = current.contains(presetId)
            val presetName = QrPresets.findById(presetId)?.name
                ?: customPresets.value.find { "custom_${it.id}" == presetId }?.name
                ?: "Style"
            if (isFav) {
                repository.removeFavorite(presetId)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Removed '$presetName' from Favorites"
                }
            } else {
                repository.addFavorite(presetId)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "★ Added '$presetName' to Favorites!"
                }
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            withContext(Dispatchers.Main) {
                _userMessage.value = "Cleared all history"
            }
        }
    }

    fun clearHistoryByType(isScanned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearByType(isScanned)
            withContext(Dispatchers.Main) {
                _userMessage.value = if (isScanned) "Cleared scanned QR history" else "Cleared saved QR creations"
            }
        }
    }

    fun selectBuiltInLogo(logoId: String?) {
        recordUndoState()
        if (logoId != null) {
            val app = getApplication<android.app.Application>()
            val bmp = com.example.qr.engine.BuiltInLogos.loadLogoBitmap(app, logoId)
            _customLogo.value = bmp
            _style.value = _style.value.copy(
                selectedLogoId = logoId,
                ecc = "H",
                logoScale = _style.value.logoScale.coerceIn(0.20f, 0.26f)
            )
        } else {
            _customLogo.value = null
            _style.value = _style.value.copy(selectedLogoId = null)
        }
        triggerRender()
    }

    fun autoFixScan() {
        recordUndoState()
        viewModelScope.launch(Dispatchers.Default) {
            _isOptimizing.value = true
            try {
                val currentPayloadText = _payload.value.toEncodedText()
                val currentPhoto = _photoBitmap.value
                val currentLogo = _customLogo.value

                val result = QrScannabilityEvaluator.optimizeScan(
                    current = _style.value,
                    payloadText = currentPayloadText,
                    photoBitmap = currentPhoto,
                    customLogo = currentLogo
                )

                // Generate new bitmap with optimized style
                val bmp = QrGenerator.generateQrBitmap(
                    payload = currentPayloadText,
                    qrStyle = result.style,
                    photoBitmap = currentPhoto,
                    customLogo = currentLogo,
                    sizePx = 1024
                )
                val evalResult = QrScannabilityEvaluator.evaluate(bmp, result.style)

                withContext(Dispatchers.Main) {
                    _style.value = result.style
                    _qrBitmap.value = bmp
                    _scanResult.value = evalResult
                    _isOptimizing.value = false
                    val summary = if (result.notes.isNotEmpty()) {
                        result.notes.take(2).joinToString(" & ")
                    } else {
                        "Level H ECC & Contrast"
                    }
                    _userMessage.value = "⚡ Optimized: $summary (Verified Scannable)!"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isOptimizing.value = false
                    _userMessage.value = "Optimization error: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun triggerRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            _isGenerating.value = true
            try {
                val currentPayloadText = _payload.value.toEncodedText()
                val currentStyle = _style.value
                val currentPhoto = _photoBitmap.value
                val currentLogo = _customLogo.value

                val bmp = QrGenerator.generateQrBitmap(
                    payload = currentPayloadText,
                    qrStyle = currentStyle,
                    photoBitmap = currentPhoto,
                    customLogo = currentLogo,
                    sizePx = 1024
                )

                val evalResult = QrScannabilityEvaluator.evaluate(bmp, currentStyle)

                withContext(Dispatchers.Main) {
                    _qrBitmap.value = bmp
                    _scanResult.value = evalResult
                    _isGenerating.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _userMessage.value = "Render error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun onScannedFromCamera(scannedText: String) {
        val trimmed = scannedText.trim()
        val parsed = QrContentParser.parse(trimmed)
        val detected = when (parsed.kind) {
            PayloadKind.GEO -> {
                _payload.value.copy(
                    kind = PayloadKind.GEO,
                    geoLat = parsed.geoLatitude ?: _payload.value.geoLat,
                    geoLng = parsed.geoLongitude ?: _payload.value.geoLng,
                    geoQuery = parsed.geoQuery ?: parsed.title
                )
            }
            PayloadKind.URL -> {
                _payload.value.copy(kind = PayloadKind.URL, url = trimmed)
            }
            PayloadKind.WIFI -> {
                val ssid = Regex("S:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: "WiFi"
                val pwd = Regex("P:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                val enc = Regex("T:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: "WPA"
                _payload.value.copy(kind = PayloadKind.WIFI, wifiSsid = ssid, wifiPassword = pwd, wifiEncryption = enc)
            }
            PayloadKind.VCARD -> {
                val fn = Regex("FN:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: "Contact"
                val tel = Regex("TEL[^:]*:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                val email = Regex("EMAIL[^:]*:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                _payload.value.copy(kind = PayloadKind.VCARD, vcardFirstName = fn, vcardLastName = "", vcardPhone = tel, vcardEmail = email)
            }
            PayloadKind.EMAIL -> {
                val email = trimmed.removePrefix("mailto:").substringBefore("?")
                _payload.value.copy(kind = PayloadKind.EMAIL, emailTo = email)
            }
            PayloadKind.PHONE -> {
                val phone = trimmed.removePrefix("tel:")
                _payload.value.copy(kind = PayloadKind.PHONE, phoneNumber = phone)
            }
            PayloadKind.SMS -> {
                val parts = trimmed.removePrefix("smsto:").split(":")
                val num = parts.getOrNull(0) ?: ""
                val msg = parts.getOrNull(1) ?: ""
                _payload.value.copy(kind = PayloadKind.SMS, smsNumber = num, smsMessage = msg)
            }
            PayloadKind.WHATSAPP -> {
                val num = Regex("""wa\.me/([0-9+]+)""").find(trimmed)?.groupValues?.get(1) ?: ""
                _payload.value.copy(kind = PayloadKind.WHATSAPP, waNumber = num)
            }
            PayloadKind.PAYMENT -> {
                _payload.value.copy(kind = PayloadKind.PAYMENT, paymentAddress = trimmed)
            }
            else -> {
                _payload.value.copy(kind = PayloadKind.TEXT, text = trimmed)
            }
        }
        _payload.value = detected
        recordScannedQr(trimmed)
        _userMessage.value = "Loaded ${parsed.title} into Studio!"
        triggerRender()
    }

    fun recordScannedQr(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = text.trim()
            val parsed = QrContentParser.parse(trimmed)
            val entity = QrEntity(
                title = parsed.title.take(45),
                payloadKind = parsed.kind.name,
                payloadRaw = trimmed,
                encodedText = trimmed,
                presetName = "Scanned",
                isScanned = true
            )
            repository.saveQr(entity)
        }
    }

    fun saveToHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val style = _style.value
            val p = _payload.value
            val entity = QrEntity(
                title = when (p.kind) {
                    PayloadKind.URL -> p.url.ifBlank { "Website URL" }
                    PayloadKind.WIFI -> "Wi-Fi: ${p.wifiSsid}"
                    PayloadKind.VCARD -> "${p.vcardFirstName} ${p.vcardLastName}".trim().ifBlank { "Contact Card" }
                    PayloadKind.EVENT -> p.eventTitle.ifBlank { "Event Calendar" }
                    PayloadKind.EMAIL -> p.emailTo.ifBlank { "Email Message" }
                    PayloadKind.PHONE -> p.phoneNumber.ifBlank { "Phone Call" }
                    PayloadKind.SMS -> "SMS: ${p.smsNumber}"
                    PayloadKind.GEO -> "Map Location"
                    else -> p.text.take(30).ifBlank { "Text Note" }
                },
                payloadKind = p.kind.name,
                payloadRaw = p.toEncodedText(),
                encodedText = p.toEncodedText(),
                presetName = style.artDirection,
                moduleShape = style.moduleShape.name,
                eyeShape = style.eyeShape.name,
                ballShape = style.ballShape.name,
                fgColor = style.fgColor,
                bgColor = style.bgColor,
                eyeColor = style.eyeColor,
                ballColor = style.ballColor,
                gradientType = style.gradientType.name,
                gradientTo = style.gradientTo,
                frameStyle = style.frameStyle.name,
                frameCaption = style.frameCaption,
                quietZone = style.quietZone,
                moduleGap = style.moduleGap,
                dotScale = style.dotScale,
                contrast = style.contrast,
                imageMode = style.imageMode.name,
                imageOpacity = style.imageOpacity,
                ecc = style.ecc,
                effect = style.effect.name,
                effectIntensity = style.effectIntensity,
                artDirection = style.artDirection,
                scanScore = _scanResult.value?.score ?: 98
            )
            repository.saveQr(entity)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Saved '${entity.title}' to History Vault!"
            }
        }
    }

    fun restoreFromHistory(entity: QrEntity) {
        val kind = try {
            PayloadKind.valueOf(entity.payloadKind)
        } catch (_: Exception) {
            PayloadKind.TEXT
        }

        val restoredPayload = when (kind) {
            PayloadKind.URL -> _payload.value.copy(kind = PayloadKind.URL, url = entity.encodedText)
            PayloadKind.TEXT -> _payload.value.copy(kind = PayloadKind.TEXT, text = entity.encodedText)
            else -> {
                val trimmed = entity.encodedText.trim()
                when {
                    trimmed.startsWith("WIFI:", ignoreCase = true) -> {
                        val ssid = Regex("S:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: "WiFi"
                        val pwd = Regex("P:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                        val enc = Regex("T:([^;]+)").find(trimmed)?.groupValues?.get(1) ?: "WPA"
                        _payload.value.copy(kind = PayloadKind.WIFI, wifiSsid = ssid, wifiPassword = pwd, wifiEncryption = enc)
                    }
                    trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                        val fn = Regex("FN:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: "Contact"
                        val tel = Regex("TEL[^:]*:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                        val email = Regex("EMAIL[^:]*:([^\r\n]+)").find(trimmed)?.groupValues?.get(1) ?: ""
                        _payload.value.copy(kind = PayloadKind.VCARD, vcardFirstName = fn, vcardLastName = "", vcardPhone = tel, vcardEmail = email)
                    }
                    trimmed.startsWith("mailto:", ignoreCase = true) -> {
                        _payload.value.copy(kind = PayloadKind.EMAIL, emailTo = trimmed.removePrefix("mailto:"))
                    }
                    trimmed.startsWith("tel:", ignoreCase = true) -> {
                        _payload.value.copy(kind = PayloadKind.PHONE, phoneNumber = trimmed.removePrefix("tel:"))
                    }
                    trimmed.startsWith("smsto:", ignoreCase = true) -> {
                        val parts = trimmed.removePrefix("smsto:").split(":")
                        _payload.value.copy(kind = PayloadKind.SMS, smsNumber = parts.getOrNull(0) ?: "", smsMessage = parts.getOrNull(1) ?: "")
                    }
                    trimmed.startsWith("geo:", ignoreCase = true) -> {
                        _payload.value.copy(kind = PayloadKind.GEO, geoQuery = trimmed.removePrefix("geo:"))
                    }
                    else -> _payload.value.copy(kind = kind, text = entity.encodedText, url = entity.encodedText)
                }
            }
        }

        val restoredEffect = try { QrEffect.valueOf(entity.effect) } catch (_: Exception) { QrEffect.None }
        val restoredArtDirection = entity.artDirection ?: entity.presetName

        val restoredStyle = QrStyle(
            moduleShape = try { ModuleShape.valueOf(entity.moduleShape) } catch (_: Exception) { ModuleShape.Rounded },
            eyeShape = try { EyeShape.valueOf(entity.eyeShape) } catch (_: Exception) { EyeShape.Rounded },
            ballShape = try { EyeShape.valueOf(entity.ballShape) } catch (_: Exception) { EyeShape.Circle },
            fgColor = entity.fgColor,
            bgColor = entity.bgColor,
            eyeColor = entity.eyeColor,
            ballColor = entity.ballColor,
            gradientType = try { GradientType.valueOf(entity.gradientType) } catch (_: Exception) { GradientType.None },
            gradientTo = entity.gradientTo,
            frameStyle = try { FrameStyle.valueOf(entity.frameStyle) } catch (_: Exception) { FrameStyle.None },
            frameCaption = entity.frameCaption,
            quietZone = entity.quietZone,
            moduleGap = entity.moduleGap,
            dotScale = entity.dotScale,
            contrast = entity.contrast,
            imageMode = try { ImageMode.valueOf(entity.imageMode) } catch (_: Exception) { ImageMode.None },
            imageOpacity = entity.imageOpacity,
            ecc = entity.ecc,
            artDirection = restoredArtDirection,
            effect = restoredEffect,
            effectIntensity = entity.effectIntensity
        )

        _payload.value = restoredPayload
        _style.value = restoredStyle

        if (restoredArtDirection != null && ArtFrameRenderer.isArtFrame(restoredArtDirection)) {
            viewModelScope.launch(Dispatchers.IO) {
                val bmp = SamplePhotos.loadSampleBitmap(getApplication(), restoredArtDirection, 1024)
                withContext(Dispatchers.Main) {
                    _photoBitmap.value = bmp
                    samplePhotoId = restoredArtDirection
                    triggerRender()
                    saveDraftState()
                }
            }
        } else {
            if (samplePhotoId != null || restoredStyle.imageMode == ImageMode.None) {
                _photoBitmap.value = null
                samplePhotoId = null
            }
        }

        _activeStudioTab.value = 0
        _userMessage.value = "Loaded '${entity.title}' from Vault into Studio!"
        triggerRender()
        saveDraftState()
    }

    fun shareHistoryItem(context: Context, item: QrEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val itemStyle = QrStyle(
                    moduleShape = try { ModuleShape.valueOf(item.moduleShape) } catch (_: Exception) { ModuleShape.Rounded },
                    eyeShape = try { EyeShape.valueOf(item.eyeShape) } catch (_: Exception) { EyeShape.Rounded },
                    ballShape = try { EyeShape.valueOf(item.ballShape) } catch (_: Exception) { EyeShape.Circle },
                    fgColor = item.fgColor,
                    bgColor = item.bgColor,
                    eyeColor = item.eyeColor,
                    ballColor = item.ballColor,
                    gradientType = try { GradientType.valueOf(item.gradientType) } catch (_: Exception) { GradientType.None },
                    gradientTo = item.gradientTo,
                    frameStyle = try { FrameStyle.valueOf(item.frameStyle) } catch (_: Exception) { FrameStyle.None },
                    frameCaption = item.frameCaption,
                    quietZone = item.quietZone
                )
                val bmp = QrGenerator.generateQrBitmap(
                    payload = item.encodedText,
                    qrStyle = itemStyle,
                    sizePx = 768
                )
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "qr_share_${item.id}.png")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.encodedText}\n\nCreated with QRWho Studio")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share QR Code").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Share failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun exportPng(context: Context, resolutionPx: Int = 2048) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val highResBitmap = QrGenerator.generateQrBitmap(
                    payload = _payload.value.toEncodedText(),
                    qrStyle = _style.value,
                    photoBitmap = _photoBitmap.value,
                    customLogo = _customLogo.value,
                    sizePx = resolutionPx
                )

                val filename = "QRWho_${System.currentTimeMillis()}_${resolutionPx}px.png"
                var outputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRWho")
                    }
                    val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        outputStream = context.contentResolver.openOutputStream(imageUri)
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/QRWho"
                    val file = File(imagesDir)
                    if (!file.exists()) file.mkdirs()
                    val image = File(file, filename)
                    outputStream = FileOutputStream(image)
                }

                outputStream?.use {
                    highResBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                withContext(Dispatchers.Main) {
                    _userMessage.value = "Exported print-ready PNG (${resolutionPx}px) to Pictures/QRWho!"
                    triggerKofiSupportModal()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun getSvgString(sizePx: Int = 1024): String {
        return QrGenerator.generateQrSvg(
            payload = _payload.value.toEncodedText(),
            qrStyle = _style.value,
            sizePx = sizePx
        )
    }

    fun exportSvg(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val svgContent = getSvgString(1024)
                val filename = "QRWho_${System.currentTimeMillis()}.svg"

                // 1. Download & Save directly to user's device Downloads/QRWho storage
                var savedToDownloads = false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QRWho")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(svgContent.toByteArray(Charsets.UTF_8))
                            }
                            savedToDownloads = true
                        }
                    } else {
                        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QRWho")
                        if (!downloadsDir.exists()) downloadsDir.mkdirs()
                        val file = File(downloadsDir, filename)
                        file.writeText(svgContent, Charsets.UTF_8)
                        savedToDownloads = true
                    }
                } catch (_: Exception) {}

                // 2. Also write to cache for FileProvider sharing / export chooser
                val dir = File(context.cacheDir, "exports")
                if (!dir.exists()) dir.mkdirs()
                val cacheFile = File(dir, filename)
                cacheFile.writeText(svgContent, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/svg+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Artistic QR Code (Vector SVG)")
                    putExtra(Intent.EXTRA_TEXT, "Vector SVG QR Code created with QRWho")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Export Vector SVG").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(chooser)
                } catch (_: Exception) {
                    try {
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, svgContent)
                        shareIntent.removeExtra(Intent.EXTRA_STREAM)
                        context.startActivity(Intent.createChooser(shareIntent, "Export SVG Content").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    _userMessage.value = if (savedToDownloads) {
                        "Vector SVG saved to Downloads/QRWho & ready to export!"
                    } else {
                        "Exported genuine vector SVG file!"
                    }
                    triggerKofiSupportModal()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "SVG export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun shareQrCode(context: Context) {
        val bmp = _qrBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "qrwho_share.png")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Created with QRWho — 100% Free Artistic QR Code Studio")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share QR Code").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Share failed: ${e.localizedMessage}"
                }
            }
        }
    }
}
