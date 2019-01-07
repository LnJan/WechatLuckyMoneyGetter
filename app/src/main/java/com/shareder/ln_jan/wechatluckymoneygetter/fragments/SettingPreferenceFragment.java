package com.shareder.ln_jan.wechatluckymoneygetter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;
import com.shareder.ln_jan.wechatluckymoneygetter.global.MyTransparentDialog;
import com.shareder.ln_jan.wechatluckymoneygetter.utils.ScreenShotter;

/**
 * Created by Ln_Jan on 2018/11/8.
 */

public class SettingPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String SCREENSHORT_TIPS = "由于技术原因目前无法直接获取聊天列表中的文字信息，" +
            "目前只能通过截屏的方式判断列表中是否包含红包信息";

    private static final int REQUEST_MEDIA_PROJECTION = 0x01;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preference);
        initPrefListeners();
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
        }
        return b;
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

    private boolean handleWatchChart() {
        if (isAutoClickPermit()) {
            return true;
        } else {
            Toast.makeText(this.getActivity(), getString(R.string.not_support_low_level), Toast.LENGTH_SHORT).show();
            return false;
        }
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
        Preference excludeWordsPref = findPreference("pref_watch_exclude_words");
        String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_watch_exclude_words", "");
        if (value.length() > 0) {
            excludeWordsPref.setSummary(summary + ":" + value);
        }
        excludeWordsPref.setOnPreferenceChangeListener(this);
        watchListPreference.setOnPreferenceChangeListener(this);
        openPockeyPreference.setOnPreferenceChangeListener(this);
        openSelfPockeyPreference.setOnPreferenceChangeListener(this);
        if (watchListPreference.isChecked()) {
            requestScreenShot();
        }
    }

}
