package dev.is_a.jakakordez.mapsforge.heatmap

import org.mapsforge.core.model.Tile

data class HeatmapNode(
    val count: Long,
    val tileId: Tile,
    val children: List<HeatmapNode>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HeatmapNode

        if (count != other.count) return false
        if (tileId != other.tileId) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count.hashCode()
        result = 31 * result + tileId.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}