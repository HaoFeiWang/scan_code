package com.whf.scancode;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.renderer.FlutterRenderer;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

/** ScanCodePlugin */
public class ScanCodePlugin implements FlutterPlugin {

  private static final String CHANNEL = "com.whf.plugin/scan_code";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    final MethodChannel channel = new MethodChannel(
            flutterPluginBinding.getFlutterEngine().getDartExecutor(), CHANNEL);
    channel.setMethodCallHandler(new MethodCallHandlerImpl(flutterPluginBinding));
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
  }
}
