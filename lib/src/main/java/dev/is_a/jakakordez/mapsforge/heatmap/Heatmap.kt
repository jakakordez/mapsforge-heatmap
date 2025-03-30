package dev.is_a.jakakordez.mapsforge.heatmap

import android.graphics.Bitmap
import org.mapsforge.core.model.Tile
import kotlin.math.pow

class Heatmap(private val rootLayer: HeatmapNode, private val levelResolution: Byte) {
    private val tileChildren = 2f.pow(levelResolution.toInt()).toInt()

    fun get9Grid(): Bitmap {
        return Bitmap.createBitmap(
            tileChildren * 3,
            tileChildren * 3,
            Bitmap.Config.ARGB_8888)
    }

    fun generateBitmap(bmp: Bitmap, calculateColor: (Long) -> Int, offsetX: Int,
                       offsetY: Int, tile: Tile) {
        fillRecursive(
            rootLayer,
            tile,
            bmp,
            calculateColor,
            offsetX * tileChildren,
            offsetY * tileChildren)
    }

    private fun fillRecursive(layer: HeatmapNode, tile: Tile, bmp: Bitmap,
                              calculateColor: (Long) -> Int, offsetX: Int, offsetY: Int) {
        if (!areTilesRelated(tile, layer.tileId)) {
            return
        }
        if (tile.zoomLevel + levelResolution > layer.tileId.zoomLevel) {
            for (child in layer.children) {
                fillRecursive(child, tile, bmp, calculateColor, offsetX, offsetY)
            }
        }
        else {
            val x = layer.tileId.tileX - (tile.tileX * tileChildren)
            val y = layer.tileId.tileY - (tile.tileY * tileChildren)
            if (x in 0 ..< tileChildren && y in 0 ..< tileChildren) {
                bmp.setPixel(x + offsetX, y + offsetY, calculateColor(layer.count))
            }
        }
    }

    private fun areTilesRelated(tile1: Tile, tile2: Tile): Boolean {
        if (tile1.zoomLevel == tile2.zoomLevel) {
            return tile1.tileX == tile2.tileX
                    && tile1.tileY == tile2.tileY
        }
        else if (tile1.zoomLevel > tile2.zoomLevel) {
            val power = 2f.pow(tile1.zoomLevel - tile2.zoomLevel).toInt()
            return tile1.tileX / power == tile2.tileX
                    && tile1.tileY / power == tile2.tileY
        }
        else {
            val power = 2f.pow(tile2.zoomLevel - tile1.zoomLevel).toInt()
            return tile1.tileX == tile2.tileX / power
                    && tile1.tileY == tile2.tileY / power
        }
    }
}