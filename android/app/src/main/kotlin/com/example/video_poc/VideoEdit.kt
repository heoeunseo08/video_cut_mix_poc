package com.example.video_poc

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.nio.ByteBuffer

object VideoEdit {
    @RequiresApi(Build.VERSION_CODES.Q)
    fun cut(inPutPath: String, outputPath: String, startMs: Long, endMs: Long) {

        val totalDuration = Utils.getDuration(inPutPath).toLong()
        val rotation = Utils.getRotation(inPutPath)

        require(startMs >= 0 && endMs <= totalDuration && startMs < endMs) {
            "error : startMs= $startMs, endMs= $endMs, totalDuration= $totalDuration"
        }

        val extractor = MediaExtractor()
        extractor.setDataSource(inPutPath)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = HashMap<Int, Int>()
        val videoTrack = mutableListOf<Int>()

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackMap[i] = muxer.addTrack(format)
                if (mime.startsWith("video/")) videoTrack.add(i)
            }
        }

        val bufferSize = Utils.getBufferSize(extractor)
        extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (rotation != 0) muxer.setOrientationHint(rotation)
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
            bufferInfo.flags = Utils.getSampleFlags(extractor)

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

        val bufferSize = Utils.getBufferSize(firstExtractor)
        firstExtractor.release()

        val rotation = Utils.getRotation(inPutPath.first())

        if (rotation != 0) muxer.setOrientationHint(rotation)
        muxer.start()

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        var ptsOffsetUs = 0L

        for (path in inPutPath) {
            val extractor = MediaExtractor()
            extractor.setDataSource(path)

            val indexToMime = HashMap<Int, String>()
            var durationUs = 0L

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    indexToMime[i] = mime
                    if (mime.startsWith("video/")) {
                        durationUs = format.getLong(MediaFormat.KEY_DURATION)
                    }
                }
            }

            extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC)

            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val mime = indexToMime[extractor.sampleTrackIndex] ?: run {
                    extractor.advance()
                    continue
                }
                val muxerTrackIndex = mimeToMuxerTrack[mime] ?: run {
                    extractor.advance()
                    continue
                }

                bufferInfo.offset = 0
                bufferInfo.presentationTimeUs = extractor.sampleTime + ptsOffsetUs
                bufferInfo.flags = Utils.getSampleFlags(extractor)

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            ptsOffsetUs += durationUs
            extractor.release()
        }

        muxer.stop()
        muxer.release()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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