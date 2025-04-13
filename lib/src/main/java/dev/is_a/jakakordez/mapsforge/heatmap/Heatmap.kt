package dev.is_a.jakakordez.mapsforge.heatmap

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
class Heatmap(private val rootLayer: HeatmapNode) {

    fun fillGrid(grid: Array<Array<Long>>, topLeft: Tile) {
        fillRecursive(rootLayer, topLeft, grid)
    }

    private fun fillRecursive(layer: HeatmapNode, topLeft: Tile, grid: Array<Array<Long>>) {
        /*if (!areTilesRelated(tile, layer.tile)) {
            return
        }*/
        if (topLeft.zoomLevel > layer.tile.zoomLevel) {
            for (child in layer.children) {
                fillRecursive(child, topLeft, grid)
            }
        }
        else {
            val zoom = layer.tile.zoomLevel
            val max = Tile.getMaxTileNumber(zoom) + 1
            val x = (max + layer.tile.tileX - topLeft.tileX) % max
            val y = layer.tile.tileY - topLeft.tileY
            if (x in 0 ..< grid.size && y in 0 ..< grid[0].size) {
                grid[x][y] += layer.count
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