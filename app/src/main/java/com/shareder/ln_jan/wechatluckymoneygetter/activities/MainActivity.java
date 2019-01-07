package com.shareder.ln_jan.wechatluckymoneygetter.activities;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.fragments.SettingPreferenceFragment;
import com.shareder.ln_jan.wechatluckymoneygetter.global.MyTransparentDialog;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.FeatureDetectionManager;
import com.tencent.bugly.Bugly;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

public class MainActivity extends AppCompatActivity implements AccessibilityManager.AccessibilityStateChangeListener,
        View.OnClickListener {
    private static final String TAG = "MainActivity";
    private AccessibilityManager accessibilityManager;
    private Switch service_switch;
    private static final String SHOWDIALOG_TAG = "NotShowPowerDialog";
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

        service_switch = findViewById(R.id.service_open_switch);
        service_switch.setOnClickListener(this);

        handleMIUIStatusBar();
        loadFragmentActivity();
        //requestScreenShot();

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

        //初始化bugly
        Bugly.init(getApplicationContext(), "2212e773ac", true);
    }

    private String getDirFileNameByIndex(String strDir, int index) {
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
    }

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
        fragmentTransaction.replace(R.id.preferences_fragment, new SettingPreferenceFragment());
        fragmentTransaction.commit();
    }

    /**
     * 更新当前 HongbaoService 显示状态
     */
    private void updateServiceStatus() {
        if (isServiceEnabled()) {
            service_switch.setChecked(true);
        } else {
            service_switch.setChecked(false);
        }
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
            dialog.show();
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
            List<PermissionItem> permissonItems = new ArrayList<>();
            permissonItems.add(new PermissionItem(Manifest.permission.READ_PHONE_STATE, "读取手机状态", R.drawable.permission_ic_phone));
            permissonItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE, "读写外部存储", R.drawable.permission_ic_storage));
            HiPermission.create(this).title(getString(R.string.authority_request))
                    .permissions(permissonItems)
                    .msg(getString(R.string.authority_info))
                    .animStyle(R.style.PermissionAnimScale)
                    .style(R.style.PermissionDefaultBlueStyle)
                    .checkMutiPermission(new PermissionCallback() {
                        @Override
                        public void onClose() {
                            openPowerStageyDialog();
                        }

                        @Override
                        public void onFinish() {
                            openPowerStageyDialog();
                        }

                        @Override
                        public void onDeny(String permission, int position) {

                        }

                        @Override
                        public void onGuarantee(String permission, int position) {

                        }
                    });
        }
    }
}
