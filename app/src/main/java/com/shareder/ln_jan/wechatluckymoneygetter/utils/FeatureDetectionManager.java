package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.shareder.ln_jan.wechatluckymoneygetter.global.MyApplication;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Ln_Jan on 2018/12/15.
 * 微信红包特征检测
 */

public class FeatureDetectionManager {
    private static FeatureDetectionManager instance = null;
    private static final String STRING_LUCKYMONEY_NAME = "[微信红包]";
    private String strMoneyPicPath;
    private Bitmap bmLocal = null;

    private FeatureDetectionManager() {
        strMoneyPicPath = MyApplication.getContext().getCacheDir().getAbsolutePath() + File.separator + "luckymoney_pic.jpg";
    }

    public static FeatureDetectionManager getInstance() {
        if (instance == null) {
            instance = new FeatureDetectionManager();
        }
        return instance;
    }

    /**
     * 检测输入的图像是否匹配[微信红包]的本地图像
     *
     * @param bmInput        输入的图像
     * @param isNormalScreen 对全面屏手机截取的区域不同，特征点也会不同
     * @return
     */
    public boolean isPictureMatchLuckyMoney(Bitmap bmInput, boolean isNormalScreen) throws CvException {
        if (!isCachePictureExist()) {
            /*if (!createLuckyMoneyPicture()) {
                return false;
            }*/
            return false;
        }

        if (bmLocal == null) {
            bmLocal = BitmapFactory.decodeFile(strMoneyPicPath);
        }

        Mat inputGrayMat = getGrayMat(bmInput);
        Mat localGrayMat = getGrayMat(bmLocal);


        //特征点提取
        ORB orb = ORB.create(1000);                           //精度越小越准确
        MatOfKeyPoint kptsInput = new MatOfKeyPoint();
        MatOfKeyPoint kptsLocal = new MatOfKeyPoint();
        orb.detect(inputGrayMat, kptsInput);
        orb.detect(localGrayMat, kptsLocal);

        //特征点描述,采用ORB默认的描述算法
        Mat descInput = new Mat();
        Mat descLocal = new Mat();
        orb.compute(inputGrayMat, kptsInput, descInput);
        orb.compute(localGrayMat, kptsLocal, descLocal);

        //BFMatcher matcher = new BFMatcher(BFMatcher.BRUTEFORCE_HAMMING, false);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matchPoints = new MatOfDMatch();
        //Log.e("matchoutput", "--start---");
        //matcher.knnMatch(descInput,descLocal,matchPointsList,2);
        try {
            matcher.match(descInput, descLocal, matchPoints);
        } catch (CvException ex) {
            Log.e("matchoutput", ex.toString());
            return false;
        }
        //Log.e("matchoutput", "--end---");

        float min_dist = 0;

        DMatch[] arrays = matchPoints.toArray();

        for (int i = 0; i < descInput.rows(); ++i) {
            float dist = arrays[i].distance;
            if (dist < min_dist) min_dist = dist;
        }

        int goodMatchPointNum = 0;

        //筛选特征点
        float compareNum = Math.max(min_dist * 2, 30.0f);

        for (int j = 0; j < descInput.rows(); j++) {
            if (arrays[j].distance <= compareNum) {
                goodMatchPointNum++;
            }
        }

        Log.e("matchoutput", goodMatchPointNum + "");

        if (isNormalScreen) {
            return goodMatchPointNum > 10;
        } else {
            return goodMatchPointNum >= 7;
        }

        //return false;
    }

    /**
     * 检查本地缓存是否有[微信红包]的图片
     *
     * @return boolean
     */
    public boolean isCachePictureExist() {
        File f = new File(strMoneyPicPath);
        return f.exists();
    }

    /**
     * 创建[微信红包]样式的图片在本地缓存中，用于后续的特征识别
     */
    public boolean createLuckyMoneyPicture() {
        boolean b = true;
        Bitmap bm = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(STRING_LUCKYMONEY_NAME, 20, 50, paint);
        try {
            File f = new File(strMoneyPicPath);
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    return false;
                }
            }
            FileOutputStream outStream = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            b = false;
        }
        return b;
    }

    public String getMoneyPicPath() {
        return strMoneyPicPath;
    }

    /**
     * 获取灰度化的Mat，减少特征匹配时的计算量
     *
     * @param bm input
     * @return
     */
    private Mat getGrayMat(Bitmap bm) {
        Mat srcMat = new Mat();
        Utils.bitmapToMat(bm, srcMat);
        Mat grayMat = new Mat();
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        return grayMat;
    }
}
