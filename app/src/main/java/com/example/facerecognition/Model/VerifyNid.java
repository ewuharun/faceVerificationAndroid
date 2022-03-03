package com.example.facerecognition.Model;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.facerecognition.FaceDetection.Box;
import com.example.facerecognition.FaceDetection.MTCNN;
import com.example.facerecognition.FaceRecognition.FaceNet;

import java.io.IOException;
import java.util.Vector;

public class VerifyNid {

    private Bitmap serverImage;
    private Bitmap capturedImage;
    private Activity activity;

    public VerifyNid(Bitmap serverImage, Bitmap capturedImage, Activity activity) {
        this.serverImage = serverImage;
        this.capturedImage = capturedImage;
        this.activity = activity;
    }

    public Double verify(){
        Double score = 0.0;
        MTCNN mtcnn = new MTCNN(activity.getAssets());

        Bitmap face1 = cropFace(serverImage, mtcnn);
        Bitmap face2 = cropFace(capturedImage, mtcnn);


        mtcnn.close();

        FaceNet facenet = null;
        try {
            facenet = new FaceNet(activity.getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (face1 != null && face2 != null) {
             score = facenet.getSimilarityScore(face1, face2);
            Log.i("score", String.valueOf(score));
            //Toast.makeText(MainActivity.this, "Similarity score: " + score, Toast.LENGTH_LONG).show();
            String text = String.format("Similarity score = %.2f", score);

        }

        facenet.close();

        return score;
    }


    private Bitmap cropFace(Bitmap bitmap, MTCNN mtcnn){
        Bitmap croppedBitmap = null;
        try {
            Vector<Box> boxes = mtcnn.detectFaces(bitmap, 10);

            Log.i("MTCNN", "No. of faces detected: " + boxes.size());

            int left = boxes.get(0).left();
            int top = boxes.get(0).top();

            int x = boxes.get(0).left();
            int y = boxes.get(0).top();
            int width = boxes.get(0).width();
            int height = boxes.get(0).height();


            if (y + height >= bitmap.getHeight())
                height -= (y + height) - (bitmap.getHeight() - 1);
            if (x + width >= bitmap.getWidth())
                width -= (x + width) - (bitmap.getWidth() - 1);

            Log.i("MTCNN", "Final x: " + String.valueOf(x + width));
            Log.i("MTCNN", "Width: " + bitmap.getWidth());
            Log.i("MTCNN", "Final y: " + String.valueOf(y + width));
            Log.i("MTCNN", "Height: " + bitmap.getWidth());

            croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        }catch (Exception e){
            e.printStackTrace();
        }
        return croppedBitmap;
    }
}
