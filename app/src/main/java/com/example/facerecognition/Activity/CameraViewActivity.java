package com.example.facerecognition.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facerecognition.Model.VerifyNid;
import com.example.facerecognition.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraViewActivity extends AppCompatActivity {
    ImageView left, front, right, flipCamera;
    PreviewView view_finder;
    TextView text_view;
    Executor executor;

    private CameraSelector lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;


    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA,Manifest.permission.INTERNET,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE};

    FaceDetectorOptions faceDetectorOptions;
    DownloadFilesTask task;

    InputImage image;
    int flag = 0;

    String encodedImage;
    Bitmap serverImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        //container = findViewById(R.id.camera_container);
        text_view = findViewById(R.id.text_prediction);
        view_finder = findViewById(R.id.view_finder);
        left = findViewById(R.id.ivleft);
        front = findViewById(R.id.ivfron);
        right = findViewById(R.id.ivRight);
        flipCamera = findViewById(R.id.btnFlipCamera);



        encodedImage = getIntent().getStringExtra("harun");
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        serverImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);



        executor = Executors.newSingleThreadExecutor();




        if (!hasPermissions(CameraViewActivity.this, PERMISSIONS)) {
            int PERMISSION_ALL = 1;
            ActivityCompat.requestPermissions(CameraViewActivity.this, PERMISSIONS, PERMISSION_ALL);
            startCamera();
        }else{
            startCamera();
        }



//
//        if (checkPermission()) {
//            startCamera();
//        }

        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCamera();

            }
        });


        // High-accuracy landmark detection and face classification
        faceDetectorOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();



    }







    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;

    }



    private void flipCamera() {
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;

        } else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
            lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;

        }

        startCamera();
    }




    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }





    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        @SuppressLint("RestrictedApi") CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing.getLensFacing())
                .build();

        executor = Executors.newSingleThreadExecutor();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    try {
                        Thread.sleep(1000);
                        image =
                                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        // Pass image to an ML Kit Vision API
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    FaceDetector detector = FaceDetection.getClient(faceDetectorOptions);


                    Task<List<Face>> result =
                            detector.process(image)
                                    .addOnSuccessListener(
                                            new OnSuccessListener<List<Face>>() {
                                                @Override
                                                public void onSuccess(List<Face> faces) {
                                                    try {

//                                                        headRotationDetect(faces, image, toBitmap(mediaImage), imageProxy);
                                                        headRotationDetect(faces,toBitmap(mediaImage),imageProxy);
                                                        imageProxy.close();




                                                    } catch (MlKitException | InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            })
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Task failed with an exception
                                                    // ...
                                                }
                                            });


                }


