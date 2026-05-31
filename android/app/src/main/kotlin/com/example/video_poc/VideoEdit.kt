package com.example.video_poc

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer

object VideoEdit {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun cut(inPutPath: String, outputPath: String, startMs: Long, endMs: Long) {

        //미디어 조회
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inPutPath)

        val totalDuration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong() ?: 0L

        retriever.release()

        require(startMs >= 0 && endMs <= totalDuration && startMs < endMs) {
            "error : startMs= $startMs, endMs= $endMs, totalDuration= $totalDuration"
        }

        // 미디어 분리
        val extractor = MediaExtractor()
        extractor.setDataSource(inPutPath)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = HashMap<Int, Int>()
        val videoTrack = mutableListOf<Int>()

        var bufferSize = 0

        //
        for (i in 0 until extractor.trackCount) {
            val format: MediaFormat = extractor.getTrackFormat(i)
            val mine = format.getString(MediaFormat.KEY_MIME) ?: continue

            if (mine.startsWith("video/") || mine.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackMap[i] = muxer.addTrack(format)
                if (mine.startsWith("video/")) videoTrack.add(i)
            }

            val trackBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
            if (trackBufferSize > bufferSize) {
                bufferSize = trackBufferSize
            }
        }
        extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        muxer.start()

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, 0)

            if (bufferInfo.size < 0) break

            val sampleTimeUs = extractor.sampleTime
            val trackIndex = extractor.sampleTrackIndex

            if (trackIndex in videoTrack && sampleTimeUs > endMs * 1000L) break
            if (!trackMap.containsKey(trackIndex)) {
                extractor.advance()
                continue
            }

            bufferInfo.offset = 0
            bufferInfo.presentationTimeUs = sampleTimeUs - (startMs * 1000L)
            bufferInfo.flags = when {
                extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0 -> MediaCodec.BUFFER_FLAG_KEY_FRAME
                else -> 0
            }

            muxer.writeSampleData(trackMap[trackIndex]!!, buffer, bufferInfo)
            extractor.advance()
        }
        muxer.stop()
        muxer.release()
        extractor.release()
    }
}