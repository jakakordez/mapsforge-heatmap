package dev.is_a.jakakordez.mapsforge.heatmap

import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import java.util.stream.Stream

/**
 * Builder class used to build a Heatmap object by feeding it coordinate points. Typical steps to
 * build a heatmap is:
 * - initialize the builder, configuring it using the Options object passed in constructor
 * - add the coordinate points by using one of feed() functions
 * - build the heatmap using build() function
 *
 * Feed calls are fast as they are just adding the points to a hash map. The heatmap is built
 * inside the build() function from the lowest layer to the highest. For more information about the
 * data structure refer to the documentation of the Heatmap class.
 */
class HeatmapBuilder(private val options: Options) {

    data class Options(
        /**
         * Tile heatmap resolution in zoom level values.
         *
         * Value of 3 for example means that each map tile will contain 8 x 8 points of the heatmap
         * (2^3 = 8).
         */
        val levelResolution: Byte = 3,
        /**
         * Maximal map zoom which is be accessible by the user
         */
        val maxMapZoom: Byte = 12,
        /**
         * Minimal map zoom which is be accessible by the user
         */
        val minMapZoom: Byte = 0
    )
    {
        val maxHeatmapZoom: Byte = (maxMapZoom + levelResolution).toByte()
        val minHeatmapZoom: Byte = (minMapZoom + levelResolution).toByte()
    }

    private val map = HashMap<Tile, Long>()

    /**
     * Add a single coordinate point to the heatmap
     * @param location coordinate point
     */
    fun feed(location: LatLong) {
        feed(Stream.of(location))
    }

    /**
     * Add a collection of coordinate points to the heatmap
     * @param locations collection of coordinate points
     */
    fun feed(locations: Collection<LatLong>) {
        feed(locations.stream())
    }

    /**
     * Add a stream of coordinate points to the heatmap
     * @param locations stream of coordinate points
     */
    fun feed(locations: Stream<LatLong>) {
        for (location in locations) {
            val tile = getTile(location, options.maxHeatmapZoom)
            map[tile] = map.getOrDefault(tile, 0) + 1
        }
    }

    /**
     * Builds a heatmap from the points provided until now. This method doesn't destroy the builder
     * internal state so it can be called multiple times in order to generate intermediary heatmaps.
     * @return built heatmap
     */
    fun build(): Heatmap {
        val topNode = buildPyramid(map)
        return Heatmap(topNode)
    }

    private fun getTile(latLong: LatLong, zoomLevel: Byte): Tile {
        val x = MercatorProjection.longitudeToTileX(latLong.longitude, zoomLevel)
        val y = MercatorProjection.latitudeToTileY(latLong.latitude, zoomLevel)
        return Tile(x, y, zoomLevel, 1)
    }

    private fun buildPyramid(bottomLayer: HashMap<Tile, Long>) : HeatmapNode {
        var nodes = bottomLayer.map { HeatmapNode(it.value, it.key, listOf()) }
        while(nodes.size > 1 || nodes.first().tile.zoomLevel > options.minHeatmapZoom) {
            nodes = buildLayer(nodes)
        }
        return nodes.first()
    }

    private fun buildLayer(layer: List<HeatmapNode>): List<HeatmapNode>
    {
        val upperLayer = HashMap<Tile, MutableList<HeatmapNode>>()
        for (heatmapNode in layer) {
            val parentId = heatmapNode.tile.parent
            val list = upperLayer.getOrDefault(parentId, mutableListOf())
            list.add(heatmapNode)
            upperLayer[parentId] = list
        }

        return upperLayer.map { node ->
            val totalCount = node.value.sumOf { child -> child.count }
            HeatmapNode(totalCount, node.key, node.value)
        }
    }
}