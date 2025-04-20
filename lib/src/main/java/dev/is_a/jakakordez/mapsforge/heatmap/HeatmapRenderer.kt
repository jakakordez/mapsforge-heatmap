package dev.is_a.jakakordez.mapsforge.heatmap

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
    private val heatmaps: Collection<Heatmap>,
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
        /**
         * Tile heatmap resolution in zoom level values.
         *
         * Value of 3 for example means that each map tile will contain 8 x 8 points of the heatmap
         * (2^3 = 8).
         */
        var levelResolution: Byte = 3,
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

        val grid = Grid(tile, options.levelResolution)
        heatmaps.forEach {
            it.fillGrid(grid)
        }
        val gridBitmap = gridToBitmap(grid)
        val blurredGrid = blurGrid(gridBitmap, tileSize, grid)

        val tileGrid = createBitmap(tileSize, tileSize)
        val tileCanvas = Canvas(tileGrid)

        val topOffset = if (grid.top) 0 else tileSize

        tileCanvas.drawBitmap(blurredGrid,
            Rect(tileSize, topOffset, tileSize * 2, tileSize + topOffset),
            Rect(0, 0, tileSize, tileSize),
            normalPaint)

        val output = HeatmapBitmap(tileGrid)

        if (tileCache != null) {
            tileCache.put(job, output)
        }

        return output
    }

    private fun gridToBitmap(grid: Grid): Bitmap {
        val gridBitmap = createBitmap(
            grid.tileChildren * grid.width,
            grid.tileChildren * grid.height)

        if (options.zeroColor != Color.TRANSPARENT) {
            gridBitmap.eraseColor(options.zeroColor)
        }

        for (x in 0 ..< grid.tileChildren * grid.width) {
            for (y in 0 ..< grid.tileChildren * grid.height) {
                gridBitmap[x, y] = calculateColor(grid.map[x][y])
            }
        }
        return gridBitmap
    }

    private fun blurGrid(gridBitmap: Bitmap, tileSize: Int, grid: Grid): Bitmap {
        val blurredGrid = createBitmap(tileSize * grid.width, tileSize * grid.height)
        val blurredCanvas = Canvas(blurredGrid)
        blurredCanvas.drawBitmap(gridBitmap,
            Rect(0, 0, gridBitmap.width, gridBitmap.height),
            Rect(0, 0, blurredGrid.width, blurredGrid.height),
            blurPaint)
        return blurredGrid
    }

    class Grid(val tile: Tile, val levelResolution: Byte)
    {
        val tileChildren = 2f.pow(levelResolution.toInt()).toInt()

        val top = tile.tileY == 0
        val bottom = tile.tileY == Tile.getMaxTileNumber(tile.zoomLevel)

        val height = if (top || bottom) 2 else 3
        val width = 3

        val map = Array(tileChildren * width) { Array(tileChildren * height) { 0L } }

        val topLeftMap = if (top) tile.left else tile.aboveLeft
        val topLeft = Tile(
            topLeftMap.tileX * tileChildren,
            topLeftMap.tileY * tileChildren,
            (topLeftMap.zoomLevel + levelResolution).toByte(),
            topLeftMap.tileSize)

        val bottomRightMap = if (bottom) tile.right else tile.belowRight
        val bottomRight = Tile(
            ((bottomRightMap.tileX + 1) * tileChildren) - 1,
            ((bottomRightMap.tileY + 1) * tileChildren) - 1,
            (bottomRightMap.zoomLevel + levelResolution).toByte(),
            bottomRightMap.tileSize)
    }
}