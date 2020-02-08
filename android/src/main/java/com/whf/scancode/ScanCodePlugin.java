package com.whf.scancode;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.TextureRegistry;

/**
 * ScanCodePlugin
 */
public class ScanCodePlugin implements FlutterPlugin, ActivityAware,
        EventChannel.StreamHandler, MethodChannel.MethodCallHandler,
        PluginRegistry.RequestPermissionsResultListener {

    private static final String TAG = ScanCodePlugin.class.getSimpleName();

    private static final String CHANNEL = "com.whf.plugin/scan_code";
    private static final String EVENT_CHANNEL = "com.whf.plugin/scan_code/event";
    private static final String METHOD_SCAN = "startScan";
    private static final String METHOD_STOP = "stopScan";

    private static final int CAMERA_REQUEST_ID = 41284;

    private FlutterPluginBinding flutterBinding;
    private CameraScanManager cameraScanManager;
    private Activity activity;

    private MethodCall pendingCall;
    private Result pendingResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "onAttachedToEngine" + this);
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
        Log.d(TAG, "onDetachedFromEngine");
        if (cameraScanManager != null) {
            cameraScanManager.stopScan();
            cameraScanManager = null;
        }

        EventMessenger.getInstance().cleanEventSink();
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        Log.d(TAG, "onAttachedToActivity");
        activity = binding.getActivity();
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        Log.d(TAG, "onDetachedFromActivityForConfigChanges");

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        Log.d(TAG, "onReattachedToActivityForConfigChanges");
    }

    @Override
    public void onDetachedFromActivity() {
        Log.d(TAG, "onDetachedFromActivity");
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

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_ID);
        return false;
    }

    private void scan(MethodCall call, Result result) {
        if (!checkPermission()) {
            pendingCall = call;
            pendingResult = result;
            Log.d(TAG, "need request camera permission!");
            return;
        }

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

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG,"onRequestPermissionsResult");

        if (pendingCall == null || pendingResult == null) {
            Log.d(TAG,"pendingCall or pendingResult is null");
            return true;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMethodCall(pendingCall, pendingResult);
        } else {
            Log.d(TAG,"result PermissionsError!");
            pendingResult.error("PermissionsError", null, null);
        }

        return true;
    }
}
