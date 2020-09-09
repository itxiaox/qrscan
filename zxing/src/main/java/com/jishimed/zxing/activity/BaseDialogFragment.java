package com.jishimed.zxing.activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.Objects;

public class BaseDialogFragment<T> extends DialogFragment {
    protected OnResultListener<T> mListener = null;
    protected View view;

    public interface OnResultListener<T> {
        void onResult(T result);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }


    public void setListener(OnResultListener<T> listener) {
        mListener = listener;
    }

    protected void setOnClickListener(int id, View.OnClickListener listener) {
        Objects.requireNonNull(view).findViewById(id).setOnClickListener(listener);
    }

    public void fullScreen(){
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.0f;
        window.setAttributes(windowParams);
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
//            //点击返回键不消失
//            dialog.setOnKeyListener((dialog1, keyCode, event) -> {
//                if (keyCode == KeyEvent.KEYCODE_BACK) {
//                    return true;
//                }
//                return false;
//            });
//            dialog
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        //防止重复添加fragment崩溃
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fragment = manager.findFragmentByTag(tag);
        if (fragment!=null){
            transaction.remove(fragment).commitNowAllowingStateLoss();
        }
        super.show(manager, tag);
    }
}
