import 'package:flutter/material.dart';
import 'package:video_poc/controller/app_controller.dart';
import 'package:video_poc/utils/utils.dart';

class MixScreen extends StatefulWidget {
  const MixScreen({super.key});

  @override
  State<MixScreen> createState() => _MixScreenState();
}

class _MixScreenState extends State<MixScreen> {
  final controller = AppController.instance;

  void totalDuration(){

  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Mix 편집"),
      ),

      body: Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: controller.videoController.videos.length,

              itemBuilder: (context, index) {
                final video = controller.videoController.videos[index];

                return Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: ListTile(
                        leading: Text(
                          "${index + 1}",
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        title: Text(
                          video.name,
                        ),
                        subtitle: Text(
                          controller.videoController.duration[video.path] ??
                              "...",
                          style: TextStyle(fontSize: 15),
                        ),
                      ),
                    ),
                    Padding(
                      padding: .only(right: 50),
                      child: GestureDetector(
                        onTap: () {
                          final path =
                              controller.videoController.videos[index].path;
                          if (controller.videoController.videos.length > 2) {
                            setState(() {
                              controller.videoController.videos.removeAt(
                                index,
                              );
                              controller.videoController.duration.remove(
                                path,
                              );
                            });
                          }
                        },
                        child: Icon(Icons.close, size: 25, color: Colors.grey),
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text("${controller.videoController.videos.length}개"),
                ],
              ),
              ElevatedButton(
                onPressed: () async {
                  Navigator.pop(context);

                  showMessage(context, "Cut 완료!");
                },
                child: Text("Mix 실행"),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
