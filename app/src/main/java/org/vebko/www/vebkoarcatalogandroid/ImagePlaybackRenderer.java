package org.vebko.www.vebkoarcatalogandroid;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Rectangle;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.vebko.www.vebkoarcatalogandroid.SampleApplication.SampleApplicationSession;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.SampleMath;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.SampleUtils;
import org.vebko.www.vebkoarcatalogandroid.SampleApplication.utils.Texture;

// The renderer class for the ImagePlayback sample.
public class ImagePlaybackRenderer implements GLSurfaceView.Renderer {

    private static final String LOGTAG = "ImagePlaybackRenderer";
    static int NUM_QUAD_VERTEX = 4;
    static int NUM_QUAD_INDEX = 6;
    public boolean mIsActive = false;
    SampleApplicationSession vuforiaAppSession;
    // Video Playback Textures for the two targets
    int imagePlaybackTextureID[] = new int[ImagePlayback.NUM_TARGETS];
    // Trackable dimensions
    Vec3F targetPositiveDimensions[] = new Vec3F[ImagePlayback.NUM_TARGETS];
    double quadVerticesArray[] = {-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f};
    double quadTexCoordsArray[] = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f};
    double quadNormalsArray[] = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,};
    short quadIndicesArray[] = {0, 1, 2, 2, 3, 0};
    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;
    ImagePlayback mActivity;
    // Needed to calculate whether a screen tap is inside the target
    Matrix44F modelViewMatrix[] = new Matrix44F[ImagePlayback.NUM_TARGETS];
    boolean isTracking[] = new boolean[ImagePlayback.NUM_TARGETS];
    // These hold the aspect ratio of both the video and the
    // keyframe
    float keyframeQuadAspectRatio[] = new float[ImagePlayback.NUM_TARGETS];
    // Keyframe and icon rendering specific
    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeNormalHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;
    private long mLostTrackingSince[] = null;
    private Vector<Texture> mTextures;
    private int index[] = new int[5];

    private String pathFile = "power-on-off";
    private boolean isChange;

    private Hashtable<String, Rectangle> rectanglesHT = new Hashtable<String, Rectangle>();
