import 'package:flutter/services.dart';
import 'package:video_poc/controller/video_controller.dart';

class AppController {
  final VideoController videoController = VideoController();

  MethodChannel videoChannel = MethodChannel("video_editor_channel");

  int startMs = 0;
  int endMs = 10000;

  Future<String?> cutVideo(String inputPath) async {
    return await videoChannel.invokeMethod('cut_video', {
      'inputPath': inputPath,
      'startMs': startMs,
      'endMs': endMs,
    });
  }
}
