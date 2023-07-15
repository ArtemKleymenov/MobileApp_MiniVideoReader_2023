package com.example.minivideoreader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

// Only for test purpose
import android.os.SystemClock;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 83854;  // Random number
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 14839;  // Random number
    private ImageView preview;
    private Button btn;
    private ProgressBar progressBar;
    private Boolean is_process_started = false;
    private File picFile;

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    YUVtoRGB translator = new YUVtoRGB();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // TODO:
        // Filename can be changed
        picFile = new File(pictures, "test/best_pic_vid.png");

        preview = findViewById(R.id.preview);
        progressBar = findViewById(R.id.progressBar);
        btn = findViewById(R.id.button);

        btn.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            is_process_started = true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            initializeCamera();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        }
    }

    private void initializeCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // TODO:
                // Sizes can be changed
                int width_  = 1000;
                int height_ = 1500;
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(width_, height_))
                        //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(MainActivity.this),
                        new ImageAnalysis.Analyzer() {
                            @Override
                            public void analyze(@NonNull ImageProxy image) {
                                //Bitmap bitmap = toBitmap(image.getImage());
                                Image img = image.getImage();
                                Bitmap bitmap = translator.translateYUV(img, MainActivity.this);
                                Bitmap rotated_bitmap
                                        = RotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
                                if(is_process_started) {
                                    is_process_started = false;
                                    btn.setVisibility(View.INVISIBLE);
                                    ProcessFrame(rotated_bitmap);
                                }
                                preview.setImageBitmap(rotated_bitmap);
                                image.close();
                            }
                        });

                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void ProcessFrame(Bitmap img) {
        Bitmap localBitmap = img;
        Thread thread = new Thread(() -> {
            // TODO:
            // THRESHOLD SHOULD BE CHANGED
            if(CREATED_METHOD(localBitmap) > 90.0) {
                try {
                    FileOutputStream fos = new FileOutputStream(picFile);
                    localBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d("ERRORA", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("ERRORA", "Error accessing file: " + e.getMessage());
                }
                AfterProcessFinished();
                is_process_started = false;
            }
            else is_process_started = true;
        });
        thread.start();
    }

    // TODO:
    // Add own method
    public double CREATED_METHOD(Bitmap img) {
        SystemClock.sleep(1000);
        Random r = new Random();
        double v = r.nextGaussian() * 50.0 + 50.0;
        return v;
    }

    public void AfterProcessFinished() {
        String toast = "Image saved into "+picFile.getAbsolutePath();
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
            btn.setVisibility(View.VISIBLE);
        });
    }

//    private Bitmap toBitmap(Image image) {
//        Image.Plane[] planes = image.getPlanes();
//        ByteBuffer buffer = planes[0].getBuffer();
//        int pixelStride = planes[0].getPixelStride();
//        int rowStride = planes[0].getRowStride();
//        int rowPadding = rowStride - pixelStride * image.getWidth();
//        Bitmap bitmap = Bitmap.createBitmap(image.getWidth()+rowPadding/pixelStride,
//                image.getHeight(), Bitmap.Config.ARGB_8888);
//        bitmap.copyPixelsFromBuffer(buffer);
//        return bitmap;
//    }
}