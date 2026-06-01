import 'package:flutter/material.dart';
import 'package:video_poc/screen/app_screen.dart';

void main() {
  FlutterError.onError = (details){
    debugPrint('Flutter error: ${details.exceptionAsString()}');
    debugPrint('Stack: ${details.stack}');
  };

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: AppScreen(),
    );
  }
}
