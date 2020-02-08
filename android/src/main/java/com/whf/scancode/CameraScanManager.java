package com.whf.scancode;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * 采用Camera1会导致Flutter中的预览帧频繁卡死
 */
public class CameraScanManager {

    private static final String TAG = CameraScanManager.class.getSimpleName();

    private Context context;
    private Handler handler;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    private ScanParams scanParams;
    private SurfaceTexture surfaceTexture;
    private ImageReader pictureImageReader;

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
                startScanAsync();
            }
        });
    }

    private void startScanAsync() {
        try {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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
            Log.e(TAG, "openCamera error = " + e);
            e.printStackTrace();
        }
    }

    private void createCaptureSession() {
        try {
            setDefaultBufferSize();
            createScanImageReader();

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
            Log.e(TAG, "createCaptureSession error = " + e);
            e.printStackTrace();
        }
    }

    private void setDefaultBufferSize() {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("0");
            StreamConfigurationMap streamConfigurationMap = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] sizeArray = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            List<Size> sizeList = Arrays.asList(sizeArray);
            Collections.reverse(sizeList);

            //设置相机预览尺寸比例和surface的尺寸比例相同
            int finalWidth = 0;
            int finalHeight = 0;
            float surfaceProportion = scanParams.getHeight() / (float) scanParams.getWidth();
            float difference = Integer.MAX_VALUE;

            for (Size size : sizeList) {
                float proportion = size.getWidth() / (float) size.getHeight();
                float curDiff = Math.abs(proportion - surfaceProportion);

                if (Math.abs(proportion - surfaceProportion) < difference) {
                    difference = curDiff;
                    finalWidth = size.getWidth();
                    finalHeight = size.getHeight();
                }
            }

            Log.d(TAG, "default buffer size width: " + finalHeight + " height = " + finalHeight);
            surfaceTexture.setDefaultBufferSize(finalWidth, finalHeight);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setDefaultBufferSize error = " + e);
            e.printStackTrace();
        }
    }

    private void createScanImageReader() {
        if (pictureImageReader != null) {
            return;
        }

        pictureImageReader = ImageReader.newInstance(
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
    }

    private void sendRequest(CameraCaptureSession cameraCaptureSession, CaptureRequest.Builder builder) {
        try {
            this.cameraCaptureSession = cameraCaptureSession;
            cameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setRepeatingRequest error = " + e);
            e.printStackTrace();
        }
    }

    private void scanRGBImage(Image image) {
        Log.d(TAG, "image width = " + image.getWidth() + " height = " + image.getHeight());

        //RGB扫码，扫出的码有错误
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



        //YUV分量
        /*byte[] imageByte = getBytesFromImageAsType(image);
        byte[] rotateImageByte = rotateYUV420Degree90(imageByte, image.getWidth(), image.getHeight());
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                rotateImageByte, image.getHeight(), image.getWidth(),
                0, 0, image.getHeight(), image.getWidth(), false);*/



        //只有Y分量
        /*ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        int size = byteBuffer.remaining();
        Log.d(TAG, "byteBuffer remaining = " + size);
        byte[] imageByte = new byte[size];
        byteBuffer.get(imageByte);
        byte[] rotateImageByte = rotateYUV420Degree90(imageByte, image.getWidth(), image.getHeight());
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                rotateImageByte, image.getHeight(), image.getWidth(),
                0, 0, image.getHeight(), image.getWidth(), false);*/

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();

        Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        decodeFormats.add(BarcodeFormat.EAN_13);
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        reader.setHints(hints);

        try {
            Result result = reader.decodeWithState(binaryBitmap);
            parseResult(result);
        } catch (Exception e) {
            //no-op
        }
    }

    private byte[] getBytesFromImageAsType(Image image) {
        try {
            //获取源数据，如果是YUV格式的数据planes.length = 3
            //plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
            final Image.Plane[] planes = image.getPlanes();

            //数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
            // 所以我们只取width部分
            int width = image.getWidth();
            int height = image.getHeight();

            //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
            byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            //目标数组的装填到的位置
            int dstIndex = 0;

            //临时存储uv数据的
            byte uBytes[] = new byte[width * height / 4];
            byte vBytes[] = new byte[width * height / 4];
            int uIndex = 0;
            int vIndex = 0;

            int pixelsStride, rowStride;
            for (int i = 0; i < planes.length; i++) {
                pixelsStride = planes[i].getPixelStride();
                rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                //如果pixelsStride==2，一般的Y的buffer长度=640*480，UV的长度=640*480/2-1
                //源数据的索引，y的数据是byte中连续的，u的数据是v向左移以为生成的，两者都是偶数位为有效数据
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                int srcIndex = 0;
                if (i == 0) {
                    //直接取出来所有Y的有效区域，也可以存储成一个临时的bytes，到下一步再copy
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(bytes, srcIndex, yuvBytes, dstIndex, width);
                        srcIndex += rowStride;
                        dstIndex += width;
                    }
                } else if (i == 1) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                } else if (i == 2) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                }
            }

            //   image.close();

            //根据要求的结果类型进行填充

            System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
            System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);

            return yuvBytes;
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.i(TAG, e.toString());
        }
        return null;
    }

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {

        //只旋转Y
        /*byte[] yuv = new byte[imageWidth * imageHeight + imageWidth];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }*/


        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }

        return yuv;
    }

    private void parseResult(final Result result) {
        BarcodeFormat format = result.getBarcodeFormat();
        Log.d(TAG, "parse result = " + result.getText() + " format = " + format + " format name = " + format.name());
        EventMessenger.getInstance().sendEvent(result.getText());
    }

    public void stopScan() {
        if (pictureImageReader != null) {
            pictureImageReader.close();
            pictureImageReader = null;
        }
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
