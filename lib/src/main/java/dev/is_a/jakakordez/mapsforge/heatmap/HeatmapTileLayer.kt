package dev.is_a.jakakordez.mapsforge.heatmap

import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.TileLayer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.MapWorkerPool
import org.mapsforge.map.layer.renderer.RendererJob
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.model.MapViewPosition
import org.mapsforge.map.model.common.Observer
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture

class HeatmapTileLayer(
    tileCache: TileCache, private val mapDataStore: MapDataStore, mapViewPosition: MapViewPosition?,
    private val graphicFactory: GraphicFactory, heatmaps: Set<Heatmap>,
    options: HeatmapRenderer.Options
) :
    TileLayer<RendererJob>(
        tileCache,
        mapViewPosition,
        graphicFactory.createMatrix(),
        true
    ),
    Observer {
    private val heatmapRenderer: HeatmapRenderer = HeatmapRenderer(
        this.mapDataStore,
        graphicFactory,
        tileCache,
        heatmaps,
        options
    )
    private var mapWorkerPool: MapWorkerPool? = null
    private var themeFuture: RenderThemeFuture? = null

    override fun onDestroy() {
        mapDataStore.close()
        super.onDestroy()
    }

    @Synchronized
    override fun setDisplayModel(displayModel: DisplayModel?) {
        super.setDisplayModel(displayModel)
        themeFuture = RenderThemeFuture(graphicFactory, null, displayModel)
        if (displayModel != null) {
            if (mapWorkerPool == null) {
                mapWorkerPool = MapWorkerPool(
                    this.tileCache,
                    this.jobQueue,
                    this.heatmapRenderer,
                    this
                )
            }
            mapWorkerPool?.start()
        } else {
            // if we do not have a displayModel any more we can stop rendering.
            mapWorkerPool?.stop()
        }
    }

    override fun createJob(tile: Tile): RendererJob {
        return RendererJob(
            tile, this.mapDataStore, themeFuture, this.displayModel, 1f,
            this.isTransparent, false
        )
    }

    override fun isTileStale(tile: Tile, bitmap: TileBitmap): Boolean {
        return mapDataStore.getDataTimestamp(tile) > bitmap.timestamp
    }

    override fun onAdd() {
        mapWorkerPool?.start()
        tileCache?.addObserver(this)

        super.onAdd()
    }

    override fun onRemove() {
        mapWorkerPool?.stop()
        tileCache?.removeObserver(this)
        super.onRemove()
    }

    override fun onChange() {
        this.requestRedraw()
    }
}