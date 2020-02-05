import 'dart:async';

import 'package:flutter/services.dart';

class ScanCode {
  static const MethodChannel _channel = const MethodChannel('com.whf.plugin/scan_code');

  Future<int> scanBarCode() async{
    return _channel.invokeMethod("scanBarCode");
  }
}
