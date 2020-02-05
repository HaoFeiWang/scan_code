import 'package:flutter/material.dart';

import 'package:scan_code_example/home_page.dart';
import 'package:scan_code_example/scan_code_page.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  final routes = <String, WidgetBuilder>{
    "scanCodePage": (context) => ScanCodePage(),
    "homePage":(context) => HomePage(),
  };

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      routes: routes,
      home: HomePage(),
    );
  }
}
