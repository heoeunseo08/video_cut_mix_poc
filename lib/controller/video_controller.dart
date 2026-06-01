import 'package:image_picker/image_picker.dart';

class VideoController {
  final ImagePicker picker = ImagePicker();
  final List<XFile> videos = [];
  final Map<String, String> duration = {};
  final Map<String, int> durationMS = {};

  Future<XFile?> pickVideo() async {
    return picker.pickVideo(source: ImageSource.gallery);
  }

  void addVideo(XFile video, int ms) {
    videos.add(video);
    durationMS[video.path] = ms;
    final d = Duration(milliseconds: ms);
    duration[video.path] =
        '${d.inMinutes}:${(d.inSeconds % 60).toString().padLeft(2, '0')}';
  }

  String totalDuration() {
    final totalMs = durationMS.values.fold<int>(
      0,
      (previousValue, element) => previousValue + element,
    );
    final d = Duration(milliseconds: totalMs);
    final m = d.inMinutes.toString().padLeft(2, '0');
    final s = (d.inSeconds % 60).toString().padLeft(2, '0');

    return '$m:$s';
  }
}
