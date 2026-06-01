import 'package:image_picker/image_picker.dart';

class VideoController {
  final ImagePicker picker = ImagePicker();
  final List<XFile> videos = [];
  final Map<String, String> duration = {};
  final Map<String, int> durationMs = {};

  Future<XFile?> pickVideo() async {
    return await picker.pickVideo(source: ImageSource.gallery);
  }

  void addVideo(XFile video, int ms) {
    videos.add(video);
    durationMs[video.path] = ms;
    final d = Duration(milliseconds: ms);
    duration[video.path] =
    '${d.inMinutes}:${(d.inSeconds % 60).toString().padLeft(2, '0')}';
  }
}