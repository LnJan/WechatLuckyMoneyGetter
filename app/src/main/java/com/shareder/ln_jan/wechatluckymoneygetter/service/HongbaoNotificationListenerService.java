package com.shareder.ln_jan.wechatluckymoneygetter.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.PowerUtil;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.SoundPlayer;

import java.lang.ref.WeakReference;

/**
 * Created by Ln_Jan on 2019/1/30.
 * NotificationListenerService监听红包消息通知
 */

public class HongbaoNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    private static final int HANDLER_POSTDELAY_SCREENON = 0x01;         //锁屏广播
    private SharedPreferences mSharedPreferences;
    private PowerUtil mPowerUtil;
    private SoundPlayer mSoundPlayer = new SoundPlayer();               //提示音播放类
    private NotificationServiceHandler mHandler = new NotificationServiceHandler(this);
    private NotificationBroadcastReceiver mReceiver = new NotificationBroadcastReceiver(this);

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPowerUtil = new PowerUtil(this);
        mSoundPlayer.loadMusic(this, R.raw.redpackey_sound);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);
        Log.e(TAG, "onListenerConnected");
    }

    @Override
    public void onListenerDisconnected() {
        mPowerUtil.releasePower();
        mSoundPlayer.releaseMusic();
        unregisterReceiver(mReceiver);
        Log.e(TAG, "onListenerDisconnected");
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        //Log.e(TAG, sbn.getPackageName());

        // 如果该通知的包名不是微信，那么 pass 掉
        if (!"com.tencent.mm".equals(sbn.getPackageName())) {
            return;
        }

        if (!mSharedPreferences.getBoolean("HongBaoServiceEnable", false) ||
                !mSharedPreferences.getBoolean("pref_watch_notification", true)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        Bundle extras = notification.extras;
        if (extras != null) {
            // 获取通知标题
            //String title = extras.getString(Notification.EXTRA_TITLE, "");
            //Log.e(TAG, "title:" + title);
            // 获取通知内容
            String content = extras.getString(Notification.EXTRA_TEXT, "");
            //Log.e(TAG, "content:" + content);
            if (!TextUtils.isEmpty(content) && content.contains(WECHAT_NOTIFICATION_TIP)) {
                if (mSharedPreferences.getBoolean("pref_watch_on_lock", false)) {
                    mPowerUtil.handleWakeLock(true);
                } else {
                    if (mSharedPreferences.getBoolean("pref_tips_sound", true) &&
                            mPowerUtil.getIsScreenLock()) {
                        mSoundPlayer.playMusic();
                    }
                }
                PendingIntent pendingIntent = notification.contentIntent;
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    static class NotificationServiceHandler extends Handler {
        private WeakReference<HongbaoNotificationListenerService> mRef;

        NotificationServiceHandler(HongbaoNotificationListenerService service) {
            mRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_POSTDELAY_SCREENON:
                    mRef.get().mPowerUtil.setIsScreenLock(false);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    static class NotificationBroadcastReceiver extends BroadcastReceiver {
        private WeakReference<HongbaoNotificationListenerService> mRef;

        NotificationBroadcastReceiver(HongbaoNotificationListenerService service) {
            mRef = new WeakReference<>(service);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    mRef.get().mPowerUtil.setIsScreenLock(true);
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    //mRef.get().mPowerUtil.setIsScreenLock(false);
                    //这里需要延时设置变量，因为在MIUI中只要亮屏就会发送这个广播，影响PowerUtil类的判断
                    mRef.get().mHandler.sendEmptyMessageDelayed(HANDLER_POSTDELAY_SCREENON, 500);
                }
            }
        }
    }

}
