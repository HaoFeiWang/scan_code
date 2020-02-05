package com.whf.scancode;

import io.flutter.Log;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.TextureRegistry;

public class MethodCallHandlerImpl implements MethodChannel.MethodCallHandler {

    private static final String TAG = MethodCallHandlerImpl.class.getSimpleName();

    private TextureRegistry textureRegistry;

    public MethodCallHandlerImpl(TextureRegistry textureRegistry) {
        this.textureRegistry = textureRegistry;
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
                textureRegistry.createSurfaceTexture();

        CameraPreviewManager cameraPreviewManager = new CameraPreviewManager();
        cameraPreviewManager.startPreview(flutterSurfaceTexture.surfaceTexture());

        result.success(flutterSurfaceTexture.id());
    }
}
