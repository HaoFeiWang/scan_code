package com.whf.scancode;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraPreviewManager {

    private static final String TAG = CameraPreviewManager.class.getSimpleName();

    private static CameraPreviewManager instance;

    private Context context;
    private ExecutorService singleThreadPool;

    public synchronized static CameraPreviewManager getInstance(Context context) {
        if (instance == null) {
            instance = new CameraPreviewManager(context);
        }
        return instance;
    }

    private CameraPreviewManager(Context context) {
        this.context = context;
        this.singleThreadPool = Executors.newSingleThreadExecutor();
    }

    public void startPreview(final SurfaceTexture surfaceTexture) {
        startPreviewAsync(surfaceTexture);

        /*singleThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                startPreviewAsync(surfaceTexture);
            }
        });*/
    }

    private void startPreviewAsync(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "start preview");
        Camera camera = Camera.open();

        setDisplayOrientation(camera);
        setParameters(camera);
        surfaceTexture.setDefaultBufferSize(1280,640);
        camera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int i, Camera camera) {
                Log.d(TAG,"camera onError = "+i);
            }
        });

        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "start preview error = " + e);
        }
    }

    private void setParameters(Camera camera){
        Camera.Parameters parameters = camera.getParameters();
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewSize(1280,640);
        camera.setParameters(parameters);
    }

    private void setDisplayOrientation(Camera camera) {
        //获取屏幕方向
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏
            camera.setDisplayOrientation(90);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            camera.setDisplayOrientation(180);
        }
    }
}
