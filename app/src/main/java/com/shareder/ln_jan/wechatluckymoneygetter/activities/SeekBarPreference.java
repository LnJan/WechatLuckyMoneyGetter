package com.shareder.ln_jan.wechatluckymoneygetter.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shareder.ln_jan.wechatluckymoneygetter.R;

import java.util.Locale;

/**
 * Created by Ln_Jan on 2019/1/1.
 * 带滚动条的DialogPreference
 */

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private TextView mTextView;
    private String mSeekbarKind;
    private static final String OPEN_IMMEDIATELY = "立即拆开红包";
    private static final int SEEKBAR_MAX = 20;

    public static final String PREFERENCE_TAG = "packetDelay";
    public static final String PREFERENCE_EVENT_TAG = "eventInterval";

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_seekbar);
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            if (attr.equalsIgnoreCase("seekbarkind")) {
                mSeekbarKind = attrs.getAttributeValue(i);
                break;
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        SharedPreferences preferences = getSharedPreferences();
        this.mSeekBar = view.findViewById(R.id.delay_seekBar);
        this.mTextView = view.findViewById(R.id.pref_seekbar_textview);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        if (mSeekbarKind.equals("open_delay")) {
            int delay = preferences.getInt(PREFERENCE_TAG, 0);
            int progress = delay / 100;
            this.mSeekBar.setMax(SEEKBAR_MAX);
            progress = progress > SEEKBAR_MAX ? SEEKBAR_MAX : progress;
            this.mSeekBar.setProgress(progress);
            setHintText(delay);
        } else if (mSeekbarKind.equals("event_interval")) {
            int eventDelay = preferences.getInt(PREFERENCE_EVENT_TAG, 100);
            int progress = eventDelay / 50;
            progress = progress > 0 ? progress : 1;
            progress = progress <= 10 ? progress : 10;
            this.mSeekBar.setMax(9);
            this.mSeekBar.setProgress(progress - 1);
            setHintText(eventDelay);
        } else {
            setHintText(0);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int delay = 0;
        if (mSeekbarKind.equals("open_delay")) {
            delay = i * 100;
        } else if (mSeekbarKind.equals("event_interval")) {
            delay = (i + 1) * 50;
        } else {
            return;
        }
        setHintText(delay);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SharedPreferences.Editor editor = getEditor();
            if (mSeekbarKind.equals("open_delay")) {
                editor.putInt(PREFERENCE_TAG, this.mSeekBar.getProgress() * 100);
                editor.commit();
            } else if (mSeekbarKind.equals("event_interval")) {
                editor.putInt(PREFERENCE_EVENT_TAG, (this.mSeekBar.getProgress() + 1) * 50);
                editor.commit();
                if (Build.VERSION.SDK_INT >= 24) {
                    Toast.makeText(this.getContext(), "需重新启动服务，配置方可生效", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this.getContext(), "需手动重启服务使配置生效！", Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent("com.shareder.ln_jan.broadcast.shutdownservice");
                this.getContext().sendBroadcast(intent);
            }
        }
        super.onDialogClosed(positiveResult);
    }

    private void setHintText(int delay) {
        if (mSeekbarKind.equals("open_delay")) {
            if (delay == 0) {
                this.mTextView.setText(OPEN_IMMEDIATELY);
            } else {
                this.mTextView.setText(String.format(Locale.CHINA, "延时%d毫秒后拆开红包", delay));
            }
        } else if (mSeekbarKind.equals("event_interval")) {
            this.mTextView.setText(String.format(Locale.CHINA, "%d ms", delay));
        } else {
            this.mTextView.setText("暂无信息");
        }
    }
}
