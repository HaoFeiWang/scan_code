package com.whf.scancode;


import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

public class CameraPreviewManager {

    private static final String TAG = CameraPreviewManager.class.getSimpleName();

    public void startPreview(final SurfaceTexture surfaceTexture) {
        Log.d(TAG, "start preview");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Camera camera = Camera.open();
                try {
                    camera.setPreviewTexture(surfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "start preview error = " + e);
                }
            }
        }).start();
    }
}
