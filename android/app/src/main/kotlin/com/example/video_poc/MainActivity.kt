package com.example.video_poc

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private val videoChannel = "video_editor_channel"

    //백그라운드 레이어에서 실행
    private val executor = Executors.newSingleThreadExecutor()

    //백그라운드에서 작업 후 메인으로 결과 반환 -> 필요
    private val mainHandler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            videoChannel
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "cut_video" -> {
                    val inputPath = call.argument<String>("inputPath")!!
                    val startMs = call.argument<Int>("startMs")!!.toLong()
                    val endMs = call.argument<Int>("endMs")!!.toLong()

                    val outPath =
                        "${context.cacheDir.absolutePath}/cut_${System.currentTimeMillis()}.mp4"

                    executor.execute {
                        try {
                            VideoEdit.cut(inputPath, outPath, startMs, endMs)

                            val galleryUri = VideoEdit.saveToGallery(context, outPath)

                            mainHandler.post { result.success(galleryUri) }
                        } catch (e: Exception) {
                            mainHandler.post { result.error("CUT_ERROR", e.message, null) }
                        }
                    }
                }

                "mix_video" -> {
                    val inputPath = call.argument<List<String>>("inputPaths")!!
                    val outPath =
                        "${context.cacheDir.absolutePath}/mix_${System.currentTimeMillis()}.mp4"

                    executor.execute {
                        try {
                            VideoEdit.mix(inputPath, outPath)
                            val galleryUri = VideoEdit.saveToGallery(context, outPath)
                            mainHandler.post { result.success(galleryUri) }
                        } catch (e: Exception) {
                            mainHandler.post { result.error("MIX_ERROR", e.message, null) }
                        }
                    }
                }

                "get_duration" -> {
                    val path = call.argument<String>("path")!!
                    executor.execute {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(path)
                            val durationMs = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION
                            )?.toInt() ?: 0
                            retriever.release()

                            mainHandler.post { result.success(durationMs) }
                        } catch (e: Exception) {
                            mainHandler.post { result.error("DURATION ERROR", e.message, null) }
                        }
                    }
                }


                else -> result.notImplemented()
            }
        }
    }
}

