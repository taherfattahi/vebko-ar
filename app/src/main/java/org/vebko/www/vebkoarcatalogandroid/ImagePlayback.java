package org.vebko.www.vebkoarcatalogandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.Frame;
import com.vuforia.HINT;
import com.vuforia.Image;
import com.vuforia.ObjectTracker;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.ar.pl.DebugLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Vector;

import org.vebko.www.vebkoarcatalogandroid.SampleAppMenu.SampleAppMenu;
import org.vebko.www.vebkoarcatalogandroid.SampleAppMenu.SampleAppMenuGroup;
import org.vebko.www.vebkoarcatalogandroid.SampleAppMenu.SampleAppMenuInterface;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.SampleApplicationControl;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.SampleApplicationException;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.SampleApplicationSession;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.LoadingDialogHandler;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.SampleApplicationGLView;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.Texture;


// The AR activity for the ImagePlayback sample.
public class ImagePlayback extends Activity implements SampleApplicationControl, SampleAppMenuInterface {

    // Movie for the Targets:
    public static final int NUM_TARGETS = 4;
    public static final int TEST = 0;
    private static final String LOGTAG = "ImagePlayback";
    final private static int CMD_BACK = -1;
    SampleApplicationSession vuforiaAppSession;
    Activity mActivity;
    DataSet dataSetVebco = null;
    boolean mIsDroidDevice = false;
    boolean mIsInitialized = false;
    // Helpers to detect events such as double tapping:
    private GestureDetector mGestureDetector = null;
    private SimpleOnGestureListener mSimpleListener = null;
    // Our OpenGL view:
    public SampleApplicationGLView mGlView;
    // Our renderer:
    private ImagePlaybackRenderer mRenderer;
    // The textures we will use for rendering:
    public Vector<Texture> mTextures;
    public DrawingView mUILayout;
    private SampleAppMenu mSampleAppMenu;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

//    public DrawingView drawingView;

