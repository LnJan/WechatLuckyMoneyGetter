package com.shareder.ln_jan.wechatluckymoneygetter.global;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Created by Ln_Jan on 2018/12/15.
 * 获取全局context
 */

public class MyApplication extends Application {
    private static WeakReference<Context> mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext=new WeakReference<>(getApplicationContext());
    }

    public static Context getContext(){
        return mContext.get();
    }
}
