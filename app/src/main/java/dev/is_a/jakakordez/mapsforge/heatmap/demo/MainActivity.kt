package dev.is_a.jakakordez.mapsforge.heatmap.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.is_a.jakakordez.mapsforge.heatmap.Heatmap
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapBuilder
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapRenderer
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapTileLayer
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var map: MapView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        map = findViewById(R.id.mapView)

        initializeMap()

        val heatmap = buildHeatmap()

        val heatmapCache = AndroidUtil.createTileCache(baseContext,
            "heatmapCache",
            map.model.displayModel.tileSize,
            1f,
            map.model.frameBufferModel.overdrawFactor)

        val heatmapLayer = HeatmapTileLayer(
            heatmapCache,
            MultiMapDataStore(),
            map.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE,
            heatmap,
            HeatmapRenderer.Options())

        map.layerManager.layers.add(heatmapLayer)
    }

    /**
     * Generate a random heatmap with 5 clusters, each containing 30 points
     */
    private fun buildHeatmap(): Heatmap {
        val builder = HeatmapBuilder(HeatmapBuilder.Options())
        for (cluster in 0 .. 5) {
            val clusterLat = Random.nextDouble(-80.0, 80.0)
            val clusterLon = Random.nextDouble(-170.0, 170.0)
            
            for (point in 0 .. 30) {
                val lat = clusterLat + Random.nextDouble(-10.0, 10.0)
                val lon = clusterLon + Random.nextDouble(-10.0, 10.0)
                builder.feed(LatLong(lat, lon))
            }
        }
        return builder.build()
    }

    /**
     * Initialize a simple map of the world using map file and render theme from assets
     */
    private fun initializeMap() {
        AndroidGraphicFactory.createInstance(baseContext)
        map.isClickable = true
        map.mapScaleBar.isVisible = false
        map.setBuiltInZoomControls(false)

        val mapStream = baseContext.assets.open("world.map")
        val cachedMap = File(baseContext.filesDir, "world.map")
        FileOutputStream(cachedMap).use { mapStream.copyTo(it) }

        val themeStream = baseContext.assets.open("render_theme.xml")
        val cachedTheme = File(baseContext.filesDir, "render_theme.xml")
        FileOutputStream(cachedTheme).use { themeStream.copyTo(it) }

        val mapCache = AndroidUtil.createTileCache(baseContext,
            "mapcache",
            map.model.displayModel.tileSize,
            1f,
            map.model.frameBufferModel.overdrawFactor)
        val mapStore: MapDataStore = MapFile(cachedMap)
        val mapLayer = TileRendererLayer(mapCache, mapStore, map.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE)
        mapLayer.setXmlRenderTheme(ExternalRenderTheme(cachedTheme))

        map.layerManager.layers.add(mapLayer)
        map.setCenter(LatLong(0.0, 0.0))
        map.setZoomLevel(2)
        map.setZoomLevelMin(2)
        map.setZoomLevelMax(12)
    }
}