import 'dart:io';

import 'package:image_picker/image_picker.dart';
import 'package:video_player/video_player.dart';

class VideoController {
  final ImagePicker picker = ImagePicker();
  final List<XFile> videos = [];
  final Map<String, String> duration = {};

  Future<void> pickVideo() async {
    final XFile? video = await picker.pickVideo(source: ImageSource.gallery);

    if (video != null) {
      videos.add(video);
      duration[video.path] = await getVideoTime(video);
    }
  }

  Future<String> getVideoTime(XFile file) async {
    final controller = VideoPlayerController.file(File(file.path));

    await controller.initialize();
    final duration = controller.value.duration;
    await controller.dispose();
    return '${duration.inMinutes}:${(duration.inSeconds % 60).toString().padLeft(2, '0')}';
  }
}
