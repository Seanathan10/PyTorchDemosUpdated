package org.pytorch.demo.aslrecognition;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;

public class MainActivity extends AppCompatActivity implements Runnable {
    private ImageView mImageView;
    private Button mButtonRecognize;
    private TextView mTvResult;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private int mStartLetterPos = 1;
    private String mLetter = "A";
    public final static int SIZE = 200;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows( getWindow(), false );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows( false );
        }

        int transparent = ResourcesCompat.getColor(getResources(), android.R.color.transparent, getTheme());

//        getWindow().setStatusBarColor( transparent );
        getWindow().setNavigationBarColor( transparent );



        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open("A1.jpg"));
        } catch (IOException e) {
            Log.e("ASLRecognition", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);

        mTvResult = findViewById(R.id.tvResult);
        mTvResult.setText(mLetter);


        mImageView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
            return insets;
        });



        final Button btnNext = findViewById(R.id.nextButton);
        btnNext.setOnClickListener(v -> {
            mStartLetterPos = (mStartLetterPos + 1) % 26;
            if (mStartLetterPos == 0) {
                mStartLetterPos = 26;
            }
            mLetter = String.valueOf((char)(mStartLetterPos + 64));
            String imageName = String.format("%s1.jpg", mLetter);
            mTvResult.setText(mLetter);
            try {
                mBitmap = BitmapFactory.decodeStream(getAssets().open(imageName));
                mImageView.setImageBitmap(mBitmap);
            } catch (IOException e) {
                Log.e("ASLRecognition", "Error reading assets", e);
                finish();
            }
        });

        mButtonRecognize = findViewById(R.id.recognizeButton);
        mButtonRecognize.setOnClickListener(v -> {
            Thread thread = new Thread(MainActivity.this);
            thread.start();
        });

        final Button buttonLive = findViewById(R.id.liveButton);
        buttonLive.setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, LiveASLRecognitionActivity.class);
            startActivity(intent);
        });

        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "asl.ptl"));
        } catch (IOException e) {
            Log.e("ASLRecognition", "Error reading model file", e);
            finish();
        }
    }

    public static Pair<Integer, Long> bitmapRecognition(Bitmap bitmap, Module module) {
        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(3 * SIZE * SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int colour = bitmap.getPixel(x, y);

                int red = Color.red(colour);
                int blue = Color.blue(colour);
                int green = Color.green(colour);
                inTensorBuffer.put(x + SIZE * y, (float) blue);
                inTensorBuffer.put(SIZE * SIZE + x + SIZE * y, (float) green);
                inTensorBuffer.put(2 * SIZE * SIZE + x + SIZE * y, (float) red);
            }
        }

        Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, SIZE, SIZE});
        final long startTime = SystemClock.elapsedRealtime();
        Tensor outTensor = module.forward(IValue.from(inputTensor)).toTensor();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;

        final float[] scores = outTensor.getDataAsFloatArray();
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
        return new Pair<>(maxScoreIdx, inferenceTime);
    }

    @Override
    public void run() {
        Pair<Integer, Long> idxTm = bitmapRecognition(mBitmap, mModule);

        int finalMaxScoreIdx = idxTm.first;
        runOnUiThread(() -> {
            mTvResult.setText(String.format("%s - %s", mLetter, (char) (1 + finalMaxScoreIdx + 64)));
            mButtonRecognize.setEnabled(true);
            mButtonRecognize.setText(getString(R.string.recognize));
        });
    }
}