    // Called when the activity first starts or the user navigates back
    // to an activity.
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this);

        mActivity = this;

        startLoadingAnimation();

        //todo
        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();

        //todo
        loadTextures();

        // Create the gesture detector that will handle the single and
        // double taps:
        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(), mSimpleListener);

        // Set the double tap listener:
        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
            public boolean onDoubleTap(MotionEvent e) {
                // We do not react to this event
                return false;
            }


            public boolean onDoubleTapEvent(MotionEvent e) {
                // We do not react to this event
                return false;
            }

            // Handle the single tap
            public boolean onSingleTapConfirmed(MotionEvent e) {
                boolean isSingleTapHandled = false;
                // Do not react if the StartupScreen is being displayed
                for (int i = 0; i < NUM_TARGETS; i++) {
                    // Verify that the tap happened inside the target
                    if (mRenderer != null && mRenderer.isTapOnScreenInsideTarget(i, e.getX(),
                            e.getY())) {

                        mRenderer.whichRectangleInsideTargetClicked(i, e.getX(), e.getY());
                        isSingleTapHandled = true;

                        // Even though multiple videos can be loaded only one
                        // can be playing at any point in time. This break
                        // prevents that, say, overlapping videos trigger
                        // simultaneously playback.
                        break;
                    }
                }

                return isSingleTapHandled;
            }
        });
    }

    // We want to load specific textures from the APK, which we will later
    // use for rendering.
    private void loadTextures() {

        mTextures.add(Texture.loadTextureFromApk("main-card.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("6X32A.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("3X64A.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("1X32A.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("2X32A.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("1X128A.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("4X150V.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("1X150V.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("1X300V.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("1X450V.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("analog-dc-ac-inputs.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("aux-dc.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("binary-analog-inputs.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("binary-output.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("indicator-led.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("neutrik-port.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("power-on-off.png", getAssets()));
//        try {
//            for (String s : getAssets().list("wjl_renwu"))
//                mTextures.add(Texture.loadTextureFromApk("wjl_renwu/" + s, getAssets()));
////
////            for (String s : getAssets().list("wjl_dance"))
////                mTextures.add(Texture.loadTextureFromApk("wjl_dance/" + s, getAssets()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    // Called when the activity will start interacting with the user.
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        try {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Resume the GL view:
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

    }

    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }

    // Called when the system is about to start resuming a previous activity.
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }

    // The final call you receive before your activity is destroyed.
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }

    private void startLoadingAnimation() {
        mUILayout = (DrawingView) View.inflate(this, R.layout.camera_overlay, null);

//        drawingView = (DrawingView) findViewById(R.id.drawing_view);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    // Initializes AR application components.
    private void initApplicationAR() {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImagePlaybackRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);

        // The renderer comes has the OpenGL context, thus, loading to texture
        // must happen when the surface has been created. This means that we
        // can't render from this thread (GUI) but instead we must
        // tell the GL thread to load it once the surface has been created.

        mGlView.setRenderer(mRenderer);

        for (int i = 0; i < NUM_TARGETS; i++) {
            float[] temp = {0f, 0f, 0f};
            mRenderer.targetPositiveDimensions[i].setData(temp);
            mRenderer.imagePlaybackTextureID[i] = -1;
        }

    }

    // We do not handle the touch event here, we just forward it to the
    // gesture detector
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        if (mSampleAppMenu != null)
            result = mSampleAppMenu.processEvent(event);

        // Process the Gestures
        if (!result) {
            mGestureDetector.onTouchEvent(event);
            mUILayout.onTouchEventCustom(event);
        }

        return result;
    }

    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
                .getClassType());
        if (tracker == null) {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        }

        return result;
    }

    @Override
    public boolean doLoadTrackersData() {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        // Create the data sets:
        dataSetVebco = objectTracker.createDataSet();
        if (dataSetVebco == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data sets:
        if (!dataSetVebco.load("VebcoTester.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        // Activate the data set:
        if (!objectTracker.activateDataSet(dataSetVebco)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.start();
            Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
        } else
            result = false;

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        else
            result = false;

        return result;
    }

    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        if (dataSetVebco != null) {
            if (objectTracker.getActiveDataSet(0) == dataSetVebco
                    && !objectTracker.deactivateDataSet(dataSetVebco)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set VebcoTester because the data set could not be deactivated.");
                result = false;
            } else if (!objectTracker.destroyDataSet(dataSetVebco)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set VebcoTester.");
                result = false;
            }

            dataSetVebco = null;
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        // Deinit the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.mIsActive = true;

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Hides the Loading Dialog
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (SampleApplicationException e) {
                Log.e(LOGTAG, e.getString());
            }

            //todo
            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
//            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

//            mSampleAppMenu = new SampleAppMenu(this, this, "Video Playback",
//                    mGlView, mUILayout, null);
//            setSampleAppMenuSettings();

            mIsInitialized = true;

        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    @Override
    public void onVuforiaResumed() {
    }

    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImagePlayback.this);
                builder.setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    @Override
    public void onVuforiaUpdate(State state) {
        //todo
//        Image imageRGB565 = null;
//        Frame frame = state.getFrame();
//
//        for (int i = 0; i < frame.getNumImages(); ++i) {
//            Image image = frame.getImage(i);
//            if (image.getFormat() == PIXEL_FORMAT.RGB565) {
//
//                Bitmap srcBmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.RGB_565);
//                srcBmp.copyPixelsFromBuffer(image.getPixels());
//
////                Mat rawMat = new Mat(srcBmp.getWidth(), srcBmp.getHeight(), CvType.CV_8UC3);
////                Utils.bitmapToMat(srcBmp, rawMat);
//
//                if (srcBmp != null) {
//                    OutputStream fOut = null;
//                    Integer counter = 0;
//                    String path = Environment.getExternalStorageDirectory().toString();
//                    File file = new File(path, "FitnessGirl" + counter + ".jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
//                    try {
//                        fOut = new FileOutputStream(file);
//
//                        // BitmapFactory.Options options = new BitmapFactory.Options();
//                        // options.inMutable = true;
//                        // Bitmap pictureBitmap = BitmapFactory.decodeByteArray(pixelArray, 0, pixelArray.length, options);
//                        // Canvas canvas = new Canvas(bmp); // now it should work ok
//                        srcBmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
//                        fOut.flush(); // Not really required
//                        fOut.close(); // do not forget to close the stream
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                imageRGB565 = image;
//                break;
//            }
//        }
//
//        if (imageRGB565 != null) {
//            ByteBuffer pixels = imageRGB565.getPixels();
//            byte[] pixelArray = new byte[pixels.remaining()];
//            pixels.get(pixelArray, 0, pixelArray.length);
//            int imageWidth = imageRGB565.getWidth();
//            int imageHeight = imageRGB565.getHeight();
//            int stride = imageRGB565.getStride();
//            DebugLog.LOGD("Image", "Image width: " + imageWidth);
//            DebugLog.LOGD("Image", "Image height: " + imageHeight);
//            DebugLog.LOGD("Image", "Image stride: " + stride);
//            DebugLog.LOGD("Image", "First pixel byte: " + pixelArray[0]);
//        }
    }

    @Override
    public void onVuforiaStarted() {

    }

    // This method sets the menu's settings
    private void setSampleAppMenuSettings() {
        SampleAppMenuGroup group;

        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        mSampleAppMenu.attachMenu();
    }


    @Override
    public boolean menuProcess(int command) {

        boolean result = true;

        switch (command) {
            case CMD_BACK:
                finish();
                break;
        }

        return result;
    }

}
