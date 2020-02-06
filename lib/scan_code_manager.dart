import 'dart:async';

import 'package:flutter/services.dart';

class ScanCodeManager {
  static const MethodChannel _channel =
      const MethodChannel('com.whf.plugin/scan_code');

  static const EventChannel _eventChannel =
      const EventChannel('com.whf.plugin/scan_code/event');

  ScanCodeManager() {
    _eventChannel.receiveBroadcastStream().listen(_listener);
  }

  Future<int> startScan(int previewWidth, int previewHeight) async {
    return _channel.invokeMethod("startScan", <String, int>{
      "previewWidth": previewWidth,
      "previewHeight": previewHeight
    });
  }

  Future<void> stopScan() async {
    return _channel.invokeMethod("stopScan");
  }

  void _listener(dynamic event) {
    print("WHF Flutter scan result = $event");
  }
}
