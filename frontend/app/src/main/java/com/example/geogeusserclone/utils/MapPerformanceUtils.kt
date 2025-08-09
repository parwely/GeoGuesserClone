package com.example.geogeusserclone.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.views.MapView
import java.util.concurrent.Executors

/**
 * Performance-optimierte Map Utilities
 */
object MapPerformanceUtils {

    /**
     * Konfiguriert OSMDroid für optimale Performance
     */
    fun configureMapPerformance(context: Context) {
        Configuration.getInstance().apply {
            // Thread Pool Optimierung
            val coreCount = Runtime.getRuntime().availableProcessors()
            setTileFileSystemThreads(minOf(coreCount, 4).toShort())
            setTileDownloadThreads(minOf(coreCount * 2, 8).toShort())

            // Cache Optimierung basierend auf verfügbarem Speicher
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val cacheSize = (maxMemory / 8).coerceAtMost(100 * 1024 * 1024L) // Max 100MB

            setTileFileSystemCacheMaxBytes(cacheSize)
            setTileFileSystemCacheTrimBytes((cacheSize * 0.8).toLong())

            // Performance Settings
            setMapViewHardwareAccelerated(true)
            setTileFileSystemMaxQueueSize(40)
            setTileDownloadMaxQueueSize(20)

            userAgentValue = "GeoGuesserClone/1.0"
        }
    }

    /**
     * Erstellt eine performance-optimierte Tile Source mit Load Balancing
     */
    fun createOptimizedTileSource(): OnlineTileSourceBase {
        val urls = arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        )

        return object : OnlineTileSourceBase(
            "PerformanceMapnik",
            1, 19, 256, ".png",
            urls
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)

                // Load Balancing über mehrere Server
                val serverIndex = ((x + y) % urls.size)
                val baseUrl = urls[serverIndex]

                return "$baseUrl$zoom/$x/$y.png"
            }
        }
    }

    /**
     * Memory-efficient Marker Icon Creation
     */
    suspend fun createOptimizedMarkerIcon(
        context: Context,
        resourceId: Int,
        size: Int = 64
    ): BitmapDrawable? = withContext(Dispatchers.IO) {
        try {
            val drawable = ContextCompat.getDrawable(context, resourceId)
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                BitmapDrawable(context.resources, bitmap)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Optimiert MapView Settings für bessere Performance
     */
    fun optimizeMapView(mapView: MapView) {
        mapView.apply {
            // Hardware Acceleration
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // Disable unnecessary features
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // Optimize tile scaling
            isTilesScaledToDpi = true

            // Set reasonable zoom limits
            minZoomLevel = 1.0
            maxZoomLevel = 19.0

            // Optimize overlay rendering
            overlayManager.tilesOverlay.setLoadingBackgroundColor(
                android.graphics.Color.TRANSPARENT
            )
        }
    }

    /**
     * Cleanup Map Resources - Verbessert für Memory-sicherheit
     */
    fun cleanupMapResources(mapView: MapView?) {
        mapView?.let { map ->
            try {
                // Stoppe alle Background-Threads vor Cleanup
                map.onPause()

                // Cleanup Overlays
                map.overlays.clear()

                // Cleanup Tile Provider
                map.tileProvider?.let { provider ->
                    try {
                        provider.clearTileCache()
                        provider.detach()
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }

                // Final detach
                map.onDetach()

            } catch (e: Exception) {
                // Silent cleanup - verhindert Crashes
            }
        }
    }

    /**
     * Pre-cache tiles für bessere Performance
     */
    suspend fun precacheTiles(
        mapView: MapView,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double = 100.0
    ) = withContext(Dispatchers.IO) {
        try {
            val boundingBox = calculateBoundingBox(centerLat, centerLng, radiusKm)
            // Pre-cache logic würde hier implementiert werden
            // Dies ist eine vereinfachte Version
        } catch (e: Exception) {
            // Silent fail - App funktioniert weiter ohne Precaching
        }
    }

    private fun calculateBoundingBox(
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double
    ): org.osmdroid.util.BoundingBox {
        val latOffset = radiusKm / 111.0 // Ungefähr 111km pro Grad
        val lngOffset = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))

        return org.osmdroid.util.BoundingBox(
            centerLat + latOffset,
            centerLng + lngOffset,
            centerLat - latOffset,
            centerLng - lngOffset
        )
    }
}

/**
 * Composable für performance-bewusstes Map Setup
 */
@Composable
fun rememberOptimizedMapConfiguration(): org.osmdroid.config.IConfigurationProvider {
    val context = LocalContext.current

    return remember {
        Configuration.getInstance().also {
            MapPerformanceUtils.configureMapPerformance(context)
        }
    }
}

/**
 * Performance-optimierte Map State Management
 */
@Composable
fun rememberMapState(
    initialZoom: Double = 3.0,
    initialCenter: org.osmdroid.util.GeoPoint = org.osmdroid.util.GeoPoint(20.0, 0.0)
): MapState {
    var zoom by remember { mutableDoubleStateOf(initialZoom) }
    var center by remember { mutableStateOf(initialCenter) }
    var isLoading by remember { mutableStateOf(false) }

    return remember {
        MapState(
            zoom = zoom,
            center = center,
            isLoading = isLoading,
            setZoom = { zoom = it },
            setCenter = { center = it },
            setLoading = { isLoading = it }
        )
    }
}

data class MapState(
    val zoom: Double,
    val center: org.osmdroid.util.GeoPoint,
    val isLoading: Boolean,
    val setZoom: (Double) -> Unit,
    val setCenter: (org.osmdroid.util.GeoPoint) -> Unit,
    val setLoading: (Boolean) -> Unit
)
