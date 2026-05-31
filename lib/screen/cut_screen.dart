import 'package:flutter/material.dart';
import 'package:video_poc/controller/app_controller.dart';

class CutScreen extends StatefulWidget {
  const CutScreen({super.key});

  @override
  State<CutScreen> createState() => _CutScreenState();
}

class _CutScreenState extends State<CutScreen> {

  final controller = AppController.controller;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Cut 편집"),),
      body: Column(
        children: [

        ],
      ),
    );
  }
}
