package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.tinker.LuckyMoneyTinkerApplication;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Ln_Jan on 2018/12/15.
 * 微信红包特征检测
 */

public class FeatureDetectionManager {
    private static final String TAG = "FeatureDetectionManager";
    private static FeatureDetectionManager instance = null;
    private static final String STRING_LUCKYMONEY_NAME = "[微信红包]";
    private String strMoneyPicPath;
    private Bitmap bmLocal = null;
    private Bitmap bmOpenLocal = null;                  //[开]的本地样本图片

    private FeatureDetectionManager() {
        strMoneyPicPath = LuckyMoneyTinkerApplication.getContext().getCacheDir().getAbsolutePath() + File.separator + "luckymoney_pic.jpg";
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
        if (bmInput == null) {
            return false;
        }

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
            Log.e(TAG, ex.toString());
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

        Log.e(TAG, goodMatchPointNum + "");

        if (isNormalScreen) {
            return goodMatchPointNum > 10;
        } else {
            return goodMatchPointNum >= 7;
        }

        //return false;
    }

    /**
     * 查找输入图片中[开]的位置
     *
     * @param bmpInput 输入的图像
     * @return 返回红包[开]字在图片中对应的位置
     * @throws CvException
     */
    public Point getOpenPackeyPos(Bitmap bmpInput) throws CvException {
        Point resultRect = null;
        if (bmpInput == null) {
            return null;
        }

        if (bmOpenLocal == null) {
            bmOpenLocal = BitmapFactory.decodeResource(LuckyMoneyTinkerApplication.getContext().getResources(),
                    R.drawable.open_pic);
            if (bmOpenLocal == null) {
                return null;
            }
        }

        Mat inputGrayMat = getGrayMat(bmpInput);
        Mat sampleGrayMat = getGrayMat(bmOpenLocal);

        //特征点提取
        ORB orb = ORB.create(1000);                           //精度越小越准确
        MatOfKeyPoint kptsInput = new MatOfKeyPoint();
        MatOfKeyPoint kptsLocal = new MatOfKeyPoint();
        orb.detect(inputGrayMat, kptsInput);
        orb.detect(sampleGrayMat, kptsLocal);

        //特征点描述,采用ORB默认的描述算法
        Mat descInput = new Mat();
        Mat descSample = new Mat();
        orb.compute(inputGrayMat, kptsInput, descInput);
        orb.compute(sampleGrayMat, kptsLocal, descSample);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matchPoints = new MatOfDMatch();
        matcher.match(descInput, descSample, matchPoints);


        float min_dist = 0;

        DMatch[] arrays = matchPoints.toArray();

        for (int i = 0; i < descInput.rows(); ++i) {
            float dist = arrays[i].distance;
            if (dist < min_dist) min_dist = dist;
        }

        //筛选特征点
        float compareNum = Math.max(min_dist * 2, 40.0f);

        List<Integer> goodQueryIdxList = new ArrayList<>();

        for (int j = 0; j < descInput.rows(); j++) {
            if (arrays[j].distance <= compareNum) {
                goodQueryIdxList.add(arrays[j].queryIdx);
            }
        }

        KeyPoint[] inputKeyPointArray = kptsInput.toArray();
        List<KeyPoint> goodKeyPointList = new LinkedList<>();
        for (int k = 0; k < goodQueryIdxList.size(); k++) {
            int n = goodQueryIdxList.get(k);
            if (n >= inputKeyPointArray.length) {
                continue;
            }
            goodKeyPointList.add(inputKeyPointArray[n]);
        }

        if (!goodKeyPointList.isEmpty()) {
            if (goodKeyPointList.size() >= 20) {
                removeWrongPoint(goodKeyPointList, (int) (goodKeyPointList.size() * 0.05));
            }
            int sumX, sumY;
            double ptX, ptY;
            sumX = sumY = 0;
            for (int i = 0; i < goodKeyPointList.size(); i++) {
                Point pt = goodKeyPointList.get(i).pt;
                sumX += pt.x;
                sumY += pt.y;
            }
            ptX = sumX / goodKeyPointList.size();
            ptY = sumY / goodKeyPointList.size();
            resultRect = new Point(ptX, ptY);
        }
        return resultRect;
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

    /**
     * 移除一些边缘的点
     *
     * @param list 特征点列表
     * @param n    移除的边缘点数
     */
    private void removeWrongPoint(List<KeyPoint> list, int n) {
        if (list.size() <= n || n == 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            double minX, maxX;
            double minY, maxY;
            int minIndexX, maxIndexX, minIndexY, maxIndexY;
            minIndexX = maxIndexX = minIndexY = maxIndexY = 0;
            minX = maxX = list.get(0).pt.x;
            minY = maxY = list.get(0).pt.y;
            for (int j = 1; j < list.size(); j++) {
                Point pt = list.get(j).pt;
                if (pt.x < minX) {
                    minX = pt.x;
                    minIndexX = j;
                }
                if (pt.x > maxX) {
                    maxX = pt.x;
                    maxIndexX = j;
                }
                if (pt.y < minY) {
                    minY = pt.y;
                    minIndexY = j;
                }
                if (pt.y > maxY) {
                    maxY = pt.y;
                    maxIndexY = j;
                }
            }
            list.remove(maxIndexY);
            if (minIndexX != maxIndexY &&
                    minIndexX < list.size()) {
                list.remove(minIndexX);
            }
            if (maxIndexX != minIndexX &&
                    maxIndexX != maxIndexY &&
                    maxIndexX < list.size()) {
                list.remove(maxIndexX);
            }
            if (minIndexY != maxIndexX &&
                    minIndexY != minIndexX &&
                    minIndexY != maxIndexY &&
                    minIndexY < list.size()) {
                list.remove(minIndexY);
            }
        }
    }

    /*private Rect getRectFromKeypointList(List<KeyPoint> list) {
        int maxSize = (int) (ScreenShotter.getInstance().getScreenHeightPublic() * 0.2);
        int n = 0;
        while (list.size() > 2) {
            //int width = (int) (list.get(list.size() - 1).pt.x - list.get(0).pt.x);
            int[] array = findKeyPointTrimIndex(list);
            int width = (int) (list.get(array[1]).pt.x - list.get(array[0]).pt.x);
            int height = (int) (list.get(list.size() - 1).pt.y - list.get(0).pt.y);
            if (width <= maxSize && height <= maxSize) {
                break;
            }
            if (width > maxSize) {
                if (array[0] != 0 && array[0] != list.size() - 1) {
                    list.remove(array[0]);
                }
                if (array[1] != 0 && array[1] != list.size() - 1) {
                    list.remove(array[1]);
                }
            }
            if (height > maxSize) {
                list.remove(0);
                list.remove(list.size() - 1);
            }
        }
        Rect rt;
        if (list.size() >= 2) {
            int[] array = findKeyPointTrimIndex(list);
            int width = (int) (list.get(array[1]).pt.x - list.get(array[0]).pt.x);
            int height = (int) (list.get(list.size() - 1).pt.y - list.get(0).pt.y);
            rt = new Rect((int) (list.get(array[0]).pt.x), (int) (list.get(0).pt.y), width, height);
        } else {
            int x = Math.max((int) (list.get(0).pt.x - maxSize / 2), 0);
            int y = Math.max((int) (list.get(0).pt.y - maxSize / 2), 0);
            rt = new Rect(x, y, maxSize, maxSize);
        }
        return rt;
    }*/

    /**
     * 查找特征点列表中分布在最左和最右的两个关键点的索引
     *
     * @param list 关键点列表
     * @return int[] 0:最左的索引 1:最右的索引
     */
    /*private int[] findKeyPointTrimIndex(List<KeyPoint> list) {
        int[] result = {0, 0};
        double minX, maxX;
        int minIndex, maxIndex;
        minX = maxX = list.get(0).pt.x;
        minIndex = maxIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            double tmp = list.get(i).pt.x;
            if (tmp < minX) {
                minX = tmp;
                minIndex = i;
            }
            if (tmp > maxX) {
                maxX = tmp;
                maxIndex = i;
            }
        }
        result[0] = minIndex;
        result[1] = maxIndex;
        return result;
    }*/
}
