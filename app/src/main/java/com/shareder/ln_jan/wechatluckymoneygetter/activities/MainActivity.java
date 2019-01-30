package com.shareder.ln_jan.wechatluckymoneygetter.activities;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.fragments.SettingPreferenceFragment;
import com.shareder.ln_jan.wechatluckymoneygetter.global.AuthorDetailDialog;
import com.shareder.ln_jan.wechatluckymoneygetter.global.MyTransparentDialog;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.FeatureDetectionManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AccessibilityManager.AccessibilityStateChangeListener,
        View.OnClickListener {
    private static final String TAG = "MainActivity";
    private AccessibilityManager accessibilityManager;
    private Switch service_switch;
    private MainActivityHandler mHandler = new MainActivityHandler(this);
    private SettingPreferenceFragment mFragment = new SettingPreferenceFragment();
    private SharedPreferences mSharedPreferences;
    private static final String SHOWDIALOG_TAG = "NotShowPowerDialog";
    private static final int HANDLER_REQUEST_AUTHOR = 0x01;
    private static final int HANDLER_SHOW_POWER_DIALOG = 0x02;
    private static final int REQUEST_AUTHOR_ID = 0xff;
    //private static final String mChannel1 = "system";
    //private static final int REQUEST_MEDIA_PROJECTION = 0x01;
    //private int mIndex = 0;
    //private ImageView mImageView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    Log.e(TAG, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.e(TAG, "加载失败");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //CreateNotificationChannel();
        setContentView(R.layout.activity_main);

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            accessibilityManager.addAccessibilityStateChangeListener(this);
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        service_switch = findViewById(R.id.service_open_switch);
        service_switch.setOnClickListener(this);

        handleMIUIStatusBar();
        loadFragmentActivity();

        FeatureDetectionManager.getInstance().createLuckyMoneyPicture();

        checkAuthority();

        /*mImageView = findViewById(R.id.imv_show_cache);
        mImageView.setImageBitmap(BitmapFactory.decodeFile(FeatureDetectionManager.getInstance().getMoneyPicPath()));
        Button btnNext = findViewById(R.id.btn_next_pic);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = getDirFileNameByIndex(getCacheDir().getAbsolutePath(), mIndex);
                if(str==null){
                    Toast.makeText(MainActivity.this,"没有图片了",Toast.LENGTH_SHORT).show();
                }else{
                    mImageView.setImageBitmap(BitmapFactory.decodeFile(str));
                    mIndex++;
                }
            }
        });*/
    }

    /*private String getDirFileNameByIndex(String strDir, int index) {
        File f = new File(strDir);
        if (!f.exists()) {
            return null;
        }
        File[] files = f.listFiles();
        if (files == null) {
            return null;
        }
        if (index >= files.length) {
            return null;
        }
        return files[index].getAbsolutePath();
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        //加载opencv
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.e(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //mIndex = 0;
    }

    @Override
    protected void onDestroy() {
        accessibilityManager.removeAccessibilityStateChangeListener(this);
        Log.e("MainActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onAccessibilityStateChanged(boolean b) {
        updateServiceStatus();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.service_open_switch:
                openAccessibility();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_AUTHOR_ID) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        builder.append("读写外存权限被拒绝！！\n");
                    } else if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE)) {
                        builder.append("读取手机状态权限被拒绝！！\n");
                    }
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
                Toast.makeText(this, builder.toString(), Toast.LENGTH_SHORT).show();
            }
            mHandler.sendEmptyMessage(HANDLER_SHOW_POWER_DIALOG);
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /*public static String getChannelName() {
        return mChannel1;
    }*/

    private void openAccessibility() {
        try {
            if (service_switch.isChecked()) {
                Toast.makeText(this, getString(R.string.turn_on_toast), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.turn_off_toast), Toast.LENGTH_SHORT).show();
            }
            Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibleIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.turn_on_error_toast), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    /*private void CreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName1 = "系统消息";
            NotificationChannel channel1 = new NotificationChannel(mChannel1, channelName1, NotificationManager.IMPORTANCE_HIGH);
            channel1.enableVibration(false);
            channel1.setVibrationPattern(new long[]{0});
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel1);
            }
        }
    }*/

    private void handleMIUIStatusBar() {
        Window window = getWindow();

        Class clazz = window.getClass();
        try {
            int tranceFlag = 0;
            Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");

            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_TRANSPARENT");
            tranceFlag = field.getInt(layoutParams);

            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, tranceFlag, tranceFlag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFragmentActivity() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.preferences_fragment, mFragment);
        fragmentTransaction.commit();
    }

    /**
     * 更新当前 HongbaoService 显示状态
     */
    private void updateServiceStatus() {
        SharedPreferences.Editor editor=mSharedPreferences.edit();
        if (isServiceEnabled()) {
            service_switch.setChecked(true);
            editor.putBoolean("HongBaoServiceEnable",true);
        } else {
            service_switch.setChecked(false);
            editor.putBoolean("HongBaoServiceEnable",false);
        }
        editor.apply();
    }

    /**
     * 获取 HongbaoService 是否启用状态
     *
     * @return
     */
    private boolean isServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.service.HongbaoService")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 弹出对话框申请电源限制权限
     */
    private void openPowerStageyDialog() {
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        if (!preferences.getBoolean(SHOWDIALOG_TAG, false)) {
            final MyTransparentDialog dialog = MyTransparentDialog.createTransparentDialog(this).setTitle(getString(R.string.background_authority_request))
                    .setContent(getString(R.string.background_authority_info));
            dialog.setNotshowVisible(true);
            dialog.setMyDialogOnClickListener(new MyTransparentDialog.MyDialogOnClickListener() {
                @Override
                public void onClick(boolean isChecked) {
                    if (isChecked) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(SHOWDIALOG_TAG, isChecked);
                        editor.apply();
                    }
                    openPowerStagey();
                    dialog.dismiss();
                }
            });
            if (!isFinishing()) {
                dialog.show();
            }
        }
    }


    private void openPowerStagey() {
        //Intent intent = new Intent();

        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        intent.setData(Uri.fromParts("package", getPackageName(), null));

        startActivity(intent);

        Toast.makeText(this, getString(R.string.background_authority_tips), Toast.LENGTH_LONG).show();
    }

    /**
     * 检查应用权限，使用bugly需要申请一些必要的权限
     */
    private void checkAuthority() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> authorList = new ArrayList<>(2);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                authorList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                authorList.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (!authorList.isEmpty()) {
                if (isFinishing()) {
                    return;
                }
                AuthorDetailDialog dialog = new AuthorDetailDialog(this, R.style.Transparent);
                final ArrayList<String> sendList = (ArrayList<String>) authorList;
                dialog.setOnNextstepClickListener(new AuthorDetailDialog.OnNextstepClickListener() {
                    @Override
                    public void onClick() {
                        Message msg = new Message();
                        msg.what = HANDLER_REQUEST_AUTHOR;
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("authors", sendList);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                });
                dialog.show();
            } else {
                openPowerStageyDialog();
            }
        } else {
            openPowerStageyDialog();
        }
    }

    static class MainActivityHandler extends Handler {
        private WeakReference<MainActivity> mRefParent;

        MainActivityHandler(MainActivity activity) {
            mRefParent = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_REQUEST_AUTHOR:
                    requestAuthor(msg.getData().getStringArrayList("authors"));
                    break;
                case HANDLER_SHOW_POWER_DIALOG:
                    mRefParent.get().openPowerStageyDialog();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private void requestAuthor(ArrayList<String> lst) {
            if (lst.isEmpty()) {
                return;
            }
            String[] array = new String[lst.size()];
            for (int i = 0; i < lst.size(); i++) {
                array[i] = lst.get(i);
            }
            ActivityCompat.requestPermissions(mRefParent.get(), array, REQUEST_AUTHOR_ID);
        }
    }
}
