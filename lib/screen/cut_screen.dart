import 'dart:io';

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:video_poc/controller/app_controller.dart';
import 'package:video_poc/utils/utils.dart';

class CutScreen extends StatefulWidget {
  const CutScreen({super.key});

  @override
  State<CutScreen> createState() => _CutScreenState();
}

class _CutScreenState extends State<CutScreen> {
  final controller = AppController.instance;
  VideoPlayerController? videoController;

  double startMs = 0;
  double endMs = 0;
  double totalMs = 0;

  @override
  void initState() {
    super.initState();
    initVideo();
  }

  Future<void> initVideo() async {
    try{
    final video = controller.videoController.videos.first;
    videoController = VideoPlayerController.file(File(video.path));
    await videoController!.initialize();

    final total = videoController!.value.duration.inMilliseconds.toDouble();
    setState(() {
      totalMs = total;
      endMs = total;
    });

    videoController!.addListener(() => setState(() {}));
    }
    catch(e){
      debugPrint('initVideo error : $e');
    }
  }

  @override
  void dispose() {
    videoController?.dispose();
    super.dispose();
  }

  String formatMs(double ms) {
    final duration = Duration(milliseconds: ms.toInt());
    final m = duration.inMinutes.toString().padLeft(2, '0');
    final s = (duration.inSeconds % 60).toString().padLeft(2, '0');

    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    final isInit =
        videoController != null && videoController!.value.isInitialized;

    return Scaffold(
      appBar: AppBar(
        title: Text("Cut 편집"),
      ),
      body: Column(
        children: <Widget>[
          if (isInit)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 40.0),
              child: GestureDetector(
                onTap: () {
                  videoController!.value.isPlaying
                      ? videoController!.pause()
                      : videoController!.play();
                },
                child: AspectRatio(
                  aspectRatio: videoController!.value.aspectRatio,
                  child: VideoPlayer(videoController!),
                ),
              ),
            )
          else
            Expanded(child: Center(child: CircularProgressIndicator())),
          SizedBox(height: 16),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text("시작: ${formatMs(startMs)}"),
                Text("끝: ${formatMs(endMs)}"),
              ],
            ),
          ),
          Padding(
            padding: .symmetric(horizontal: 12),
            child: RangeSlider(
              min: 0,
              max: totalMs,
              values: RangeValues(startMs, endMs),
              onChanged: (value) {
                final startChange = value.start != startMs;

                setState(() {
                  startMs = value.start;
                  endMs = value.end;
                });
                videoController!.seekTo(
                  Duration(
                    milliseconds: startChange ? startMs.toInt() : endMs.toInt(),
                  ),
                );
              },
            ),
          ),

          Text("선택: ${formatMs(endMs - startMs)}"),
          SizedBox(height: 30),
          ElevatedButton(
            onPressed: () async {
              controller.startMs = startMs.toInt();
              controller.endMs = endMs.toInt();

              final result = await controller.cutVideo(
                controller.videoController.videos.first.path,
              );

              Navigator.pop(context);

              showMessage(context, result != null ? "Cut 완료!" : "Cut 실패");
            },
            child: Text("Cut 실행"),
          ),
        ],
      ),
    );
  }
}
