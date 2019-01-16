package com.shareder.ln_jan.wechatluckymoneygetter.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.activities.SeekBarPreference;
import com.shareder.ln_jan.wechatluckymoneygetter.global.MyApplication;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.FeatureDetectionManager;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.PowerUtil;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.ScreenShotter;

import org.opencv.core.CvException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ln_Jan on 2018/11/14.
 * 抢红包AccessibilityService服务
 */

public class HongbaoService extends AccessibilityService {
    private static final String TAG = "HongbaoService";
    /**
     * 微信的包名
     */
    private static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    private static final String CHATTING_LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI";
    private static final String LUCKY_MONEY_RECV_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String LUCKY_MONEY_RECV_UI_700 = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";         //微信7.0.0拆红包页面
    private static final String LUCKY_MONEY_DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private static final String WECHAT_VIEWPAGER_LAYOUT = "com.tencent.mm.ui.mogic.WxViewPager";
    private static final String LIST_VIEW_NAME = "android.widget.ListView";
    private static final String LINEARLAYOUT_NAME = "android.widget.LinearLayout";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    private static final String WECHAT_PACKEY_TIP = "微信红包";
    private static final String WECHAT_DISCOVER_TIP = "发现";
    private static final String WECHAT_COMMUNICATE_TIP = "通讯录";
    /**
     * 微信6.7.3版本的版本号
     */
    private static final int WX_673_VERCODE = 1360;
    /**
     * 微信7.0.0版本的版本号
     */
    private static final int WX_700_VERCODE = 1380;
    private static final int HANDLER_CLOSE_PACKEY = 0x01;
    private static final int HANDLER_POSTDELAY_OPEN = 0x02;           //延时打开红包
    private String currentActivityName = CHATTING_LAUNCHER_UI;
    private String currentNodeInfoName = "";
    //private String prevActivityName = CHATTING_LAUNCHER_UI;
    private boolean mGlobalMutex = false;
    private boolean mPockeyOpenMutex = false;
    private SharedPreferences mSharedPreferences;
    private HongbaoServiceHandler mHandler = new HongbaoServiceHandler(this);
    private PowerUtil mPowerUtil = null;
    private LockScreenReceiver mReceiver = null;
    private List<String> mSelfOpenList = null;
    private int mPackeyTag = 0x00;
    private int mWechatVersion = 0x00;                                  //微信版本

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        //Log.e(TAG, "event recv");
        Log.e(TAG, "class:" + accessibilityEvent.getClassName().toString());
        if (!mGlobalMutex) {
            mGlobalMutex = true;
            setCurrentActivityName(accessibilityEvent);
            switch (accessibilityEvent.getEventType()) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    if (mSharedPreferences.getBoolean("pref_watch_notification", false)) {
                        handleNotificationMessage(accessibilityEvent);
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    String tip = accessibilityEvent.getText().toString();
                    handleScreenMessage(accessibilityEvent);
                    break;
                default:
                    break;
            }
            mGlobalMutex = false;
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        this.mPowerUtil.releasePower();
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.mPowerUtil = new PowerUtil(this);
        this.mReceiver = new LockScreenReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(this.mReceiver, intentFilter);
        mSelfOpenList = new ArrayList<>(20);
        mWechatVersion = getWechatVersion();
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        String activitiesName = event.getClassName().toString();
        currentNodeInfoName = activitiesName;
        if (activitiesName.startsWith("com.tencent.mm")) {
            //prevActivityName = currentActivityName;
            currentActivityName = activitiesName;
            Log.e(TAG, "current_name:" + event.getClassName().toString());
        }
    }

