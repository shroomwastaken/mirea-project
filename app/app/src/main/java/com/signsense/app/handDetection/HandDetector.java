package com.signsense.app.handDetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.createFromOptions;

public class HandDetector {
    private static final String TAG = "HandDetector";

    private boolean mode = false;
    private boolean runOnGPU = false;
    private int maxHands = 1;
    private int modelComplexity = 1;
    private float detectionCon = 0.5f;
    private float trackCon = 0.5f;
    private List<Integer> tipIds = new ArrayList<>();
    private Context context = null;

    private MPImage image;

    private BaseOptions baseOptions;
    private HandLandmarker handLandmarker;
    private HandLandmarkerResult result;



    public HandDetector(Context context/*, boolean mode, int maxHands, int modelComplexity, float detectionCon, float trackCon*/) {
        this.context = context.getApplicationContext();
//        this.mode = mode;
//        this.maxHands = maxHands;
//        this.modelComplexity = modelComplexity;
//        this.detectionCon = detectionCon;
//        this.trackCon = trackCon;

        // Fingertip IDs (from nodes)
        for (int i = 4; i <= 20; i += 4) {
            tipIds.add(i);
        }

        // Loading model
        baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.GPU) // ALL I HAD TO DO IS TO SET IT TO GPU AND NOW IT WORKS ASGAOGYHOAHGOA
                .build();
        Log.i(TAG, "Successfully loaded Hand Detector Model " + baseOptions.toString());

        // Setting up the Hand Landmarker
        handLandmarker = createFromOptions(this.context, HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(detectionCon)
                .setMinTrackingConfidence(trackCon)
                .setMinHandPresenceConfidence(0.5f)
                .build()
        );
    }

    public Mat findHands(Bitmap bitmap, boolean draw) {
        long timestampMs = SystemClock.uptimeMillis();

        // Converting OpenCV Mat to Bitmap to Mediapipe MPImage
//        Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(frame, bitmap);
        image = new BitmapImageBuilder(bitmap).build();
        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);

        // Detecting hand
        result = handLandmarker.detect(image);
        List<Float> landmarks = new ArrayList<>();
        Log.i(TAG, handLandmarker.toString());
        Log.i(TAG, result.toString());

        // Adding tip coordinates to list of landmark
        if (result.landmarks().size() > 0) {
            for (List<NormalizedLandmark> landmark : result.landmarks()) {
                for (int tipId : tipIds) { // Getting X and Y for every tip
                    float x = landmark.get(tipId).x();
                    float y = landmark.get(tipId).y();
                    landmarks.add(x);
                    landmarks.add(y);
                    if (draw) {
                        // Drawing circles at the fingertips by finding coordinates via multiplication
                        // E.g. we have 200 pixels wide screen and the fingertip X is 0.54, so we draw x at 200 * 0.54 = 108
                        Imgproc.circle(
                                frame,
                                new Point(frame.width() * x, frame.height() * y),
                                5, new Scalar(255, 0, 0, 255),
                                10
                        );
                    }
                }
            }
        }

        Log.i(TAG, landmarks.toString());


        return frame;
    }
}
