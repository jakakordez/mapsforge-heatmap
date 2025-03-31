package dev.is_a.jakakordez.mapsforge.heatmap

import android.graphics.Bitmap
import org.mapsforge.core.model.Tile
import kotlin.math.pow
import kotlinx.serialization.Serializable

/**
 * Heatmap class represents a single heatmap with all layers representing different levels of
 * detail. The levels included in a heatmap depend on the settings used when building it and
 * distribution of included coordinate points.
 *
 * The heatmap is structured as a 2d tree with each
 * node covering exactly one Mapsforge tile, identified by a org.mapsforge.core.model.Tile object.
 * The root of the tree is the largest node (tile with lowest zoom level) which covers all
 * contained coordinate points. Children of a node for tile X are nodes which belong to the tiles
 * that are children of the tile X (their zoom level = zoom level of X + 1). The lowest layer of
 * nodes is defined when heatmap is built using HeatmapBuilder.
 */
@Serializable
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
        if (!areTilesRelated(tile, layer.tile)) {
            return
        }
        if (tile.zoomLevel + levelResolution > layer.tile.zoomLevel) {
            for (child in layer.children) {
                fillRecursive(child, tile, bmp, calculateColor, offsetX, offsetY)
            }
        }
        else {
            val x = layer.tile.tileX - (tile.tileX * tileChildren)
            val y = layer.tile.tileY - (tile.tileY * tileChildren)
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