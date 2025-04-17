package com.jieli.btsmart.tool.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.utils.JL_Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片压缩
 *
 * @author zqjasonZhong
 * @since 2020/6/18
 */
public class ImageCompress {

    private final static String TAG = ImageCompress.class.getSimpleName();
    private volatile static ImageCompress instance;
    private final ExecutorService mService = Executors.newFixedThreadPool(5);

    private ImageCompress() {
    }

    public static ImageCompress getInstance() {
        if (instance == null) {
            synchronized (ImageCompress.class) {
                if (instance == null) {
                    instance = new ImageCompress();
                }
            }
        }
        return instance;
    }

    public void compress(Context context, String filePath, IActionCallback<String> callback) {
        if (mService != null && !mService.isShutdown()) {
            mService.submit(new ImageCompressTask(context, filePath, callback));
        }
    }

    public void destroy() {
        if (mService != null) {
            if (!mService.isShutdown()) {
                mService.shutdown();
            }
        }
        instance = null;
    }

    /**
     * 根据分辨率压缩图片比例
     *
     * @param imgPath 图片路径
     */
    private void compressByResolution(Context context, String imgPath) {
        if (context == null || imgPath == null) return;
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        if (bitmap == null) return;
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return;
        windowManager.getDefaultDisplay().getMetrics(metric);
        float h = 130 * metric.density;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        JL_Log.d(TAG, "compressByResolution : "+ ", h : " + h + ", " + width + ", " + height + ", " + metric.density);
        if (height <= h) {
            return;
        }
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();// 计算宽高缩放率
        float scaleHeight =  h / height;// 缩放图片动作
        JL_Log.i(TAG, "compressByResolution："+ ", " + scaleHeight);
        matrix.postScale(scaleHeight, scaleHeight);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0,  width, height, matrix, true);
        if (bmp == null) return;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        bmp.compress(Bitmap.CompressFormat.PNG, options, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
//        JL_Log.i(TAG, "compressByResolution：bmp ： " + bmp);
        try {
            FileOutputStream fos = new FileOutputStream(new File(imgPath));//将压缩后的图片保存的本地上指定路径中
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ImageCompressTask implements Runnable {
        private Context mContext;
        private String imagePath;
        private IActionCallback<String> mCallback;

        public ImageCompressTask(Context context, String path, IActionCallback<String> callback) {
            mContext = context;
            imagePath = path;
            mCallback = callback;
        }

        @Override
        public void run() {
            compressByResolution(mContext, imagePath);
            if (mCallback != null && mContext != null) {
                Handler mHandler = new Handler(mContext.getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSuccess(imagePath);
                    }
                });
            }
        }
    }
}
