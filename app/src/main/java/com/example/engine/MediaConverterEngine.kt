package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

enum class AudioBitrate(val kbps: Int, val label: String) {
    STANDARD(128, "128 kbps (قياسي)"),
    HIGH(192, "192 kbps (جودة عالية)"),
    VERY_HIGH(320, "320 kbps (فائقة الوضوح)")
}

data class ConversionProgress(
    val percentage: Float,
    val currentBytes: Long,
    val totalBytes: Long,
    val isCompleted: Boolean = false,
    val outputFile: File? = null,
    val errorMessage: String? = null
)

object MediaConverterEngine {

    fun estimateOutputSize(durationSeconds: Long, bitrateKbps: Int): Long {
        // (bitrate in bits/s * duration) / 8
        return (bitrateKbps * 1000L * durationSeconds) / 8
    }

    /**
     * Extracts audio track from video file directly using Android native MediaExtractor & MediaMuxer.
     * 100% offline and blazingly fast.
     */
    suspend fun extractAudioFromVideo(
        context: Context,
        videoUri: Uri,
        outputFormat: String = "m4a", // or mp3 / aac
        bitrate: AudioBitrate = AudioBitrate.HIGH,
        onProgress: (ConversionProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        val outDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "AudioExtracted")
        if (!outDir.exists()) outDir.mkdirs()
        val outputFile = File(outDir, "Audio_${System.currentTimeMillis()}.$outputFormat")

        try {
            extractor.setDataSource(context, videoUri, null)
            val numTracks = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("لم يتم العثور على مسار صوتي داخل ملف الفيديو المحدد.")
            }

            extractor.selectTrack(audioTrackIndex)

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                10_000_000L
            }

            val muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
            val writeTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var totalBytesWritten = 0L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                totalBytesWritten += bufferInfo.size

                val currentUs = bufferInfo.presentationTimeUs
                val progress = if (durationUs > 0) (currentUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 0.99f) else 0.5f

                onProgress(
                    ConversionProgress(
                        percentage = progress,
                        currentBytes = totalBytesWritten,
                        totalBytes = estimateOutputSize(durationUs / 1_000_000L, bitrate.kbps)
                    )
                )

                extractor.advance()
            }

            onProgress(
                ConversionProgress(
                    percentage = 1f,
                    currentBytes = totalBytesWritten,
                    totalBytes = totalBytesWritten,
                    isCompleted = true,
                    outputFile = outputFile
                )
            )

            outputFile
        } catch (e: Exception) {
            onProgress(
                ConversionProgress(
                    percentage = 0f,
                    currentBytes = 0,
                    totalBytes = 0,
                    isCompleted = false,
                    errorMessage = e.localizedMessage ?: "فشل استخراج الصوت"
                )
            )
            throw e
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // ignore
            }
            extractor.release()
        }
    }
}
