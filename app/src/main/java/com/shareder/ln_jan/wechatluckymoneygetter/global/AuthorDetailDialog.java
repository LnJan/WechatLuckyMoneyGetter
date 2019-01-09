package com.shareder.ln_jan.wechatluckymoneygetter.global;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import com.shareder.ln_jan.wechatluckymoneygetter.R;

/**
 * Created by Ln_Jan on 2019/1/8.
 * 权限申请对话框
 */

public class AuthorDetailDialog extends Dialog {
    private OnNextstepClickListener mListener = null;

    public AuthorDetailDialog(Context context, int theme) {
        super(context, theme);
        setContentView(R.layout.dialog_authordetail_view);
        getWindow().getAttributes().gravity = Gravity.CENTER;
        setCancelable(false);
        Button btn=findViewById(R.id.btn_authordetail_next_step);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener!=null){
                    mListener.onClick();
                }
                dismiss();
            }
        });
    }

    public void setOnNextstepClickListener(OnNextstepClickListener l) {
        mListener = l;
    }

    public interface OnNextstepClickListener {
        void onClick();
    }
}
