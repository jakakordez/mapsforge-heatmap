package dev.is_a.jakakordez.mapsforge.heatmap

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.layer.renderer.RendererJob

class HeatmapRenderer(
    mapDataStore: MapDataStore?,
    graphicFactory: GraphicFactory?,
    tileCache: TileCache?,
    private val heatmap: Heatmap,
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
         * Radius of the blur filter used when rendering the heatmap
         */
        val blurRadius: Float = 16f,
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
        maskFilter = BlurMaskFilter(options.blurRadius, BlurMaskFilter.Blur.NORMAL)
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
        val grid = heatmap.get9Grid()

        if (options.zeroColor != Color.TRANSPARENT) {
            grid.eraseColor(options.zeroColor)
        }

        heatmap.generateBitmap(grid, calculateColor, 0, 0, tile.aboveLeft)
        heatmap.generateBitmap(grid, calculateColor, 1, 0, tile.above)
        heatmap.generateBitmap(grid, calculateColor, 2, 0, tile.aboveRight)
        heatmap.generateBitmap(grid, calculateColor, 0, 1, tile.left)
        heatmap.generateBitmap(grid, calculateColor, 1, 1, tile)
        heatmap.generateBitmap(grid, calculateColor, 2, 1, tile.right)
        heatmap.generateBitmap(grid, calculateColor, 0, 2, tile.belowLeft)
        heatmap.generateBitmap(grid, calculateColor, 1, 2, tile.below)
        heatmap.generateBitmap(grid, calculateColor, 2, 2, tile.belowRight)

        val blurredGrid = Bitmap.createBitmap(
            tile.tileSize * 3,
            tile.tileSize * 3,
            Bitmap.Config.ARGB_8888)
        val blurredCanvas = Canvas(blurredGrid)
        blurredCanvas.drawBitmap(grid,
            Rect(0, 0, grid.width, grid.height),
            Rect(0, 0, blurredGrid.width, blurredGrid.height),
            blurPaint)

        val tileGrid = Bitmap.createBitmap(tile.tileSize, tile.tileSize, Bitmap.Config.ARGB_8888)
        val tileCanvas = Canvas(tileGrid)
        tileCanvas.drawBitmap(blurredGrid,
            Rect(tile.tileSize, tile.tileSize, tile.tileSize * 2, tile.tileSize * 2),
            Rect(0, 0, tile.tileSize, tile.tileSize),
            normalPaint)

        val output = HeatmapBitmap(tileGrid)

        if (tileCache != null) {
            tileCache.put(job, output)
        }

        return output
    }
}