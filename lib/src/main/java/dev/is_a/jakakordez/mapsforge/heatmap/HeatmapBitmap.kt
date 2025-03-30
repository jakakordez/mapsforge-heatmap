package dev.is_a.jakakordez.mapsforge.heatmap

import android.graphics.Bitmap
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.map.android.graphics.AndroidBitmap

class HeatmapBitmap(bitmap: Bitmap?) : AndroidBitmap(bitmap), TileBitmap{


    override fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }

    override fun isExpired(): Boolean {
        return false
    }

    override fun setExpiration(expiration: Long) {

    }

    override fun setTimestamp(timestamp: Long) {

    }
}