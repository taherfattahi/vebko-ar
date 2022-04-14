package org.vebko.www.vebkoarcatalogandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends ConstraintLayout {

    private static final int PAINT_COLOR = 0xFF000000;

    List<List<Float>> listOfListsPoints = new ArrayList<List<Float>>();

    private Path drawPath;
    private Paint drawPaint;
    private Paint canvasPaint;

    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(PAINT_COLOR);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    public boolean onTouchEventCustom(@NonNull MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

//        Log.d("2222222222222222", "onTouchEvent: ");
//        for (int i=0; i<listOfListsPoints.size(); i++){
//            for (int j=0; j < listOfListsPoints.get(i).size(); j++){
//                Log.d("test: ", "onTouchEvent: "+ listOfListsPoints.get(i).get(j));
//            }
//        }
//        Log.d("11111111111111111", "onTouchEvent: ");

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                List<Float> floatPoint = new ArrayList<>();
                floatPoint.add(touchX);
                floatPoint.add(touchY);
                listOfListsPoints.add(floatPoint);
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

//    @Override
//    public boolean onTouchEvent(@NonNull MotionEvent event) {
//        float touchX = event.getX();
//        float touchY = event.getY();
//
////        Log.d("2222222222222222", "onTouchEvent: ");
////        for (int i=0; i<listOfListsPoints.size(); i++){
////            for (int j=0; j < listOfListsPoints.get(i).size(); j++){
////                Log.d("test: ", "onTouchEvent: "+ listOfListsPoints.get(i).get(j));
////            }
////        }
////        Log.d("11111111111111111", "onTouchEvent: ");
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                drawPath.moveTo(touchX, touchY);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                List<Float> floatPoint = new ArrayList<>();
//                floatPoint.add(touchX);
//                floatPoint.add(touchY);
//                listOfListsPoints.add(floatPoint);
//                drawPath.lineTo(touchX, touchY);
//                break;
//            case MotionEvent.ACTION_UP:
//                drawCanvas.drawPath(drawPath, drawPaint);
//                drawPath.reset();
//                break;
//            default:
//                return false;
//        }
//        invalidate();
//        return true;
//    }

    public void clear() {
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }
}
