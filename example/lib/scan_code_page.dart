import 'package:flutter/cupertino.dart';
import 'package:scan_code/scan_code_view.dart';

class ScanCodePage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return ScanCodePageState();
  }
}

class ScanCodePageState extends State<ScanCodePage> {
  bool _pop = false;

  @override
  Widget build(BuildContext context) {
    return ScanCodeView(receiveScanEvent);
  }

  void receiveScanEvent(dynamic event) {
    if (!_pop) {
      _pop = true;
      Navigator.pop(context, event);
    }
  }
}
