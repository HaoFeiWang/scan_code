package com.whf.scancode;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2PreviewManager {

    private static final String TAG = Camera2PreviewManager.class.getSimpleName();

    private static Camera2PreviewManager instance;

    private ExecutorService singleThreadPool;

    private Context context;
    private CameraDevice cameraDevice;
    private SurfaceTexture surfaceTexture;

    public synchronized static Camera2PreviewManager getInstance(Context context) {
        if (instance == null) {
            instance = new Camera2PreviewManager(context);
        }
        return instance;
    }

    private Camera2PreviewManager(Context context) {
        this.context = context;
        this.singleThreadPool = Executors.newSingleThreadExecutor();
    }

    public void startPreview(final SurfaceTexture surfaceTexture) {

        try {
            startPreviewAsync();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        /*singleThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                startPreviewAsync(surfaceTexture);
            }
        });*/
    }

    private void startPreviewAsync() throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraManager.openCamera("0", new CameraDevice.StateCallback(){

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                Camera2PreviewManager.this.cameraDevice = cameraDevice;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {

            }
        },null);
    }

}
