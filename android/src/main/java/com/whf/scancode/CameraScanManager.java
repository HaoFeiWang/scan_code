package com.whf.scancode;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * 采用Camera1会导致Flutter中的预览帧频繁卡死
 */
public class CameraScanManager {

    private static final String TAG = CameraScanManager.class.getSimpleName();

    private Context context;
    private Handler handler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    private ScanParams scanParams;
    private SurfaceTexture surfaceTexture;

    public CameraScanManager(Context context) {
        HandlerThread handlerThread = new HandlerThread("ScanCode");
        handlerThread.start();

        this.context = context;
        this.handler = new Handler(handlerThread.getLooper());
    }

    public void startScan(SurfaceTexture surfaceTexture, ScanParams scanParams) {
        this.scanParams = scanParams;
        this.surfaceTexture = surfaceTexture;

        handler.post(new Runnable() {
            @Override
            public void run() {
                startPreviewAsync();
            }
        });
    }

    private void startPreviewAsync() {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            cameraManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraScanManager.this.cameraDevice = cameraDevice;
                    createCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {

                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCaptureSession() {
        try {
            surfaceTexture.setDefaultBufferSize(scanParams.getWidth(), scanParams.getHeight());
            ImageReader pictureImageReader = createScanImageReader();

            Surface surface = new Surface(surfaceTexture);
            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            builder.addTarget(pictureImageReader.getSurface());

            List<Surface> surfaceList = new ArrayList<>();
            surfaceList.add(surface);
            surfaceList.add(pictureImageReader.getSurface());

            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    sendRequest(cameraCaptureSession, builder);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private ImageReader createScanImageReader() {
        ImageReader pictureImageReader = ImageReader.newInstance(
                scanParams.getWidth(), scanParams.getHeight(), ImageFormat.JPEG, 2);

        pictureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                scanRGBImage(image);
                image.close();
            }
        }, handler);

        return pictureImageReader;
    }

    private void sendRequest(CameraCaptureSession cameraCaptureSession, CaptureRequest.Builder builder) {
        try {
            this.cameraCaptureSession = cameraCaptureSession;
            cameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void scanRGBImage(Image image) {
        Log.d(TAG, "image width = " + image.getWidth() + " height = " + image.getHeight());

        int length = image.getPlanes().length;
        Log.d(TAG, "planes length = " + length);

        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        int size = byteBuffer.remaining();
        Log.d(TAG, "byteBuffer remaining = " + size);

        byte[] imageByte = new byte[size];
        byteBuffer.get(imageByte);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);

        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        Bitmap newBM = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, false);

        int width = newBM.getWidth();
        int height = newBM.getHeight();
        int[] pixels = new int[width * height];
        newBM.getPixels(pixels, 0, width, 0, 0, width, height);

        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();

        try {
            Result result = reader.decodeWithState(bitmap1);
            parseResult(result);
        } catch (Exception e) {
            //no-op
        }
    }

    private void parseResult(final Result result) {
        Log.d(TAG, "parse result = " + result.getText());
        EventMessenger.getInstance().sendEvent(result.getText());
    }

    public void stopScan() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