//                int rotationDegrees = image.getImageInfo().getRotationDegrees();
//
//                if(SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
//                    image.close();
//                    return;
//                }
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        long duration = SystemClock.elapsedRealtime() - mLastAnalysisResultTime;
//                        double fps;
//
//                        if(duration > 0)
//                            fps = 1000.f / duration;
//                        else
//                            fps = 1000.f;
//
//                        text_view.setText("Please Move Your Head");
//                    }
//                });
//
//                mLastAnalysisResultTime = SystemClock.elapsedRealtime();


            }
        });

        cameraProvider.unbindAll();
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this,
                cameraSelector, imageAnalysis, preview);

        preview.setSurfaceProvider(view_finder.getSurfaceProvider());

    }




    private void headRotationDetect(List<Face> faces,Bitmap capturedBitmap,ImageProxy imageProxy) throws MlKitException, InterruptedException {


        for (Face face : faces) {

            Rect bounds = face.getBoundingBox();
            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            float rotZ = face.getHeadEulerAngleZ();


            Log.e("rotY", String.valueOf(rotY));


            if(flag==2 || flag == 3){
                if (rotY < -18) {






//                    right.setBackgroundTintList(getResources().getColorStateList(R.color.green));




//                    Bitmap serverImageBitmap = BitmapFactory.decodeResource(getApplication().getResources(), R.drawable.harun);
                    Bitmap capBit = getResizedBitmap(capturedBitmap,900,600);
                    Bitmap rotatBitmap = rotateBitmap(capBit,imageProxy.getImageInfo().getRotationDegrees(),false,false);



                    VerifyNid verifyNid = new VerifyNid(serverImage,rotatBitmap,CameraViewActivity.this);


                    task = (DownloadFilesTask) new DownloadFilesTask().execute(verifyNid);


//                    String text = String.format("Similarity score = %.2f", d);
//                    Log.e("Similarity",text);
                    flag=3;


                }
            }

            if(flag==1){
                if (rotY > 18) {


//                left.setBackgroundTintList(getResources().getColorStateList(R.color.green));





//                bitmap = ImageConvertUtils.getInstance().getUpRightBitmap(image);
                //savePicture(bitmap);


//                    Bitmap serverImageBitmap = BitmapFactory.decodeResource(getApplication().getResources(), R.drawable.harun);
                    Bitmap capBit = getResizedBitmap(capturedBitmap,900,600);
                    Bitmap rotatBitmap = rotateBitmap(capBit,imageProxy.getImageInfo().getRotationDegrees(),false,false);



                    VerifyNid verifyNid = new VerifyNid(serverImage,rotatBitmap,CameraViewActivity.this);
                    task  = (DownloadFilesTask) new DownloadFilesTask().execute(verifyNid);

//                    String text = String.format("Similarity score = %.2f", d);
//
//                    Log.e("Similarity",text);

                    flag = 2;


            }
            }



            Log.e("flag",String.valueOf(flag));

            if(flag!=1 && flag==0){
                text_view.setText("Please wait at least 5 seconds");
                if (rotY > -20 && rotY < 15) {
                //front face detect


//                bitmap = ImageConvertUtils.getInstance().getUpRightBitmap(image);
                //savePicture(bitmap);

//                front.setBackgroundTintList(getResources().getColorStateList(R.color.green));


//                Bitmap serverImageBitmap = BitmapFactory.decodeResource(getApplication().getResources(), R.drawable.harun);
                Bitmap capBit = getResizedBitmap(capturedBitmap,900,600);
                Bitmap rotatBitmap = rotateBitmap(capBit,imageProxy.getImageInfo().getRotationDegrees(),false,false);

                VerifyNid verifyNid = new VerifyNid(serverImage,rotatBitmap,CameraViewActivity.this);
                 task = (DownloadFilesTask) new DownloadFilesTask().execute(verifyNid);

//                String text = String.format("Similarity score = %.2f", d);
//
//                Log.e("Similarity",text);

//                front.setImageBitmap(rotatBitmap);



            }
            }

        }

    }




    private Bitmap toBitmap(Image image) {
        byte[] nv21 = YUV_420_888toNV21(image);


        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        //System.out.println("bytes"+ Arrays.toString(imageBytes));

        //System.out.println("FORMAT"+image.getFormat());

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }



    private void savePicture(Bitmap bitmap) {
        Log.e("save", "picture");
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/MDMR/Images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "test.jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Bundle conData = new Bundle();
            Intent intent = new Intent();
            String path = Uri.fromFile(file).getPath();
            Log.d("PathTest", path);
            if (!path.equalsIgnoreCase("")) {
//                new ImageModel(getContentResolver()).saveImage(path);
                conData.putString("imageName", path);
                intent.putExtras(conData);
                setResult(RESULT_OK, intent);
            } else {
                setResult(RESULT_CANCELED, intent);
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ErrorTest", e.getLocalizedMessage());
        }
    }




    private class TaskParam{
        VerifyNid verifyNid;
        float rotY;

        public TaskParam(VerifyNid verifyNid, float rotY) {
            this.verifyNid = verifyNid;
            this.rotY = rotY;
        }
    }


    private class DownloadFilesTask extends AsyncTask<VerifyNid, Void, Double> {


        @SuppressLint("WrongThread")
        @Override
        protected Double doInBackground(VerifyNid... verifyNids) {

            onPostExecute(verifyNids[0].verify());
            return null;
        }

        @Override
        protected void onPostExecute(Double doubleDoubleHashMap) {
            result(doubleDoubleHashMap);
        }
    }

    private void result(Double aDouble) {

        if(aDouble!=null){

            runOnUiThread(new Runnable() {
                public void run() {
                    if(aDouble<20.00 && aDouble>0.00){

                        task.cancel(true);

                        if(flag==0){
                            flag=1;
                        }


                        if(flag==1){
                            front.setBackgroundTintList(getResources().getColorStateList(R.color.green));
                            text_view.setText("Move your face left side & wait 5 seconds");
                        }
                        if(flag==2){
                            left.setBackgroundTintList(getResources().getColorStateList(R.color.green));
                            text_view.setText("Move your face right side & wait 5 seconds");
                        }
                        if(flag==3){
                            right.setBackgroundTintList(getResources().getColorStateList(R.color.green));
                            Intent intent = new Intent(CameraViewActivity.this,NidFetchActivity.class);

                            try {
                                Thread.sleep(1000);
                                startActivity(intent);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            flag = 0;
                            finish();

                        }


                    }
                    if(aDouble>=20.00){
                        Toast.makeText(CameraViewActivity.this, "Unverified", Toast.LENGTH_SHORT).show();
                        task.cancel(true);
                    }

                }
            });

        }
    }


    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);


        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

//        if (bm != null && !bm.isRecycled()) {
//            bm.recycle();
//        }

        return resizedBitmap;
    }


    private Bitmap rotateBitmap(
            Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }




}



