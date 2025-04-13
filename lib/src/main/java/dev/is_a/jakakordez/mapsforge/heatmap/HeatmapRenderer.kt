package dev.is_a.jakakordez.mapsforge.heatmap

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.annotation.ColorInt
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.layer.renderer.RendererJob
import kotlin.math.pow
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

class HeatmapRenderer(
    mapDataStore: MapDataStore?,
    graphicFactory: GraphicFactory?,
    tileCache: TileCache?,
    private val heatmaps: Set<Heatmap>,
    private val options: Options
) : DatabaseRenderer(
    mapDataStore,
    graphicFactory,
    tileCache,
    null,
    false,
    false,
    null
) {
    data class Options(
        val levelResolution: Byte = 3,
        /**
         * Minimal number of points to determine the color of a point. Values less than minValue
         * will be painted using minColor.
         */
        val minValue: Int = 1,
        /**
         * Maximal number of points to determine the color of a point. Values more than maxValue
         * will be painted using maxColor.
         */
        val maxValue: Int = 10,
        /**
         * Color of the smallest number of points.
         */
        @ColorInt
        val minColor: Int = Color.GREEN,
        /**
         * Color of the largest number of points.
         */
        @ColorInt
        val maxColor: Int = Color.RED,
        /**
         * Color of pixels where there are no points.
         */
        @ColorInt
        val zeroColor: Int = Color.TRANSPARENT)

    private val blurPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    private val tileChildren = 2f.pow(options.levelResolution.toInt()).toInt()
    private val normalPaint = Paint()
    private val rgbEvaluator = ArgbEvaluator()

    private val calculateColor = fun (count: Long): Int {
        if (count == 0L) {
            return options.zeroColor
        }
        val span = options.maxValue - options.minValue
        val percentage = ((count - options.minValue).toFloat() / span).coerceIn(0f, 1f)
        return rgbEvaluator.evaluate(percentage, options.minColor, options.maxColor) as Int
    }

    override fun executeJob(job: RendererJob?): TileBitmap {
        val tile = job!!.tile
        val tileSize = tile.tileSize

        val height = if (tile.tileY == 0) 2 else 3

        val grid = generateGrid(tile, height)
        val gridBitmap = gridToBitmap(grid, height)
        val blurredGrid = blurGrid(gridBitmap, tileSize, height)

        val tileGrid = createBitmap(tileSize, tileSize)
        val tileCanvas = Canvas(tileGrid)
        tileCanvas.drawBitmap(blurredGrid,
            Rect(tileSize, (height - 2) * tileSize, tileSize * 2, tileSize * (height - 1)),
            Rect(0, 0, tileSize, tileSize),
            normalPaint)

        val output = HeatmapBitmap(tileGrid)

        if (tileCache != null) {
            tileCache.put(job, output)
        }

        return output
    }

    private fun generateGrid(tile: Tile, height: Int) : Array<Array<Long>> {
        val grid = Array(tileChildren * 3) { Array(tileChildren * height) { 0L } }

        val corner = if (height == 2) tile.left else tile.aboveLeft
        val cornerChild = Tile(
            corner.tileX * tileChildren,
            corner.tileY * tileChildren,
            (corner.zoomLevel + options.levelResolution).toByte(),
            corner.tileSize)

        heatmaps.forEach {
            it.fillGrid(grid, cornerChild)
        }
        return grid
    }

    private fun gridToBitmap(grid: Array<Array<Long>>, height: Int): Bitmap {
        val gridBitmap = createBitmap(tileChildren * 3, tileChildren * height)

        if (options.zeroColor != Color.TRANSPARENT) {
            gridBitmap.eraseColor(options.zeroColor)
        }

        for (x in 0 ..< tileChildren * 3) {
            for (y in 0 ..< tileChildren * height) {
                gridBitmap[x, y] = calculateColor(grid[x][y])
            }
        }
        return gridBitmap
    }

    private fun blurGrid(gridBitmap: Bitmap, tileSize: Int, height: Int): Bitmap {
        val blurredGrid = createBitmap(tileSize * 3, tileSize * height)
        val blurredCanvas = Canvas(blurredGrid)
        blurredCanvas.drawBitmap(gridBitmap,
            Rect(0, 0, gridBitmap.width, gridBitmap.height),
            Rect(0, 0, blurredGrid.width, blurredGrid.height),
            blurPaint)
        return blurredGrid
    }
}