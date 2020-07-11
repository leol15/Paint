package com.example.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.view.MotionEvent;
import android.view.View;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class PaintView extends View {

    int viewWidth;
    int viewHeight;
    int CURRENT_COLOR;
    int CURRENT_BG_COLOR = Color.rgb(50, 50, 50);
    float CURRENT_WIDTH = 20;

    List<PathWarp> paths;
    Stack<PathWarp> undoStack;

    Bitmap mBitmap;
    Canvas mBMCanvas;

    public PaintView(Context context) {
        super(context);
        p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setDither(true);                    // set the dither to true
        p.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        p.setPathEffect(new CornerPathEffect(10) );   // set the path effect when they join.
        p.setAntiAlias(true);

        paths = new LinkedList<>();
        undoStack = new Stack<>();

        currentLines = new HashMap<>();
        lastPos = new HashMap<>();

        this.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                viewWidth = getWidth();
                viewHeight = getHeight();
                mBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
                mBMCanvas = new Canvas(mBitmap);
                redrawBitmap();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        viewWidth = right - left;
        viewHeight = bottom - top;
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            mBMCanvas = new Canvas(mBitmap);
        }
    }


    Paint p;
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(60, 60, 60));
        canvas.drawBitmap(mBitmap, 0, 0, null);
        if (isDrawing && currentLines.size() > 0) {
            for (PathWarp pw : currentLines.values()) {
                p.setColor(pw.Color);
                p.setStrokeWidth(pw.Width);
                canvas.drawPath(pw.P, p);
            }
        }

    }


    Map<Integer, PathWarp> currentLines;
    Map<Integer, float[]> lastPos;

    boolean isDrawing;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int index = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int id = e.getPointerId(index);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (!currentLines.containsKey(id)) {
                    float[] pos = new float[]{e.getX(index), e.getY(index)};
                    Path newLine = new Path();
                    currentLines.put(id, new PathWarp(newLine, CURRENT_COLOR, CURRENT_WIDTH));
                    lastPos.put(id, pos);
                    newLine.moveTo(e.getX(index), e.getY(index));
                    isDrawing = true;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                int size = e.getHistorySize();
                float nx, ny;
                for (int id_key: currentLines.keySet()) {
                    for (int i = 0; i < size; i++) {
                        float[] lastP = lastPos.get(id_key);
                        nx = (e.getHistoricalX(e.findPointerIndex(id_key), i));
                        ny = (e.getHistoricalY(e.findPointerIndex(id_key), i));
                        currentLines.get(id_key).P.quadTo(lastP[0], lastP[1],
                                (nx + lastP[0]) / 2, (ny + lastP[1]) / 2);
                        lastP[0] = nx;
                        lastP[1] = ny;
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                paths.add(currentLines.get(id));
                currentLines.remove(id);
                lastPos.remove(id);
                PathWarp pw = paths.get(paths.size() - 1);
                p.setColor(pw.Color);
                p.setStrokeWidth(pw.Width);
                mBMCanvas.drawPath(pw.P, p);
                isDrawing = currentLines.size() != 0;
                undoStack.clear();
            default:
                return true;
        }
    }

    static class PathWarp {
        Path P;
        int Color;
        float Width;
        PathWarp(Path p, int color, float width) {
            P = p;
            Color = color;
            Width = width;
        }
    }

    /**
     * clear the bitmap
     */
    void clear() {
        if (isDrawing) return;
        paths.clear();
        undoStack.clear();
        redrawBitmap();
        invalidate();
    }

    void setBackground(int color) {
        if (color == CURRENT_BG_COLOR) {
            CURRENT_BG_COLOR = Color.TRANSPARENT;
        } else {
            CURRENT_BG_COLOR = color;
        }
        redrawBitmap();
        invalidate();
    }

    void setColor(int color) { CURRENT_COLOR = color; }

    void setStrokeWeight(float width) { CURRENT_WIDTH = width; }

    void undo() {
        if (paths.size() > 0) {
            undoStack.push(paths.remove(paths.size() - 1));
            redrawBitmap();
            invalidate();
        }
    }

    void redo() {
        if (undoStack.size() > 0) {
            paths.add(undoStack.pop());
            redrawBitmap();
            invalidate();
        }
    }

    void redrawBitmap() {
        mBMCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mBMCanvas.drawColor(CURRENT_BG_COLOR);
        for (PathWarp pw : paths) {
            p.setColor(pw.Color);
            p.setStrokeWidth(pw.Width);
            mBMCanvas.drawPath(pw.P, p);
        }
    }
}
