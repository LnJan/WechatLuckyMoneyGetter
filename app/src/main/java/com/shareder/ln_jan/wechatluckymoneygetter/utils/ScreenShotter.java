package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;

import java.nio.ByteBuffer;

/**
 * Created by Ln_Jan on 2018/12/13.
 * 截屏工具
 */

public class ScreenShotter {
    private static ScreenShotter instance = null;

    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;

    private ScreenShotter() {
        mMediaProjection = null;
        mImageReader = null;
    }

    public static ScreenShotter getInstance() {
        if (instance == null) {
            instance = new ScreenShotter();
        }
        return instance;
    }

    public void setMediaProjection(MediaProjection projection) {
        mMediaProjection = projection;
    }

    public boolean isShotterUseful() {
        return mMediaProjection != null;
    }

    public Bitmap getScreenShotSync() {
        if (!isShotterUseful()) {
            return null;
        }

        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(
                    getScreenWidth(),
                    getScreenHeight(),
                    PixelFormat.RGBA_8888,//此处必须和下面 buffer处理一致的格式 ，RGB_565在一些机器上出现兼容问题。
                    1);
        }

        VirtualDisplay tmpDisplay = virtualDisplay();
        try{
            Thread.sleep(50);                   //需要稍微停一下，否则截图为空
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        Image img = mImageReader.acquireLatestImage();

        if (img == null) {
            return null;
        }

        int width = img.getWidth();
        int height = img.getHeight();
        final Image.Plane[] planes = img.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        //每个像素的间距
        int pixelStride = planes[0].getPixelStride();
        //总的间距
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                Bitmap.Config.ARGB_8888);//虽然这个色彩比较费内存但是 兼容性更好
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        img.close();
        //mImageReader.close();
        tmpDisplay.release();
        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay virtualDisplay() {
        return mMediaProjection.createVirtualDisplay("screen-mirror",
                getScreenWidth(),
                getScreenHeight(),
                Resources.getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    private int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
