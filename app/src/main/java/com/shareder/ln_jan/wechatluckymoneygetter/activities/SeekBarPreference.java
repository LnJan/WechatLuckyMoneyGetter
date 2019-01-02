package com.shareder.ln_jan.wechatluckymoneygetter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shareder.ln_jan.wechatluckymoneygetter.R;

/**
 * Created by Ln_Jan on 2019/1/1.
 * 带滚动条的DialogPreference
 */

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private TextView mTextView;
    private static final String OPEN_IMMEDIATELY = "立即拆开红包";
    private static final int SEEKBAR_MAX = 20;

    public static final String PREFERENCE_TAG = "packetDelay";

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_seekbar);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        SharedPreferences preferences = getSharedPreferences();
        int delay = preferences.getInt(PREFERENCE_TAG, 0);
        int progress = delay / 100;
        this.mSeekBar = view.findViewById(R.id.delay_seekBar);
        this.mTextView = view.findViewById(R.id.pref_seekbar_textview);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setMax(SEEKBAR_MAX);
        progress = progress > SEEKBAR_MAX ? SEEKBAR_MAX : progress;
        this.mSeekBar.setProgress(progress);
        setHintText(delay);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int delay = i * 100;
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
            editor.putInt(PREFERENCE_TAG, this.mSeekBar.getProgress() * 100);
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }

    private void setHintText(int delay) {
        if (delay == 0) {
            this.mTextView.setText(OPEN_IMMEDIATELY);
        } else {
            this.mTextView.setText("延时" + delay + "毫秒后拆开红包");
        }
    }
}