    private void handleScreenMessage(AccessibilityEvent ev) {
        //setCurrentActivityName(ev);
        if (CHATTING_LAUNCHER_UI.equals(currentActivityName)) {                               //聊天列表和聊天页面
            if (!isInChartList()) {
                //Log.e(TAG, "In Chart");
                if (mWechatVersion != WX_700_VERCODE) {
                    findRedpockeyAndClick(ev);
                } else {
                    findRedpockeyAndClick_700();
                }
                //Log.e(TAG,"find packey");
            } else {
                //Log.e(TAG, "In ChartList");
                if (mSharedPreferences.getBoolean("pref_watch_list", false)) {
                    Rect selRect = checkNewsImageIsLuckyMoney();
                    if (selRect != null) {
                        clickMiddleInRect(selRect);
                    }
                }
            }
        } else if (LUCKY_MONEY_RECV_UI.equals(currentActivityName) ||
                LUCKY_MONEY_RECV_UI_700.equals(currentActivityName)) {                          //拆红包页面
            /*AccessibilityNodeInfo info = findOpenButton(ev.getSource());
            if (info == null) {
                Log.e(TAG, "Recv_ui:AccessibilityNodeInfo null");
            }*/
            if (mSharedPreferences.getBoolean("pref_watch_chat", false)) {
                if (mPackeyTag != 0x01) {
                    return;
                }
                mPackeyTag = 0x02;
                int delay = mSharedPreferences.getInt(SeekBarPreference.PREFERENCE_TAG, 0);
                if (delay == 0x00) {
                    openPacket();
                } else {
                    mHandler.sendEmptyMessageDelayed(HANDLER_POSTDELAY_OPEN, delay);
                }
                mHandler.sendEmptyMessageDelayed(HANDLER_CLOSE_PACKEY, delay + 1000);
            }
            //Log.e(TAG,"open packey");
        } else if (LUCKY_MONEY_DETAIL_UI.equals(currentActivityName)) {                        //红包详情页面
            if (currentActivityName.equals(currentNodeInfoName)) {
                Log.e(TAG, "detail UI");
                mPackeyTag = 0x00;
                performGlobalAction(GLOBAL_ACTION_BACK);
                //Log.e(TAG,"back");
            }
        }
    }


    private void handleNotificationMessage(AccessibilityEvent event) {
        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return;

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                if (mSharedPreferences.getBoolean("pref_watch_on_lock", false)) {
                    mPowerUtil.handleWakeLock(true);
                }
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否在聊天列表
     *
     * @return boolean
     */
    private boolean isInChartList() {
        boolean b = false;
        /*AccessibilityNodeInfo nodeInfo = findNodeInfoByClass(getRootInActiveWindow(), WECHAT_VIEWPAGER_LAYOUT);
        if (nodeInfo == null) {
            //Log.e(TAG,WECHAT_VIEWPAGER_LAYOUT+"not find");
            return false;
        }
        b = nodeInfo.isFocusable();*/
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            if (root.getChildCount() > 0) {
                AccessibilityNodeInfo subInfo = root.getChild(0);
                if (subInfo != null) {
                    b = subInfo.getChildCount() > 1;
                }
            }
        }
        return b;
    }

