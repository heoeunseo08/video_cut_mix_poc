import 'package:flutter/services.dart';
import 'package:video_poc/controller/video_controller.dart';

class AppController {
  static final AppController instance = AppController._();

  AppController._();

  final VideoController videoController = VideoController();
  MethodChannel videoChannel = MethodChannel("video_editor_channel");

  int startMs = 0;
  int endMs = 10000;

  Future<void> pickVideo() async {
    final video = await videoController.pickVideo();
    if (video == null) return;
    final ms = await getDuration(video.path);
    videoController.addVideo(video, ms);
  }

  Future<int> getDuration(String path) async =>
      await videoChannel.invokeMethod<int>('get_duration', {
        'path': path,
      }) ?? 0;

  Future<String?> cutVideo(String inputPath) async {
    return await videoChannel.invokeMethod('cut_video', {
      'inputPath': inputPath,
      'startMs': startMs,
      'endMs': endMs,
    });
  }

  Future<String?> mixVideo(List<String> inputPath) async {
    return await videoChannel.invokeMethod('mix_video', {
      'inputPath': inputPath
    });
  }
}
