import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

MethodChannel _channel = const MethodChannel('com.whf.plugin/scan_code');
EventChannel _eventChannel =
    const EventChannel('com.whf.plugin/scan_code/event');

class ScanCodeView extends StatefulWidget {
  ScanEventListener scanEventListener;

  ScanCodeView(this.scanEventListener);

  @override
  State<StatefulWidget> createState() {
    return _ScanCodeViewState(scanEventListener);
  }
}

class _ScanCodeViewState extends State<ScanCodeView> {
  WidgetsBinding widgetsBinding;
  ScanEventListener scanEventListener;

  int textureId;
  int previewWidth;
  int previewHeight;
  bool scanning = false;
  bool textureIdReady = false;

  StreamSubscription eventSubscription;

  _ScanCodeViewState(this.scanEventListener);

  @override
  void initState() {
    super.initState();

    eventSubscription =
        _eventChannel.receiveBroadcastStream().listen(scanResultListener);

    widgetsBinding = WidgetsBinding.instance;
    widgetsBinding.addPostFrameCallback((callback) {
      previewWidth = context.size.width.toInt();
      previewHeight = context.size.height.toInt();
      startScan();
    });
  }

  @override
  void deactivate() {
    super.deactivate();
    eventSubscription?.cancel();
    stopScan();
  }

  @override
  Widget build(BuildContext context) {
    return textureIdReady
        ? Texture(textureId: textureId)
        : Container(
            width: double.infinity,
            height: double.infinity,
          );
  }

  Future startScan() async {
    if (scanning) {
      return;
    }

    scanning = true;
    return _channel.invokeMethod("startScan", <String, int>{
      "previewWidth": previewWidth,
      "previewHeight": previewHeight
    }).then((value) {
      textureId = value;
      textureIdReady = true;
      setState(() {});
    });
  }

  Future stopScan() async {
    return _channel.invokeMethod("stopScan").then((data) {
      scanning = false;
    });
  }

  void scanResultListener(dynamic event) {
    if (scanEventListener != null) {
      scanEventListener(event);
    }
  }
}

typedef ScanEventListener(dynamic event);
