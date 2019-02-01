package com.shareder.ln_jan.wechatluckymoneygetter.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.global.MyTransparentDialog;
import com.shareder.ln_jan.wechatluckymoneygetter.service.HongbaoNotificationListenerService;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.ScreenShotter;
import com.tencent.bugly.beta.Beta;

import java.util.Set;

/**
 * Created by Ln_Jan on 2018/11/8.
 * 功能设置Fragment
 */

public class SettingPreferenceFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String SCREENSHORT_TIPS = "由于技术原因目前无法直接获取聊天列表中的文字信息，" +
            "目前只能通过截屏的方式判断列表中是否包含红包信息";
    private static final String MY_GITHUB_ISSUES_URL = "https://github.com/LnJan/WechatLuckyMoneyGetter/issues";

    private static final int REQUEST_MEDIA_PROJECTION = 0x01;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preference);
        initPrefListeners();
        if (isNotificationListenerEnabled(this.getActivity())) {
            toggleNotificationListenerService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setWatchNotificationPref(isNotificationListenerEnabled(this.getActivity()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        boolean b = false;
        if (preference.getKey().equals("pref_watch_chat")) {
            b = handleWatchChart();
        } else if (preference.getKey().equals("pref_watch_self")) {
            b = handleWatchSelf(preference);
        } else if (preference.getKey().equals("pref_watch_list")) {
            b = handleWatchList(preference);
        } else if (preference.getKey().equals("pref_watch_exclude_words")) {
            b = handleExcludeWords(preference, o);
        } else if (preference.getKey().equals("pref_watch_notification")) {
            b = handleWatchNotification();
        }
        return b;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("pref_etc_check_update")) {
            Beta.checkUpgrade(true, false);
        } else if (preference.getKey().equals("pref_etc_issue")) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MY_GITHUB_ISSUES_URL));
            startActivity(intent);
        } else if (preference.getKey().equals("pref_etc_shared")) {
            clipDownloadLink();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION: {
                CheckBoxPreference preference = (CheckBoxPreference) findPreference("pref_watch_list");
                if (resultCode == -1 && data != null) {
                    ScreenShotter.getInstance()
                            .setMediaProjection(getMediaProjectionManager()
                                    .getMediaProjection(Activity.RESULT_OK, data));
                    if (preference != null) {
                        preference.setChecked(true);
                    }
                } else {
                    if (preference != null) {
                        preference.setChecked(false);
                    }
                }
            }
        }
    }

    private void setWatchNotificationPref(boolean b) {
        CheckBoxPreference preference = (CheckBoxPreference) findPreference("pref_watch_notification");
        if (preference != null) {
            preference.setChecked(b);
        }
    }

    private boolean handleWatchChart() {
        if (isAutoClickPermit()) {
            return true;
        } else {
            Toast.makeText(this.getActivity(), getString(R.string.not_support_low_level), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean handleWatchNotification() {
        if (!isNotificationListenerEnabled(this.getActivity())) {
            Toast.makeText(this.getActivity(), getString(R.string.notification_tips), Toast.LENGTH_LONG).show();
            openNotificationListenSettings();
        }
        return true;
    }

    private boolean handleWatchSelf(Preference preference) {
        CheckBoxPreference preference1 = (CheckBoxPreference) findPreference("pref_watch_chat");
        if (preference1 != null) {
            if (preference instanceof CheckBoxPreference) {
                if (!((CheckBoxPreference) preference).isChecked()) {
                    if (!preference1.isChecked()) {
                        Toast.makeText(this.getActivity(), "请先开启自动拆红包选项", Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleWatchList(Preference preference) {
        if (isAutoClickPermit()) {
            if (preference instanceof CheckBoxPreference) {
                if (!((CheckBoxPreference) preference).isChecked()) {
                    final MyTransparentDialog dialog = MyTransparentDialog.createTransparentDialog(this.getContext()).setTitle(getString(R.string.app_name))
                            .setContent(SCREENSHORT_TIPS);
                    dialog.setMyDialogOnClickListener(new MyTransparentDialog.MyDialogOnClickListener() {
                        @Override
                        public void onClick(boolean isChecked) {
                            requestScreenShot();
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else {
                    return true;
                }
            }
        } else {
            Toast.makeText(this.getActivity(), getString(R.string.not_support_low_level), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private boolean handleExcludeWords(Preference preference, Object o) {
        String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        if (o != null && o.toString().length() > 0) {
            preference.setSummary(summary + ":" + o.toString());
        } else {
            preference.setSummary(summary);
        }
        return true;
    }

    /**
     * 把下载链接复制到剪切板中
     */
    private void clipDownloadLink() {
        ClipboardManager clipboardManager = (ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            //创建ClipData对象
            ClipData clipData = ClipData.newPlainText(getString(R.string.app_name),
                    getString(R.string.baidu_downlaod_link));
            //添加ClipData对象到剪切板中
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(this.getActivity(), "下载链接已复制到剪切板中", Toast.LENGTH_LONG).show();
        }
    }

    //打开通知监听设置页面
    public void openNotificationListenSettings() {
        try {
            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //检测通知监听服务是否被授权
    public boolean isNotificationListenerEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this.getActivity());
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    //把应用的NotificationListenerService实现类disable再enable，即可触发系统rebind操作
    private void toggleNotificationListenerService() {
        PackageManager pm = this.getActivity().getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName(this.getActivity(), HongbaoNotificationListenerService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(
                new ComponentName(this.getActivity(), HongbaoNotificationListenerService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * 是否支持自动点击
     * API >23 支持
     * API <=23 需要root
     *
     * @return result
     */
    private boolean isAutoClickPermit() {
        return android.os.Build.VERSION.SDK_INT > 23;
    }

    /**
     * 申请屏幕录取权限
     */
    private void requestScreenShot() {
        startActivityForResult(
                ((MediaProjectionManager) this.getActivity().getSystemService("media_projection")).createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    private MediaProjectionManager getMediaProjectionManager() {

        return (MediaProjectionManager) getActivity().getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
    }

    private void initPrefListeners() {
        CheckBoxPreference watchListPreference = (CheckBoxPreference) findPreference("pref_watch_list");
        CheckBoxPreference openPockeyPreference = (CheckBoxPreference) findPreference("pref_watch_chat");
        CheckBoxPreference openSelfPockeyPreference = (CheckBoxPreference) findPreference("pref_watch_self");
        CheckBoxPreference watchNotificationPreference = (CheckBoxPreference) findPreference("pref_watch_notification");
        Preference excludeWordsPref = findPreference("pref_watch_exclude_words");
        if (watchListPreference == null || openPockeyPreference == null ||
                openSelfPockeyPreference == null || watchNotificationPreference == null ||
                excludeWordsPref == null) {
            return;
        }
        String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_watch_exclude_words", "");
        if (value.length() > 0) {
            excludeWordsPref.setSummary(summary + ":" + value);
        }
        excludeWordsPref.setOnPreferenceChangeListener(this);
        watchListPreference.setOnPreferenceChangeListener(this);
        openPockeyPreference.setOnPreferenceChangeListener(this);
        openSelfPockeyPreference.setOnPreferenceChangeListener(this);
        watchNotificationPreference.setOnPreferenceChangeListener(this);
        if (watchListPreference.isChecked()) {
            requestScreenShot();
        }

        Preference updatePref = findPreference("pref_etc_check_update");
        if (updatePref != null) {
            updatePref.setOnPreferenceClickListener(this);
        }
        Preference issuesPref = findPreference("pref_etc_issue");
        if (issuesPref != null) {
            issuesPref.setOnPreferenceClickListener(this);
        }
        Preference sharedPref = findPreference("pref_etc_shared");
        if (sharedPref != null) {
            sharedPref.setOnPreferenceClickListener(this);
        }

    }

}
