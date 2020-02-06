package com.whf.scancode;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.TextureRegistry;

public class MethodCallHandlerImpl implements MethodChannel.MethodCallHandler {

    private static final String TAG = MethodCallHandlerImpl.class.getSimpleName();

    private FlutterPlugin.FlutterPluginBinding flutterPluginBinding;

    public MethodCallHandlerImpl(FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        Log.d(TAG, "onMethodCall = " + call.method);
        switch (call.method) {
            case "scanBarCode":
                scanBarCode(call, result);
                break;

            default:
                result.notImplemented();
        }
    }

    private void scanBarCode(MethodCall call, MethodChannel.Result result) {
        TextureRegistry.SurfaceTextureEntry flutterSurfaceTexture =
                flutterPluginBinding.getTextureRegistry().createSurfaceTexture();

        CameraPreviewManager cameraPreviewManager = CameraPreviewManager
                .getInstance(flutterPluginBinding.getApplicationContext());
        cameraPreviewManager.startPreview(flutterSurfaceTexture.surfaceTexture());

        result.success(flutterSurfaceTexture.id());
    }
}
