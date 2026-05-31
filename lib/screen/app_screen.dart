import 'package:flutter/material.dart';
import 'package:video_poc/controller/app_controller.dart';
import 'package:video_poc/screen/cut_screen.dart';
import 'package:video_poc/screen/mix_screen.dart';
import 'package:video_poc/utils/utils.dart';

class AppScreen extends StatefulWidget {
  const AppScreen({super.key});

  @override
  State<AppScreen> createState() => _AppScreenState();
}

class _AppScreenState extends State<AppScreen> {
  bool isCut = true;

  final AppController controller = AppController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Video Cut/Mix")),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await controller.videoController.pickVideo();
          setState(() {});
        },
        child: Icon(Icons.add),
      ),
      body: Column(
        mainAxisSize: MainAxisSize.max,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            alignment: .center,
            width: MediaQuery.of(context).size.width,
            child: ToggleButtons(
              isSelected: [isCut, !isCut],
              children: [Text("Cut"), Text("Mix")],
              onPressed: (index) => setState(() => isCut = index == 0),
            ),
          ),
          controller.videoController.videos.isEmpty
              ? Text("+버튼으로 비디오 추가")
              : Expanded(
                  child: ListView.builder(
                    itemCount: controller.videoController.videos.length,

                    itemBuilder: (context, index) {
                      final video = controller.videoController.videos[index];

                      return ListTile(
                        leading: Icon(Icons.video_file),
                        title: Text(
                          video.name,
                        ),
                        subtitle: FutureBuilder(
                          future: controller.videoController.getVideoTime(
                            video,
                          ),
                          builder: (context, snapshot) {
                            if (!snapshot.hasData) return Text("...");
                            return Text(snapshot.data!);
                          },
                        ),
                      );
                    },
                  ),
                ),
          Padding(
            padding: const EdgeInsets.only(bottom: 50),
            child: ElevatedButton(
              onPressed: () {
                if(controller.videoController.videos.isEmpty){
                  showMessage(context, "비디오를 먼저 선택하세요");
                  return;
                }
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => isCut ? CutScreen() : MixScreen(),
                  ),
                );
              },
              child: Text(isCut ? "Cut 편집" : "Mix 편집"),
            ),
          ),
        ],
      ),
    );
  }
}
