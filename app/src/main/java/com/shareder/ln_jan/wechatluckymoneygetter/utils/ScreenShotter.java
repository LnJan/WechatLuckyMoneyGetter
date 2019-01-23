package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.DeadSystemException;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.shareder.ln_jan.wechatluckymoneygetter.global.MyApplication;

import java.nio.ByteBuffer;

/**
 * Created by Ln_Jan on 2018/12/13.
 * 截屏工具
 */

public class ScreenShotter {
    private static ScreenShotter instance = null;

    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private boolean mIsNormalScreen = true;
    private volatile Point[] mRealSizes = new Point[2];
    private int mScreenRealHeight;

    private static final int PORTRAIT = 0;
    private static final int LANDSCAPE = 1;

    private ScreenShotter() {
        mMediaProjection = null;
        mImageReader = null;
        mIsNormalScreen = checkScreenSizeIsNormal();
        mScreenRealHeight = mIsNormalScreen ? getScreenHeight() : getScreenRealHeight();
    }

    public static ScreenShotter getInstance() {
        if (instance == null) {
            instance = new ScreenShotter();
        }
        return instance;
    }

    public boolean isNormalScreen() {
        return mIsNormalScreen;
    }

    public void setMediaProjection(MediaProjection projection) {
        mMediaProjection = projection;
    }

    public boolean isShotterUseful() {
        return mMediaProjection != null;
    }

    public Bitmap getScreenShotSync() throws DeadSystemException {
        if (!isShotterUseful()) {
            return null;
        }

        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(
                    getScreenWidth(),
                    mScreenRealHeight,
                    PixelFormat.RGBA_8888,//此处必须和下面 buffer处理一致的格式 ，RGB_565在一些机器上出现兼容问题。
                    1);
        }

        VirtualDisplay tmpDisplay = virtualDisplay();
        try {
            Thread.sleep(50);                   //需要稍微停一下，否则截图为空
        } catch (InterruptedException e) {
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

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /**
     * 某些全面屏手机获取的屏幕宽度不对，需用此方法获取宽度
     *
     * @return
     */
    public int getScreenRealHeight() {
        int orientation = MyApplication.getContext().getResources().getConfiguration().orientation;
        orientation = orientation == Configuration.ORIENTATION_PORTRAIT ? PORTRAIT : LANDSCAPE;

        if (mRealSizes[orientation] == null) {
            WindowManager windowManager = (WindowManager) MyApplication.getContext().getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) {
                return getScreenHeight();
            }
            Display display = windowManager.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            mRealSizes[orientation] = point;
        }
        return mRealSizes[orientation].y;
    }

    /**
     * 检查屏幕尺寸是否16:9
     * 某些18:9的全面屏手机会出现截屏位置偏移的问题
     *
     * @return
     */
    private boolean checkScreenSizeIsNormal() {
        DisplayMetrics dm = new DisplayMetrics();
        dm = Resources.getSystem().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        return screenHeight < screenWidth * 1.8;
    }
}
