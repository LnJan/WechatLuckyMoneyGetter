package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Ln_Jan on 2019/1/1.
 * 获取唤醒锁和取消屏幕锁屏
 */

public class PowerUtil {
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;
    private boolean mIsScreenLock = false;

    public PowerUtil(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "HongbaoWakelock");
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("HongbaoKeyguardLock");
    }

    private void acquire() {
        wakeLock.acquire();
        keyguardLock.disableKeyguard();
        wakeLock.release();
    }

    private void release() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        keyguardLock.reenableKeyguard();
    }

    public void handleWakeLock(boolean isWake) {
        if (mIsScreenLock) {
            if (isWake) {
                this.acquire();
            } else {
                this.release();
            }
        }
    }

    public void releasePower() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void setIsScreenLock(boolean b) {
        mIsScreenLock = b;
    }

    public boolean getIsScreenLock(){
        return mIsScreenLock;
    }
}
