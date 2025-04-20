package dev.is_a.jakakordez.mapsforge.heatmap

import dev.is_a.jakakordez.mapsforge.heatmap.HeatmapRenderer.Grid
import org.mapsforge.core.model.Tile
import kotlinx.serialization.Serializable
import org.mapsforge.core.model.BoundingBox

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
    fun fillGrid(grid: Grid) {
        fillRecursive(rootLayer, grid)
    }

    private fun fillRecursive(layer: HeatmapNode, grid: Grid) {
        if (grid.topLeft.zoomLevel > layer.tile.zoomLevel) {
            for (child in layer.children) {
                fillRecursive(child, grid)
            }
        }
        else {
            val zoom = layer.tile.zoomLevel
            val max = Tile.getMaxTileNumber(zoom) + 1
            val x = (max + layer.tile.tileX - grid.topLeft.tileX) % max
            val y = layer.tile.tileY - grid.topLeft.tileY
            if (x in 0 ..< grid.map.size && y in 0 ..< grid.map[0].size) {
                grid.map[x][y] += layer.count
            }
        }
    }

    fun getBoundingBox(zoomLevel: Byte): BoundingBox {
        return boundingBoxRecursive(rootLayer, zoomLevel)
    }

    private fun boundingBoxRecursive(layer: HeatmapNode, zoomLevel: Byte): BoundingBox {
        if (zoomLevel > layer.tile.zoomLevel) {
            return layer.children
                .map { boundingBoxRecursive(it, zoomLevel) }
                .reduce { bb, next -> bb.extendBoundingBox(next) }
        }
        else {
            return layer.tile.boundingBox
        }
    }
}