//    private Hashtable<String, String> rectangles2imagePath = new Hashtable<String, String>();

    public ImagePlaybackRenderer(ImagePlayback activity, SampleApplicationSession session) {

        rectanglesHT.put("VOutA",new Rectangle(546, 849, 909, 916));
        rectanglesHT.put("COutA",new Rectangle(551, 948, 773, 1120));
//        rectanglesHT.put("COutB",new Rectangle(141, 135, 206, 154));
        rectanglesHT.put("AUXDC",new Rectangle(800, 923, 905, 983));
//        rectanglesHT.put("VOutB",new Rectangle(215, 73, 246, 93));
        rectanglesHT.put("NEUTRIK",new Rectangle(799, 991 , 906, 1106));
        rectanglesHT.put("BINARYOUT",new Rectangle(977, 848, 1201, 973));
        rectanglesHT.put("ANALOGACDCINPUT",new Rectangle(1204, 849, 1310, 973));
        rectanglesHT.put("BINARYANALOGINPUT",new Rectangle(920, 1001, 1365, 1122));
        rectanglesHT.put("INDICATOR",new Rectangle(1367, 974, 1446, 1025));
        rectanglesHT.put("POWER",new Rectangle(1367, 1049, 1446, 1142));
//        rectangles2imagePath.put("VOutA","q1");
//        rectangles2imagePath.put("COutA","e");
        mActivity = activity;
        vuforiaAppSession = session;

        // Create an array of the size of the number of targets we have
        mLostTrackingSince = new long[ImagePlayback.NUM_TARGETS];

        // Initialize the arrays to default values
        for (int i = 0; i < ImagePlayback.NUM_TARGETS; i++) {
            mLostTrackingSince[i] = -1;
            targetPositiveDimensions[i] = new Vec3F();
            modelViewMatrix[i] = new Matrix44F();
        }
    }

    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Call function to initialize rendering:
        // The video texture is also created on this step
        initRendering();

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        Vuforia.onSurfaceCreated();
    }

    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Call Vuforia function to handle render surface size changes:
        Vuforia.onSurfaceChanged(width, height);

    }


    // Called to draw the current frame.
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

        // Call our function to render content
        renderFrame();

        for (int i = 0; i < ImagePlayback.NUM_TARGETS; i++) {
            // Ask whether the target is currently being tracked and if so react
            // to it
            if (isTracking(i)) {
                // If it is tracking reset the timestamp for lost tracking
                mLostTrackingSince[i] = -1;
            } else {
                // If it isn't tracking
                // check whether it just lost it or if it's been a while
                if (mLostTrackingSince[i] < 0)
                    mLostTrackingSince[i] = SystemClock.uptimeMillis();
            }
        }

        // If you would like the video to start playing as soon as it starts
        // tracking
        // and pause as soon as tracking is lost you can do that here by
        // commenting
        // the for-loop above and instead checking whether the isTracking()
        // value has
        // changed since the last frame. Notice that you need to be careful not
        // to
        // trigger automatic playback for fullscreen since that will be
        // inconvenient
        // for your users.

    }

    @SuppressLint("InlinedApi")
    void initRendering() {
        Log.d(LOGTAG, "ImagePlayback ImagePlaybackRenderer initRendering");

        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures) {
            // Here we create the textures for the keyframe
            // and for all the icons
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        // Now we create the texture for the video data from the movie
        // IMPORTANT:
        // Notice that the textures are not typical GL_TEXTURE_2D textures
        // but instead are GL_TEXTURE_EXTERNAL_OES extension textures
        // This is required by the Android SurfaceTexture
        for (int i = 0; i < ImagePlayback.NUM_TARGETS; i++) {
            GLES20.glGenTextures(1, imagePlaybackTextureID, i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    imagePlaybackTextureID[i]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        // This is a simpler shader with regular 2D textures
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
                KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
                KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexPosition");
        keyframeNormalHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexNormal");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
                "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
                keyframeShaderID, "texSampler2D");

        keyframeQuadAspectRatio[ImagePlayback.TEST] = (float) mTextures
                .get(0).mHeight / (float) mTextures.get(0).mWidth;

        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);

    }

    private Buffer fillBuffer(double[] array) {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();

        return bb;

    }

    private Buffer fillBuffer(short[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
        // short
        // takes 2
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();

        return bb;
    }

    private Buffer fillBuffer(float[] array) {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;
    }

    @SuppressLint("InlinedApi")
    void renderFrame() {
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Get the state from Vuforia and mark the beginning of a rendering
        // section
        State state = Renderer.getInstance().begin();

        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        // We must detect if background reflection is active and adjust the
        // culling direction.
        // If the reflection is active, this means the post matrix has been
        // reflected as well,
        // therefore standard counter clockwise face culling will result in
        // "inside out" models.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        float temp[] = {0.0f, 0.0f, 0.0f};
        for (int i = 0; i < ImagePlayback.NUM_TARGETS; i++) {
            isTracking[i] = false;
            targetPositiveDimensions[i].setData(temp);
        }

        // Did we find any trackables this frame?
//        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
        int tIdx = 0;
        if (state.getNumTrackableResults() > 0) {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);

            ImageTarget imageTarget = (ImageTarget) trackableResult
                    .getTrackable();

            int currentTarget;

            // We store the modelview matrix to be used later by the tap
            // calculation
            if (imageTarget.getName().compareTo("GuBei") == 0)
                currentTarget = ImagePlayback.TEST;
            else
                currentTarget = ImagePlayback.TEST;

            modelViewMatrix[currentTarget] = Tool
                    .convertPose2GLMatrix(trackableResult.getPose());

            isTracking[currentTarget] = true;

            targetPositiveDimensions[currentTarget] = imageTarget.getSize();

            // The pose delivers the center of the target, thus the dimensions
            // go from -width/2 to width/2, same for height
            temp[0] = targetPositiveDimensions[currentTarget].getData()[0] / 2.0f;
            temp[1] = targetPositiveDimensions[currentTarget].getData()[1] / 2.0f;
            targetPositiveDimensions[currentTarget].setData(temp);

            // If the movie is ready to start playing or it has reached the end
            // of playback we render the keyframe
//            if (!isChange){
            render(trackableResult, currentTarget, 0, "main-card");
            render(trackableResult, currentTarget, 0, pathFile);
//            render(trackableResult, currentTarget, 1, "wjl_renwu");
//            }else{
//            }
//            render(trackableResult, currentTarget, 1, "wjl_fountain");
//            render(trackableResult, currentTarget, 2, "wjl_renwu");

            SampleUtils.checkGLError("ImagePlayback renderFrame");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();
    }

//    public static Image getImageFromFrame(CloseableFrame frame, int format) {
//
//        long numImgs = frame.getNumImages();
//        for (int i = 0; i < numImgs; i++) {
//            if (frame.getImage(i).getFormat() == format) {
//                return frame.getImage(i);
//            }//if
//        }//for
//
//        return null;
//    }

    private void render(TrackableResult trackableResult, int currentTarget, int layer, String path) {
        float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
                trackableResult.getPose()).getData();
        float[] modelViewProjectionKeyframe = new float[16];
//        if (layer > 0)
//            Matrix.translateM(modelViewMatrixKeyframe, 0, 0, 0,
//                    targetPositiveDimensions[currentTarget].getData()[1] / 20f * layer);
//        Matrix.translateM(modelViewMatrixKeyframe, 0, 0, 0, 1f * layer);

//        Log.e("111111", "" + targetPositiveDimensions[currentTarget].getData()[1]);
        // Here we use the aspect ratio of the keyframe since it
        // is likely that it is not a perfect square

        float ratio = 1.0f;
        if (mTextures.get(currentTarget).mSuccess)
            ratio = keyframeQuadAspectRatio[currentTarget];
        else
            ratio = targetPositiveDimensions[currentTarget].getData()[1]
                    / targetPositiveDimensions[currentTarget].getData()[0];

        Matrix.scaleM(modelViewMatrixKeyframe, 0,
                targetPositiveDimensions[currentTarget].getData()[0],
                targetPositiveDimensions[currentTarget].getData()[0]
                        * ratio,
                targetPositiveDimensions[currentTarget].getData()[2]);
        Matrix.multiplyMM(modelViewProjectionKeyframe, 0,
                vuforiaAppSession.getProjectionMatrix().getData(), 0,
                modelViewMatrixKeyframe, 0);

        //enable blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glBlendFunc(GLES20.GL_DST_ALPHA, GLES20.GL_ONE_MINUS_DST_ALPHA);

        GLES20.glUseProgram(keyframeShaderID);

        // Prepare for rendering the keyframe
        GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                GLES20.GL_FLOAT, false, 0, quadVertices);
        GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                GLES20.GL_FLOAT, false, 0, quadNormals);
        GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, quadTexCoords);

        GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
        GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
        GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + layer);
        if (layer > 0) {
            index[layer]++;
        }
        index[layer] = index[layer] % mTextures.size();
        while (!mTextures.get(index[layer]).name.contains(path)) {
            index[layer]++;
            index[layer] = index[layer] % mTextures.size();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(index[layer]).mTextureID[0]);
        GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                modelViewProjectionKeyframe, 0);
        GLES20.glUniform1i(keyframeTexSampler2DHandle, layer);

        // Render
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                GLES20.GL_UNSIGNED_SHORT, quadIndices);

        GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
        GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
        GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);

        GLES20.glUseProgram(0);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    boolean isTapOnScreenInsideTarget(int target, float x, float y) {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
                        .Matrix44FInverse(vuforiaAppSession.getProjectionMatrix()),
                modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
                new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));

        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target].getData()[0]))
                && (intersection.getData()[0] <= (targetPositiveDimensions[target].getData()[0]))
                && (intersection.getData()[1] >= -(targetPositiveDimensions[target].getData()[1]))
                && (intersection.getData()[1] <= (targetPositiveDimensions[target].getData()[1]))) {
        }

        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target].getData()[0]))
                && (intersection.getData()[0] <= (targetPositiveDimensions[target].getData()[0]))
                && (intersection.getData()[1] >= -(targetPositiveDimensions[target].getData()[1]))
                && (intersection.getData()[1] <= (targetPositiveDimensions[target].getData()[1]))) {

            return true;
        }
        else{
            return false;
        }
    }

    boolean isTapOnScreenInsideRectangle(int target, float x, float y, Rectangle r) {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();


        //todo
        Log.d("2222222222222222", "onTouchEvent: ");
        for (int i=0; i<mActivity.mUILayout.listOfListsPoints.size(); i++){
            for (int j=0; j < mActivity.mUILayout.listOfListsPoints.get(i).size(); j++){
                Log.d("test: ", "onTouchEvent: "+ mActivity.mUILayout.listOfListsPoints.get(i).get(j));
            }
        }
        Log.d("11111111111111111", "onTouchEvent: ");

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
                        .Matrix44FInverse(vuforiaAppSession.getProjectionMatrix()),
                modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
                new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));
        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target].getData()[0])+r.getLeftTopX())
                && (intersection.getData()[0] <= -(targetPositiveDimensions[target].getData()[0])+r.getRightBottomX())
                && (intersection.getData()[1] >= (targetPositiveDimensions[target].getData()[1])-r.getRightBottomY())
                && (intersection.getData()[1] <= (targetPositiveDimensions[target].getData()[1])-r.getLeftTopY())) {
            return true;
        }
        else{
            return false;
        }
    }

    void whichRectangleInsideTargetClicked(int target, float x, float y){
        Enumeration<String> enumeration = rectanglesHT.keys();

        // iterate using enumeration object
        while(enumeration.hasMoreElements()) {

            String key = enumeration.nextElement();
            Rectangle currentRect = rectanglesHT.get(key);
            //todo
            if (isTapOnScreenInsideRectangle(target, x, y, currentRect)){
//                pathFile = rectangles2imagePath.get(key);
                switch (key) {
                    case "VOutA":
                        switch (pathFile){
                            case "4X150V":
                                pathFile = "1X150V";
                                break;
                            case "1X150V":
                                pathFile = "1X300V";
                                break;
                            case "1X300V":
                                pathFile = "1X450V";
                                break;
                            case "1X450V":
                                pathFile = "4X150V";
                                break;
                            default:
                                pathFile = "4X150V";
                                break;
                        }
                        break;
                    case "COutA":
                        switch (pathFile){
                            case "6X32A":
                                pathFile = "3X64A";
                                break;
                            case "3X64A":
                                pathFile = "1X32A";
                                break;
                            case "1X32A":
                                pathFile = "2X32A";
                                break;
                            case "2X32A":
                                pathFile = "1X128A";
                                break;
                            default:
                                pathFile = "6X32A";
                                break;
                        }
                        break;
                    case "AUXDC":
                        pathFile = "aux-dc";
                        break;
                    case "NEUTRIK":
                        pathFile = "neutrik-port";
                        break;
                    case "BINARYOUT":
                        pathFile = "binary-output";
                        break;
                    case "ANALOGACDCINPUT":
                        pathFile = "analog-dc-ac-inputs";
                        break;
                    case "BINARYANALOGINPUT":
                        pathFile = "binary-analog-inputs";
                        break;
                    case "INDICATOR":
                        pathFile = "indicator-led";
                        break;
                    case "POWER":
                        pathFile = "power-on-off";
                        break;
                }
                break;
            }

        }
    }

    // Multiply the UV coordinates by the given transformation matrix
    float[] uvMultMat4f(float transformedU, float transformedV, float u,
                        float v, float[] pMat) {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */ + pMat[12]
                * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */ + pMat[13]
                * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;

        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }


    boolean isTracking(int target) {
        return isTracking[target];
    }


    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }

}