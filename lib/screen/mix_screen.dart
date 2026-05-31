import 'package:flutter/material.dart';
import 'package:video_poc/controller/app_controller.dart';

class MixScreen extends StatefulWidget {
  const MixScreen({super.key});

  @override
  State<MixScreen> createState() => _MixScreenState();
}

class _MixScreenState extends State<MixScreen> {

  final controller = AppController.controller;

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      appBar: AppBar(
        title: Text("Mix 편집"),),
    );
  }
}
