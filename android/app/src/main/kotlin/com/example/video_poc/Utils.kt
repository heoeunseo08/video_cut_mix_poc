package com.example.video_poc

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi

object Utils {

    fun getSampleFlags(extractor: MediaExtractor): Int =
        if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
            MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getBufferSize(extractor: MediaExtractor): Int {
        var bufferSize = 0
        for (i in 0 until extractor.trackCount) {
            val size = extractor.getTrackFormat(i)
                .getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
            if (size > bufferSize) bufferSize = size
        }
        return bufferSize
    }

    fun getRotation(path: String): Int{
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rotation = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )?.toInt() ?: 0
        retriever.release()
        return rotation
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getDuration(path: String): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val duration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toInt() ?: 0
        retriever.release()
        return duration
    }
}