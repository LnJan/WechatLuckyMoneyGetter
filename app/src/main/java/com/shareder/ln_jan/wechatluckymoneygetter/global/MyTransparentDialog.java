package com.shareder.ln_jan.wechatluckymoneygetter.global;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.shareder.ln_jan.wechatluckymoneygetter.R;

/**
 * Created by Ln_Jan on 2018/12/27.
 * 半透明对话框
 */

public class MyTransparentDialog extends Dialog implements View.OnClickListener {
    private MyDialogOnClickListener mListener = null;

    private MyTransparentDialog(Context context, int theme) {
        super(context, theme);
        setContentView(R.layout.dialog_transparent_view);
        getWindow().getAttributes().gravity = Gravity.CENTER;
        Button btnNext = findViewById(R.id.btn_transparent_next_step);
        btnNext.setOnClickListener(this);
    }

    private void nextStepBtnClick() {
        AppCompatCheckBox checkBox = findViewById(R.id.cb_transparent_show_ask);
        if (mListener != null) {
            mListener.onClick(checkBox.isChecked());
        }
    }

    public static MyTransparentDialog createTransparentDialog(Context context) {
        return new MyTransparentDialog(context, R.style.Transparent);
    }

    public MyTransparentDialog setTitle(String title) {
        TextView tv = findViewById(R.id.tv_transparent_title);
        tv.setText(title);
        return this;
    }

    public MyTransparentDialog setContent(String content) {
        TextView tv = findViewById(R.id.tv_transparent_content);
        tv.setText(content);
        return this;
    }

    public MyTransparentDialog setNotshowVisible(boolean b) {
        AppCompatCheckBox checkBox = findViewById(R.id.cb_transparent_show_ask);
        if (checkBox != null) {
            if (b) {
                checkBox.setVisibility(View.VISIBLE);
            } else {
                checkBox.setVisibility(View.INVISIBLE);
            }
        }
        return this;
    }

    public MyTransparentDialog setMyDialogOnClickListener(MyDialogOnClickListener l) {
        this.mListener = l;
        return this;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_transparent_next_step:
                nextStepBtnClick();
                break;
            default:
                break;
        }
    }

    public interface MyDialogOnClickListener {
        void onClick(boolean isChecked);
    }
}
