/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jishimed.zxing.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.jishimed.zxing.R;
import com.jishimed.zxing.camera.CameraManager;
import com.jishimed.zxing.decode.DecodeThread;
import com.jishimed.zxing.utils.BeepManager;
import com.jishimed.zxing.utils.CaptureActivityHandler;
import com.jishimed.zxing.utils.InactivityTimer;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureFragment extends BaseDialogFragment<String> implements SurfaceHolder.Callback {

    private static final String TAG = CaptureFragment.class.getSimpleName();
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    TextView tv_capture_show;
    private Rect mCropRect = null;
    private boolean isHasSurface = false;
    public Handler getHandler() {
        return handler;
    }
    public CameraManager getCameraManager() {
        return cameraManager;
    }
    InputClickListener inputClickListener;

    public interface InputClickListener {
        void onClick(CaptureFragment captureFragment, String msg);
    }

    public void setInputClickListener(InputClickListener clickListener) {
        this.inputClickListener = clickListener;
    }

    private View view;
    private TextView tv_capture_input;
    private boolean show;

    public void showInputHandle(boolean show) {
        this.show = show;
    }


    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView: CaptureFragment");
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = inflater.inflate(R.layout.activity_capture, container, false);
        scanPreview = view.findViewById(R.id.capture_preview);
        scanContainer = view.findViewById(R.id.capture_container);
        scanCropView = view.findViewById(R.id.capture_crop_view);
        scanLine = view.findViewById(R.id.capture_scan_line);
        tv_capture_input = view.findViewById(R.id.tv_capture_input);
        tv_capture_show = view.findViewById(R.id.tv_capture_show);
        tv_capture_show.setVisibility(show ? View.VISIBLE : View.GONE);
        inactivityTimer = new InactivityTimer(getActivity());
        beepManager = new BeepManager(getActivity());
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT,
                0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);

        tv_capture_input.setOnClickListener(v -> {
            if (inputClickListener != null) {
                dismiss();
                inputClickListener.onClick(CaptureFragment.this, "success");
            }
        });

        return view;
    }


    @Override
    public void onCancel(DialogInterface dialog) {
//        super.onCancel(dialog);
        if (mListener != null) mListener.onResult(null);

    }

    @Override
    public void onHiddenChanged(boolean hide) {

        getActivity().setRequestedOrientation(//通过程序改变屏幕显示的方向
                hide ? ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                        : ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onStart() {
        super.onStart();
        super.fullScreen();
        Log.d(TAG, "onStart: CaptureFragment");
//        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
//            @Override
//            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//                if (keyCode == KeyEvent.KEYCODE_BACK) {
//                    return true;
//                }
//                return false;
//            }
//        });
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // CameraManager must be initialized here, not in onCreate(). This is
//        // necessary because we don't
//        // want to open the camera driver and measure the screen size if we're
//        // going to show the help on
//        // first launch. That led to bugs where the scanning rectangle was the
//        // wrong size and partially
//        // off screen.
//
//    }
//
//    @Override
//    protected void onPause() {
//
//        super.onPause();
//    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getActivity().getApplication());
        handler = null;
        if (isHasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            scanPreview.getHolder().addCallback(this);
        }
        inactivityTimer.onResume();
    }

    @Override
    public void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }


    @Override
    public void onDestroyView() {
        inactivityTimer.shutdown();
        super.onDestroyView();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param bundle    The extras
     */
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        tv_capture_show.setText("扫码成功...");
//        Intent resultIntent = new Intent();
//        bundle.putInt("width", mCropRect.width());
//        bundle.putInt("height", mCropRect.height());
//        bundle.putString("result", rawResult.getText());
//        resultIntent.putExtras(bundle);
//        this.setResult(RESULT_OK, resultIntent);
//        CaptureFragment.this.finish();

        if (mListener != null) {
            mListener.onResult(rawResult.getText());
        }
    }

    public void finishFragment(String result) {
        if (mListener != null) {
            mListener.onResult(result);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }

            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.scan_qr_title);
        builder.setMessage("Camera error");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dismiss();
            }
        });
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}