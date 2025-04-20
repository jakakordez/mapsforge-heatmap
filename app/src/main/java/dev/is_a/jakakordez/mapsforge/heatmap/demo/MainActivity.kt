package dev.is_a.jakakordez.mapsforge.heatmap.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.ComponentActivity
import dev.is_a.jakakordez.mapsforge.heatmap.Heatmap
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapBuilder
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapRenderer
import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapTileLayer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var map: MapView;
    private lateinit var textBlurRadius: TextView
    private lateinit var seekBlurRadius: SeekBar

    private lateinit var heatmapCache: TileCache
    private val renderOptions = HeatmapRenderer.Options()

    private var points = 0
    private lateinit var heatmapLayer: HeatmapTileLayer
    private val heatmaps = mutableMapOf<Int, Heatmap>()

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        initializeControls()

        map = findViewById(R.id.mapView)

        initializeMap()

        heatmaps.put(1, buildHeatmap())

        heatmapCache = AndroidUtil.createTileCache(baseContext,
            "heatmapCache",
            map.model.displayModel.tileSize,
            1f,
            map.model.frameBufferModel.overdrawFactor)

        heatmapLayer = HeatmapTileLayer(
            heatmapCache,
            MultiMapDataStore(),
            map.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE,
            heatmaps.values,
            renderOptions)

        map.layerManager.layers.add(heatmapLayer)

        val json = Json.encodeToString(heatmaps)
        val cbor = Cbor.encodeToByteArray(heatmaps)
        Log.i("Heatmap", "Heatmap serialized: JSON ${json.length} bytes, CBOR: ${cbor.size} bytes")

        initializeControls()
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
                points++
            }
        }
        val result = builder.build()
        val bb = result.getBoundingBox(map.model.mapViewPosition.zoomLevel)
        Log.i("Heatmap", "Generated heatmap with bounding box $bb")
        return result
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

    @SuppressLint("SetTextI18n")
    private fun initializeControls() {
        val txtStats: TextView = findViewById(R.id.txtStats)
        txtStats.text = "Heatmaps: ${heatmaps.size}  Points: $points"

        findViewById<Button>(R.id.btnGenerateHeatmap).setOnClickListener {
            heatmaps.put(heatmaps.size, buildHeatmap())
            heatmapLayer.heatmapChanged()
            txtStats.text = "Heatmaps: ${heatmaps.size}  Points: $points"
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            heatmaps.clear()
            points = 0
            heatmapLayer.heatmapChanged()
            txtStats.text = "Heatmaps: ${heatmaps.size}  Points: $points"
        }
        textBlurRadius = findViewById(R.id.textBlurRadius)
        seekBlurRadius = findViewById(R.id.seekBlurRadius)
        seekBlurRadius.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textBlurRadius.text = "Level resolution: ${progress + 1}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    renderOptions.levelResolution = (it.progress + 1).toByte()
                    heatmapLayer.heatmapChanged()
                }
            }
        })
    }
}