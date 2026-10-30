package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class CinematicFilterType(val titleAr: String, val titleEn: String) {
    CINEMATIC("سينمائي", "Cinematic"),
    VINTAGE("عتيق", "Vintage"),
    FILM("فيلم كلاسيكي", "Film"),
    WARM("دافئ", "Warm"),
    COLD("بارد", "Cold"),
    NOIR("أبيض وأسود داكن", "Noir"),
    DRAMATIC("درامي عالي التباين", "Dramatic"),
    RETRO("ريترو ثمانينات", "Retro"),
    PORTRAIT("بورتريه ناعم", "Portrait"),
    SUNSET("غروب الشمس الذهبي", "Sunset")
}

object FilterEngine {

    fun getColorMatrix(filter: CinematicFilterType, intensity: Float): ColorMatrix {
        val normalizedIntensity = intensity.coerceIn(0f, 1f)
        val matrix = when (filter) {
            CinematicFilterType.CINEMATIC -> {
                // Teal & Orange Hollywood look
                floatArrayOf(
                    1.2f, 0.0f, 0.0f, 0.0f, 20f,
                    0.0f, 1.05f, 0.0f, 0.0f, 10f,
                    0.0f, 0.0f, 1.3f, 0.0f, 30f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.VINTAGE -> {
                // Sepia aged tone
                floatArrayOf(
                    0.9f, 0.2f, 0.1f, 0.0f, 35f,
                    0.1f, 0.8f, 0.1f, 0.0f, 25f,
                    0.1f, 0.1f, 0.6f, 0.0f, -10f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.FILM -> {
                // Matte faded blacks with rich contrast
                floatArrayOf(
                    1.15f, 0.0f, 0.0f, 0.0f, 15f,
                    0.0f, 1.15f, 0.0f, 0.0f, 15f,
                    0.0f, 0.0f, 1.1f, 0.0f, 20f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.WARM -> {
                // Boost red & green slightly, drop blue
                floatArrayOf(
                    1.2f, 0.0f, 0.0f, 0.0f, 25f,
                    0.0f, 1.05f, 0.0f, 0.0f, 10f,
                    0.0f, 0.0f, 0.85f, 0.0f, -15f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.COLD -> {
                // Boost blue, lower red
                floatArrayOf(
                    0.85f, 0.0f, 0.0f, 0.0f, -15f,
                    0.0f, 0.95f, 0.0f, 0.0f, 5f,
                    0.0f, 0.0f, 1.3f, 0.0f, 35f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.NOIR -> {
                // Deep blacks and strong highlights (Film Noir)
                floatArrayOf(
                    0.35f, 0.65f, 0.15f, 0.0f, -20f,
                    0.35f, 0.65f, 0.15f, 0.0f, -20f,
                    0.35f, 0.65f, 0.15f, 0.0f, -20f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.DRAMATIC -> {
                // S-curve contrast boost
                floatArrayOf(
                    1.4f, 0.0f, 0.0f, 0.0f, -25f,
                    0.0f, 1.4f, 0.0f, 0.0f, -25f,
                    0.0f, 0.0f, 1.4f, 0.0f, -25f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.RETRO -> {
                // 80s synthwave magenta/cyan wash
                floatArrayOf(
                    1.25f, 0.0f, 0.2f, 0.0f, 20f,
                    0.0f, 0.9f, 0.0f, 0.0f, -10f,
                    0.2f, 0.0f, 1.35f, 0.0f, 30f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.PORTRAIT -> {
                // Soft highlights, balanced skin tones
                floatArrayOf(
                    1.08f, 0.02f, 0.0f, 0.0f, 12f,
                    0.0f, 1.02f, 0.0f, 0.0f, 8f,
                    0.0f, 0.0f, 0.96f, 0.0f, 4f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
            CinematicFilterType.SUNSET -> {
                // Golden hour glow (intense amber / orange / gold)
                floatArrayOf(
                    1.35f, 0.1f, 0.0f, 0.0f, 35f,
                    0.0f, 1.15f, 0.0f, 0.0f, 18f,
                    0.0f, 0.0f, 0.7f, 0.0f, -30f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0f
                )
            }
        }

        // Interpolate with identity matrix according to intensity
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val blended = FloatArray(20)
        for (i in 0 until 20) {
            blended[i] = identity[i] + (matrix[i] - identity[i]) * normalizedIntensity
        }
        return ColorMatrix(blended)
    }

    suspend fun applyFilter(
        source: Bitmap,
        filter: CinematicFilterType,
        intensity: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = getColorMatrix(filter, intensity)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        result
    }

    suspend fun saveFilteredImage(
        context: Context,
        filteredBitmap: Bitmap,
        originalName: String
    ): File = withContext(Dispatchers.IO) {
        val outDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Cinematic")
        if (!outDir.exists()) outDir.mkdirs()
        val file = File(outDir, "Filtered_${System.currentTimeMillis()}_$originalName")
        FileOutputStream(file).use { out ->
            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        file
    }
}
