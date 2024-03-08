// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.torchvideo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.TextureView;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
//import androidx.core.content.ResourcesCompat;
import androidx.core.view.WindowCompat;
import org.pytorch.demo.torchvideo.R;

public abstract class AbstractCameraXActivity<R> extends BaseModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        int TransparentColour = ResourcesCompat.getColor( getResources(), android.R.color.holo_blue_dark, getTheme() );
        getWindow().setNavigationBarColor( TransparentColour );


//        int BlueColor = ResourcesCompat.getColor( getResources(), R.color.navigationBarBlue, getTheme() );
//                android:background="#FF2356E7"
//        getWindow().setNavigationBarColor( BlueColor );




        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows( getWindow(), false );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows( false );
        }

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                                this,
                                "You can't use live video classification example without granting CAMERA permission",
                                Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                setupCameraX();
            }
        }
    }

    private void setupCameraX() {
        final TextureView textureView = getCameraPreviewTextureView();
        final PreviewConfig previewConfig = new PreviewConfig.Builder().build();
        final Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));

        final ImageAnalysisConfig imageAnalysisConfig =
            new ImageAnalysisConfig.Builder()
                .setTargetResolution(new Size(480, 640))
                .setCallbackHandler(mBackgroundHandler)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();
        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
                return;
            }

            final R result = analyzeImage(image, rotationDegrees);
            if (result != null) {
                mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
            }
        });

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @WorkerThread
    @Nullable
    protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(R result);
}
