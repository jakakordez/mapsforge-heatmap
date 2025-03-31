package dev.is_a.jakakordez.mapsforge.heatmap

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.mapsforge.core.model.Tile

@Serializable
data class HeatmapNode(
    val count: Long,
    @Serializable(with = TileSerializer::class)
    val tile: Tile,
    val children: List<HeatmapNode>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HeatmapNode

        if (count != other.count) return false
        if (tile != other.tile) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count.hashCode()
        result = 31 * result + tile.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}

@Serializable
@SerialName("Tile")
private class TileSurrogate(val x: Int, val y: Int, val zoomLevel: Byte)

class TileSerializer : KSerializer<Tile> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "org.mapsforge.core.model.Tile",
        TileSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: Tile) {
        val surrogate = TileSurrogate(value.tileX, value.tileY, value.zoomLevel)
        encoder.encodeSerializableValue(TileSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Tile {
        val surrogate = decoder.decodeSerializableValue(TileSurrogate.serializer())
        return Tile(surrogate.x, surrogate.y, surrogate.zoomLevel, 1)
    }
}