    private AccessibilityNodeInfo findNodeInfoByClass(AccessibilityNodeInfo nodeInfo, String className) {
        if (nodeInfo == null) {
            return null;
        }
        if (nodeInfo.getClassName().toString().equals(className)) {
            return nodeInfo;
        }
        if (nodeInfo.getChildCount() > 0) {
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo tmpInfo = findNodeInfoByClass(nodeInfo.getChild(i), className);
                if (tmpInfo != null) {
                    return tmpInfo;
                }
            }
        }
        return null;
    }


    private Rect checkNewsImageIsLuckyMoney() {
        List<Rect> newsList = findNewsRectInScreen();
        Rect resultRect = null;
        if (!newsList.isEmpty()) {
            Bitmap bmScreenShot;
            try {
                bmScreenShot = ScreenShotter.getInstance().getScreenShotSync();
            }catch (Exception e){
                e.printStackTrace();
                bmScreenShot=null;
            }
            if (bmScreenShot == null) {
                return null;
            }
            for (int i = 0; i < newsList.size(); i++) {
                Rect picRect = newsList.get(i);
                if (picRect.top >= bmScreenShot.getHeight() || picRect.left >= bmScreenShot.getWidth()) {
                    continue;
                }
                int x = picRect.left > 0 ? picRect.left : 0;
                int y = picRect.top > 0 ? picRect.top : 0;
                int width = picRect.width() > (bmScreenShot.getWidth() - x) ? (bmScreenShot.getWidth() - x) : picRect.width();
                int height = picRect.height() > (bmScreenShot.getHeight() - y) ? (bmScreenShot.getHeight() - y) : picRect.height();
                if (width <= 0 || height <= 0) {
                    continue;
                }
                Bitmap bmSub = Bitmap.createBitmap(bmScreenShot, x, y, width, height);
                SaveBitmapToLocal(bmSub);
                boolean b = false;
                try {
                    b = FeatureDetectionManager.getInstance().isPictureMatchLuckyMoney(bmSub, ScreenShotter.getInstance().isNormalScreen());
                } catch (CvException ex) {
                    b = false;
                }
                bmSub.recycle();
                if (b) {
                    resultRect = picRect;
                    break;
                }
            }
            bmScreenShot.recycle();
        }
        return resultRect;
    }

    private void SaveBitmapToLocal(Bitmap bmp) {
        String strSavePath = getCacheDir().getAbsolutePath() + File.separator + java.util.UUID.randomUUID().toString() + ".jpg";
        try {
            File f = new File(strSavePath);
            if (f.createNewFile()) {
                FileOutputStream outStream = new FileOutputStream(f);
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                outStream.flush();
                outStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Rect> findNewsRectInScreen() {
        AccessibilityNodeInfo nodeInfo = findNodeInfoByClass(getRootInActiveWindow(), LIST_VIEW_NAME);
        List<Rect> resultList = new ArrayList<>();
        if (nodeInfo != null) {
            int chartCount = nodeInfo.getChildCount();
            for (int i = 0; i < chartCount; i++) {
                AccessibilityNodeInfo subChartInfo = nodeInfo.getChild(i);
                if (subChartInfo != null) {
                    if (subChartInfo.getChildCount() > 0) {                                             //表示是未读消息，有可能有红包
                        Rect outputRect = new Rect();
                        subChartInfo.getBoundsInScreen(outputRect);
                        if (!ScreenShotter.getInstance().isNormalScreen()) {
                            outputRect.top -= 20;
                            outputRect.bottom -= 20;
                        }
                        if (outputRect.height() == 0 || outputRect.width() == 0) {
                            continue;
                        }
                        outputRect.left += (int) (outputRect.width() * 0.2);                                 //去除头像区域
                        outputRect.top += (int) (outputRect.height() * 0.3);
                        resultList.add(outputRect);
                    }
                }
            }
        }
        return resultList;
    }


    private List<AccessibilityNodeInfo> getPacketNode(AccessibilityNodeInfo rootInfo, String... texts) {
        List<AccessibilityNodeInfo> resultLst = new ArrayList<>();
        if (rootInfo == null) {
            return resultLst;
        }
        for (String text : texts) {
            if (text == null) continue;
            List<AccessibilityNodeInfo> nodes = rootInfo.findAccessibilityNodeInfosByText(text);
            //过滤已拆开的自己的红包
            if (text.equals(WECHAT_VIEW_SELF_CH)) {
                for (AccessibilityNodeInfo info : nodes) {
                    String strHash = getHongbaoHash(info);
                    if (strHash != null) {
                        if (!mSelfOpenList.contains(strHash)) {
                            mSelfOpenList.add(strHash);
                            resultLst.add(info);
                            if (mSelfOpenList.size() > 50) {
                                mSelfOpenList.clear();
                            }
                        }
                    }
                }
            } else {
                if (nodes != null && !nodes.isEmpty()) {
                    resultLst.addAll(nodes);
                }
            }
        }

        Collections.sort(resultLst, new Comparator<AccessibilityNodeInfo>() {
            @Override
            public int compare(AccessibilityNodeInfo nodeInfo, AccessibilityNodeInfo t1) {
                Rect bounds1 = new Rect();
                Rect bounds2 = new Rect();
                nodeInfo.getBoundsInScreen(bounds1);
                t1.getBoundsInScreen(bounds2);
                return bounds1.bottom - bounds2.bottom;
            }
        });
        return resultLst;
    }

    /**
     * 微信7.0.0版本没有了[领取红包]的文字信息，所以用这个方法来查找红包
     */
    private void findRedpockeyAndClick_700() {
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> redPackeyTextLst = null;
        redPackeyTextLst = getPacketNode(rootInfo, WECHAT_PACKEY_TIP);
        if (redPackeyTextLst != null && !redPackeyTextLst.isEmpty()) {
            List<AccessibilityNodeInfo> recvList = new ArrayList<>();
            List<AccessibilityNodeInfo> ownList = new ArrayList<>();
            String str_filter = mSharedPreferences.getString("pref_watch_exclude_words", "");
            String[] str_fiilter_array = "".equals(str_filter) ? null : str_filter.split(" +");
            for (AccessibilityNodeInfo tmpInfo : redPackeyTextLst) {
                AccessibilityNodeInfo parentLayoutInfo = tmpInfo.getParent();
                if (parentLayoutInfo != null &&
                        parentLayoutInfo.getClassName().toString().equals(LINEARLAYOUT_NAME)) {
                    if (parentLayoutInfo.getChildCount() == 0x03) {                     //已领或领取完的红包getChildCount为4
                        Rect rt = new Rect();
                        parentLayoutInfo.getBoundsInScreen(rt);
                        int right = rt.right;
                        if (right > ScreenShotter.getInstance().getScreenWidth() * 0.8) {
                            if (mSharedPreferences.getBoolean("pref_watch_self", false)) {
                                String strHash = getHongbaoHash(tmpInfo);
                                if (strHash != null) {
                                    if (!mSelfOpenList.contains(strHash)) {
                                        mSelfOpenList.add(strHash);
                                        ownList.add(tmpInfo);
                                        if (mSelfOpenList.size() > 50) {
                                            mSelfOpenList.clear();
                                        }
                                    }
                                }
                            }
                        } else {
                            if (str_fiilter_array != null) {
                                boolean b = false;
                                try {
                                    String str_packey_detail = parentLayoutInfo.getChild(0).getText().toString();
                                    for (String str : str_fiilter_array) {
                                        if (str_packey_detail.contains(str)) {
                                            b = true;
                                            break;
                                        }
                                    }
                                } catch (NullPointerException e) {
                                    continue;
                                }
                                if (!b) {
                                    recvList.add(tmpInfo);
                                }
                            } else {
                                recvList.add(tmpInfo);
                            }
                        }
                    }
                }
            }
            List<AccessibilityNodeInfo> totalList = new ArrayList<>(recvList.size() + ownList.size());
            totalList.addAll(recvList);
            if (!ownList.isEmpty()) {
                totalList.addAll(ownList);
            }
            if (totalList.isEmpty()) {
                return;
            }
            if (totalList.size() > 1) {
                Collections.sort(totalList, new Comparator<AccessibilityNodeInfo>() {
                    @Override
                    public int compare(AccessibilityNodeInfo nodeInfo, AccessibilityNodeInfo t1) {
                        Rect bounds1 = new Rect();
                        Rect bounds2 = new Rect();
                        nodeInfo.getBoundsInScreen(bounds1);
                        t1.getBoundsInScreen(bounds2);
                        return bounds1.bottom - bounds2.bottom;
                    }
                });
            }
            AccessibilityNodeInfo clickInfo = totalList.get(totalList.size() - 1);
            if (clickInfo != null) {
                AccessibilityNodeInfo clickLayoutInfo = clickInfo.getParent();
                if (clickLayoutInfo != null) {
                    if (mSharedPreferences.getBoolean("pref_watch_chat", false)) {
                        if (mPackeyTag != 0x00) {
                            return;
                        }
                        mPackeyTag = 0x01;
                    }
                    clickLayoutInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    private void findRedpockeyAndClick(AccessibilityEvent ev) {
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        //检查领取红包和查看红包
        List<AccessibilityNodeInfo> redPackeyInfoLst = null;
        if (mSharedPreferences.getBoolean("pref_watch_self", false)) {
            redPackeyInfoLst = getPacketNode(rootInfo, WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH);
        } else {
            redPackeyInfoLst = getPacketNode(rootInfo, WECHAT_VIEW_OTHERS_CH);
        }
        if (redPackeyInfoLst != null && !redPackeyInfoLst.isEmpty()) {
            //redPackeyInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            String str_filter = mSharedPreferences.getString("pref_watch_exclude_words", "");
            AccessibilityNodeInfo openPackeyInfo = null;
            if (str_filter.equals("")) {
                openPackeyInfo = redPackeyInfoLst.get(redPackeyInfoLst.size() - 1);
            } else {
                String[] str_fiilter_array = str_filter.split(" +");
                for (int k = redPackeyInfoLst.size() - 1; k >= 0; k--) {
                    AccessibilityNodeInfo tmpInfo = redPackeyInfoLst.get(k);
                    if (tmpInfo.getParent().getChildCount() <= 0) {
                        continue;
                    }

                    String strPackeyMsg = tmpInfo.getParent().getChild(0).getText().toString();
                    boolean b = false;
                    for (String filter_text : str_fiilter_array) {
                        if ((filter_text.length() > 0) &&
                                strPackeyMsg.contains(filter_text)) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        openPackeyInfo = tmpInfo;
                        break;
                    }
                }
            }
            if (openPackeyInfo != null) {
                AccessibilityNodeInfo parentInfo = openPackeyInfo.getParent();
                if (parentInfo != null) {
                    if (mSharedPreferences.getBoolean("pref_watch_chat", false)) {
                        if (mPackeyTag != 0x00) {
                            return;
                        }
                        mPackeyTag = 0x01;
                    }
                    parentInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
            Log.e(TAG, "pockey find!");
        }
    }

    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }

        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
    }

    private void openPacket() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpi = metrics.densityDpi;
        Log.e(TAG, "openPacket！" + dpi);
        if (android.os.Build.VERSION.SDK_INT <= 23) {
            //nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Toast.makeText(MyApplication.getContext(), getString(R.string.not_support_low_level), Toast.LENGTH_SHORT).show();
        } else {
            if (android.os.Build.VERSION.SDK_INT > 23) {
                if (!mPockeyOpenMutex) {
                    mPockeyOpenMutex = true;
                    Path path = new Path();
                    int x = 0, y = 0;
                    if (640 == dpi) { //1440
                        //path.moveTo(720, 1575);
                        x = 720;
                        y = 1575;
                    } else if (320 == dpi) {//720p
                        //path.moveTo(355, 780);
                        x = 355;
                        y = 780;
                    } else if (480 == dpi) {//1080p
                        //path.moveTo(533, 1115);
                        x = 533;
                        y = 1115;
                    } else if (440 == dpi) {//1080*2160
                        //path.moveTo(450, 1250);
                        x = 450;
                        y = 1250;
                    }
                    if (mWechatVersion == WX_700_VERCODE) {
                        y += (y * 0.15);
                    }
                    path.moveTo(x, y);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    try {
                        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 450, 50)).build();
                        dispatchGesture(gestureDescription, new GestureResultCallback() {
                            @Override
                            public void onCompleted(GestureDescription gestureDescription) {
                                Log.e(TAG, "onCompleted");
                                mPockeyOpenMutex = false;
                                mPackeyTag = 0x03;
                                super.onCompleted(gestureDescription);
                            }

                            @Override
                            public void onCancelled(GestureDescription gestureDescription) {
                                Log.e(TAG, "onCancelled");
                                mPockeyOpenMutex = false;
                                mPackeyTag = 0x03;
                                super.onCancelled(gestureDescription);
                            }
                        }, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void clickMiddleInRect(Rect rect) {
        if (android.os.Build.VERSION.SDK_INT > 23) {
            if (!mPockeyOpenMutex) {
                mPockeyOpenMutex = true;
                Path path = new Path();
                path.moveTo(rect.left + rect.width() / 2, rect.top + rect.height() / 2);
                GestureDescription.Builder builder = new GestureDescription.Builder();
                try {
                    GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 450, 50)).build();
                    dispatchGesture(gestureDescription, new GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            mPockeyOpenMutex = false;
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            mPockeyOpenMutex = false;
                        }
                    }, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取节点对象唯一的id，通过正则表达式匹配
     * AccessibilityNodeInfo@后的十六进制数字
     *
     * @param node AccessibilityNodeInfo对象
     * @return id字符串
     */
    private static String getNodeId(AccessibilityNodeInfo node) {
        /* 用正则表达式匹配节点Object */
        Pattern objHashPattern = Pattern.compile("(?<=@)[0-9|a-z]+(?=;)");
        Matcher objHashMatcher = objHashPattern.matcher(node.toString());

        // AccessibilityNodeInfo必然有且只有一次匹配，因此不再作判断
        objHashMatcher.find();

        return objHashMatcher.group(0);
    }

    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */
    private static String getHongbaoHash(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npr) {
            return null;
        }

        return content + "@" + getNodeId(node);
    }

    /**
     * 获取微信版本信息
     *
     * @return 微信版本代号
     */
    private int getWechatVersion() {
        PackageInfo wechatPackageInfo;
        try {
            wechatPackageInfo = MyApplication.getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            wechatPackageInfo = null;
        }
        if (wechatPackageInfo == null) {
            return 0x00;
        }
        return wechatPackageInfo.versionCode;
    }


    static class HongbaoServiceHandler extends Handler {
        private WeakReference<HongbaoService> mRef;

        HongbaoServiceHandler(HongbaoService service) {
            mRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_CLOSE_PACKEY:
                    if (mRef.get().mPackeyTag == 0x03) {
                        mRef.get().mPackeyTag = 0x00;
                        mRef.get().performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                    break;
                case HANDLER_POSTDELAY_OPEN:
                    mRef.get().openPacket();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    static class LockScreenReceiver extends BroadcastReceiver {
        private WeakReference<HongbaoService> mRef;

        LockScreenReceiver(HongbaoService s) {
            mRef = new WeakReference<>(s);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    mRef.get().mPowerUtil.setIsScreenLock(true);
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    mRef.get().mPowerUtil.setIsScreenLock(false);
                }
            }
        }
    }

}
