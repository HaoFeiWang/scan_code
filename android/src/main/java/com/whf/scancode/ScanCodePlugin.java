package com.whf.scancode;

import androidx.annotation.NonNull;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.TextureRegistry;

/**
 * ScanCodePlugin
 */
public class ScanCodePlugin implements FlutterPlugin,
        EventChannel.StreamHandler, MethodChannel.MethodCallHandler {

    private static final String TAG = ScanCodePlugin.class.getSimpleName();

    private static final String CHANNEL = "com.whf.plugin/scan_code";
    private static final String EVENT_CHANNEL = "com.whf.plugin/scan_code/event";
    private static final String METHOD_SCAN = "startScan";
    private static final String METHOD_STOP = "stopScan";

    private FlutterPluginBinding flutterBinding;
    private CameraScanManager cameraScanManager;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.flutterBinding = flutterPluginBinding;

        final MethodChannel channel = new MethodChannel(
                flutterPluginBinding.getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(this);

        final EventChannel eventChannel = new EventChannel(
                flutterPluginBinding.getBinaryMessenger(), EVENT_CHANNEL);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        cameraScanManager.stopScan();
        cameraScanManager = null;

        EventMessenger.getInstance().cleanEventSink();
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        EventMessenger.getInstance().setEventSink(events);
    }

    @Override
    public void onCancel(Object arguments) {
        EventMessenger.getInstance().cleanEventSink();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case METHOD_SCAN:
                scan(call, result);
                break;
            case METHOD_STOP:
                stop();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void scan(MethodCall call, Result result) {
        ScanParams scanParams = new ScanParams();
        scanParams.setWidth((Integer) call.argument("previewWidth"));
        scanParams.setHeight((Integer) call.argument("previewHeight"));
        Log.d(TAG, "scan params = " + scanParams);

        TextureRegistry.SurfaceTextureEntry flutterSurfaceTexture =
                flutterBinding.getTextureRegistry().createSurfaceTexture();
        result.success(flutterSurfaceTexture.id());

        if (cameraScanManager == null) {
            cameraScanManager = new CameraScanManager(flutterBinding.getApplicationContext());
        }
        cameraScanManager.startScan(flutterSurfaceTexture.surfaceTexture(), scanParams);
    }

    private void stop() {
        cameraScanManager.stopScan();
    }
}
