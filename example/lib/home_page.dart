import 'package:flutter/material.dart';

class HomePage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _HomePageState();
  }
}

class _HomePageState extends State<HomePage> {
  dynamic scanResult;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          FlatButton(
            onPressed: startScan,
            child: Text("开始扫描"),
          ),
          SizedBox(height: 20),
          Text("扫描结果：$scanResult"),
        ],
      ),
    );
  }

  void startScan() async {
    scanResult = await Navigator.of(context).pushNamed("scanCodePage");
    setState(() {});
  }
}
