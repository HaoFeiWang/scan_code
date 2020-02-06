import 'package:flutter/material.dart';
import 'package:scan_code/scan_code_manager.dart';

class HomePage extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Center(
        child: FlatButton(
          onPressed: ()=>startBarScan(context),
          child: Text("开启相机扫描"),
        ),
      ),
    );
  }

  void startBarScan(BuildContext context) async {
    int textureId = await ScanCodeManager().startScan(1280, 640);
    Navigator.of(context).pushReplacementNamed("scanCodePage",arguments: textureId);
  }

}