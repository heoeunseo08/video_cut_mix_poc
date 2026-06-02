package com.example.video_poc

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.nio.ByteBuffer

object VideoEdit {

    private fun getSampleFlags(extractor: MediaExtractor): Int =
        if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
            MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getBufferSize(extractor: MediaExtractor): Int {
        var bufferSize = 0
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
            if (size > bufferSize) bufferSize = size
        }
        return bufferSize
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun cut(inPutPath: String, outputPath: String, startMs: Long, endMs: Long) {

        //미디어 조회
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inPutPath)

        val totalDuration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong() ?: 0L

        val rotation = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )?.toInt() ?: 0
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

        //
        for (i in 0 until extractor.trackCount) {
            val format: MediaFormat = extractor.getTrackFormat(i)
            val mine = format.getString(MediaFormat.KEY_MIME) ?: continue

            if (mine.startsWith("video/") || mine.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackMap[i] = muxer.addTrack(format)
                if (mine.startsWith("video/")) videoTrack.add(i)
            }
        }
        extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        if (rotation != 0) {
            muxer.setOrientationHint(rotation)
        }

        muxer.start()

        val buffer = ByteBuffer.allocate(getBufferSize(extractor))
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
            bufferInfo.flags = getSampleFlags(extractor)

            muxer.writeSampleData(trackMap[trackIndex]!!, buffer, bufferInfo)
            extractor.advance()
        }
        muxer.stop()
        muxer.release()
        extractor.release()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun mix(inPutPath: List<String>, outputPath: String) {
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val mimeToMuxerTrack = HashMap<String, Int>()
        val firstExtractor = MediaExtractor()
        firstExtractor.setDataSource(inPutPath.first())

        for (i in 0 until firstExtractor.trackCount) {
            val format = firstExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                firstExtractor.selectTrack(i)
                mimeToMuxerTrack[mime] = muxer.addTrack(format)
            }
        }

        val bufferSize = getBufferSize(firstExtractor)
        firstExtractor.release()

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inPutPath.first())
        val rotation = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )?.toInt() ?: 0

        retriever.release()

        if (rotation != 0) muxer.setOrientationHint(rotation)
        muxer.start()

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        var ptsOffsetUs = 0L

        for (path in inPutPath) {
            val extractor = MediaExtractor()
            extractor.setDataSource(path)

            val ret = MediaMetadataRetriever()
            ret.setDataSource(path)
            val durationMs = ret.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L
            ret.release()

            val mimeToExtractorTrack = HashMap<String, Int>()
            for (i in 0 until extractor.trackCount) {
                val format: MediaFormat = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    mimeToExtractorTrack[mime] = i
                }
            }
            extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC)
            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val extractorTraceIndex = extractor.sampleTrackIndex
                val sampleTimeUs = extractor.sampleTime

                val mime =
                    mimeToExtractorTrack.entries.find { it.value == extractorTraceIndex }?.key
                        ?: run {
                            extractor.advance()
                            continue
                        }
                val muxerTrackIndex = mimeToMuxerTrack[mime] ?: run {
                    extractor.advance()
                    continue
                }
                bufferInfo.offset = 0
                bufferInfo.presentationTimeUs = sampleTimeUs + ptsOffsetUs
                bufferInfo.flags = getSampleFlags(extractor)

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            ptsOffsetUs += durationMs * 1000L
            extractor.release()
        }

        muxer.stop()
        muxer.release()
    }

    fun saveToGallery(context: Context, filePath: String): String {
        val file = File(filePath)

        val value = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, value)
            ?: throw Exception("MediaStore URI 생성 실패")

        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        value.clear()
        value.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, value, null, null)

        file.delete()
        return uri.toString()
    }
}