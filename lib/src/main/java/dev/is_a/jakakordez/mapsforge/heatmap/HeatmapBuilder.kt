package dev.is_a.jakakordez.mapsforge.heatmap

import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import java.util.stream.Stream

class HeatmapBuilder(private val options: Options) {

    data class Options(
        val levelResolution: Byte = 2,
        val maxMapZoom: Byte = 12,
        val minMapZoom: Byte = 0
    )
    {
        val maxHeatmapZoom: Byte = (maxMapZoom + levelResolution).toByte()
        val minHeatmapZoom: Byte = (minMapZoom + levelResolution).toByte()
    }

    private val map = HashMap<Tile, Long>()

    fun feed(location: LatLong) {
        feed(Stream.of(location))
    }

    fun feed(locations: Collection<LatLong>) {
        feed(locations.stream())
    }

    fun feed(locations: Stream<LatLong>) {
        for (location in locations) {
            val tile = getTile(location, options.maxHeatmapZoom)
            map[tile] = map.getOrDefault(tile, 0) + 1
        }
    }

    fun build(): Heatmap {
        val topNode = buildPyramid(map)
        return Heatmap(topNode, options.levelResolution)
    }

    private fun getTile(latLong: LatLong, zoomLevel: Byte): Tile {
        val x = MercatorProjection.longitudeToTileX(latLong.longitude, zoomLevel)
        val y = MercatorProjection.latitudeToTileY(latLong.latitude, zoomLevel)
        return Tile(x, y, zoomLevel, 1)
    }

    private fun buildPyramid(bottomLayer: HashMap<Tile, Long>) : HeatmapNode {
        var nodes = bottomLayer.map { HeatmapNode(it.value, it.key, listOf()) }
        while(nodes.size > 1 || nodes.first().tileId.zoomLevel > options.minHeatmapZoom) {
            nodes = buildLayer(nodes)
        }
        return nodes.first()
    }

    private fun buildLayer(layer: List<HeatmapNode>): List<HeatmapNode>
    {
        val upperLayer = HashMap<Tile, MutableList<HeatmapNode>>()
        for (heatmapNode in layer) {
            val parentId = heatmapNode.tileId.parent
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