import 'dart:developer';

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

  final controller = AppController.instance;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Video Cut/Mix")),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await controller.pickVideo();
          setState(() {});
          log("${controller.videoController.videos.length}");
        },
        child: Icon(Icons.add),
      ),
      body: Column(
        mainAxisSize: MainAxisSize.max,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            alignment: Alignment.center,
            width: MediaQuery.of(context).size.width,
            child: ToggleButtons(
              isSelected: [isCut, !isCut],
              onPressed: (index) => setState(() => isCut = index == 0),
              constraints: BoxConstraints(minHeight: 50, minWidth: 70),
              children: [Text("Cut"), Text("Mix")],
            ),
          ),
          controller.videoController.videos.isEmpty
              ? Text("+버튼으로 비디오 추가")
              : Expanded(
                  child: ListView.builder(
                    itemCount: controller.videoController.videos.length,

                    itemBuilder: (context, index) {
                      final video = controller.videoController.videos[index];

                      return Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: ListTile(
                              leading: Icon(Icons.video_file),
                              title: Text(
                                video.name,
                              ),
                              subtitle: Text(
                                controller.videoController.duration[video
                                        .path] ??
                                    "...",
                              ),
                            ),
                          ),
                          isCut
                              ? Container()
                              : IconButton(
                                  onPressed: () => setState(() {
                                    final path = controller
                                        .videoController
                                        .videos[index]
                                        .path;
                                    controller.videoController.videos.removeAt(
                                      index,
                                    );
                                    controller.videoController.duration.remove(
                                      path,
                                    );
                                    controller.videoController.durationMS.remove(
                                      path,
                                    );
                                  }),
                                  icon: Icon(Icons.close, size: 25),
                                  padding: .only(right: 45),
                                ),
                        ],
                      );
                    },
                  ),
                ),
          Padding(
            padding: const EdgeInsets.only(bottom: 50),
            child: ElevatedButton(
              onPressed: () {
                if (controller.videoController.videos.isEmpty) {
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
