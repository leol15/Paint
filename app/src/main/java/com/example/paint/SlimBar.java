package com.example.paint;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class SlimBar extends View {

    int width;
    int height;
    int deviceW;

    int colorBoxStartY;
    int colorBoxSize;
    int colorBoxMargin;
    int currColorIndex;
    int prevColorIndex;

    Paint brush;

    PaintView PV;

    int[] colors;
    RectF[] colorBoxes;

    public SlimBar(Context context) {
        super(context);
        brush = new Paint();
        brush.setAntiAlias(true);
        colors = new int[8];
        colors[6] = Color.parseColor("#c8af00fa"); // purple
        colors[1] = Color.parseColor("#c8f23838"); // red
        colors[2] = Color.parseColor("#c8ffa600"); // orange
        colors[3] = Color.parseColor("#c8fffb00"); // yellow
        colors[4] = Color.parseColor("#c804d400"); // green
        colors[5] = Color.parseColor("#c80080db"); // blue
        colors[0] = Color.parseColor("#c8f3f3f3"); // white
        colors[7] = Color.parseColor("#c803fce3"); // black
        colorBoxes = new RectF[colors.length];
        for (int i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i] = new RectF(0,0,0,0);
        }

        currColorIndex = 0;
        brush.setColor(colors[currColorIndex]);
        prevColorIndex = currColorIndex;

        // device width
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay()
                .getMetrics(dm);
        deviceW = dm.widthPixels;
    }

    /**
     * initialize the various sizes
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = right - left;
        height = bottom - top;
        // color box
        colorBoxSize = height / 2 / (colorBoxes.length * 2) + 10;
        colorBoxMargin = colorBoxSize / colorBoxes.length * 2;
        colorBoxStartY = height / 2 + 50;
        for (int i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i].set(
                    left + width / 2f,
                    colorBoxStartY + i * (colorBoxSize + colorBoxMargin),
                    right,
                    colorBoxStartY + i * (colorBoxSize + colorBoxMargin) + colorBoxSize);
        }
        // custom color: 200 high
        customColorSelectorSize = width;
        customColorStartY = colorBoxStartY - (int) (customColorSelectorSize * 1.4);
        // stroke width
        adjustSizeCenterY = customColorStartY - 120;
        // bg color
        setBgStartY = adjustSizeCenterY - width - 70 - 20;
    }

    public void setPaintView(PaintView pv) {
        PV = pv;
        pv.setColor(colors[currColorIndex]);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        // draw bg
        if (selectingCustomColor) {
            if (customColorIndex == 0) brush.setColor(Color.rgb(200, 0, 0));
            if (customColorIndex == 1) brush.setColor(Color.rgb(0, 200, 0));
            if (customColorIndex == 2) brush.setColor(Color.rgb(0, 0, 200));
            if (customColorIndex == 3) {
                brush.setColor(Color.argb(customColor[3],
                        customColor[0], customColor[1], customColor[2]));
            }
            canvas.drawRoundRect(0,
                    (255 - customColor[customColorIndex]) / 255f * height,
                    width, height, 40, 40, brush);
        }
        drawStrokeWeightSelection(canvas);
        drawSetBackground(canvas);
        drawColorSelection(canvas);
        drawColorBox(canvas);
    }


    int selectedColorBoxTranslation = 0;
    int hoveredColorBoxIndex;
    /**
     * draw the color selections
     * @param c the canvas
     */
    void drawColorBox(Canvas c) {
        brush.setStyle(Paint.Style.FILL);
        int i = 0;
        if (selectedColorBoxTranslation > - width / 3f) {
            selectedColorBoxTranslation -= 1;
            postInvalidate();
        }
        for (RectF r : colorBoxes) {
            brush.setColor(colors[i]);
            c.save();
            if (i == currColorIndex) {
                c.translate(selectedColorBoxTranslation, 0);
            } else if (i == hoveredColorBoxIndex && selectingColor) {
                c.translate(-width / 4f, 0);
            } else if (i == prevColorIndex) {
                c.translate(-width / 3f - selectedColorBoxTranslation, 0);
            }
            c.drawRoundRect(r, 30, 30, brush);
            c.restore();
            i += 1;
        }
    }

    // get color box index
    int getColorBoxIndex(int yPos) {
        int index = 0;
        yPos -= colorBoxStartY;
        while (yPos > colorBoxSize) {
            yPos -= colorBoxSize + colorBoxMargin;
            index += 1;
        }
        return yPos > 0 && index < colors.length ? index : -1;
    }


    int adjustSizeCenterY;
    float currStrokeWidth = 20;
//    int adjustSizeSize;
    void drawStrokeWeightSelection(Canvas c) {
        brush.setStyle(Paint.Style.FILL);
        brush.setColor(Color.BLACK);
        brush.setAlpha(100);
        c.drawRoundRect(0, adjustSizeCenterY - 70, width,
                adjustSizeCenterY + 70, 0, 50, brush);
        brush.setColor(Color.WHITE);
        c.drawCircle(width / 2f, adjustSizeCenterY, currStrokeWidth / 2f, brush);
    }


    // custom color
    int customColorSelectorSize;
    int customColorStartY;
    int[] customColor = new int[]{244, 244, 244, 255};
    void drawColorSelection(Canvas c) {
        brush.setStyle(Paint.Style.FILL);
        if (selectingCustomColor) {
            brush.setColor(Color.argb(customColor[3],
                    customColor[0], customColor[1], customColor[2]));
            c.drawRoundRect(0, customColorStartY,
                    customColorSelectorSize,
                    customColorStartY + customColorSelectorSize, 30, 30, brush);
        } else {
            for (int i = 0; i < customColor.length; i++) {
                if (i == 0) brush.setColor(Color.RED);
                if (i == 1) brush.setColor(Color.GREEN);
                if (i == 2) brush.setColor(Color.BLUE);
                if (i == 3)
                    brush.setColor(Color.argb(customColor[3],
                            customColor[0], customColor[1], customColor[2]));
                c.drawArc(0, customColorStartY, width,
                        customColorStartY + customColorSelectorSize,
                        90 * (4 - i), 90, true, brush);

            }
        }
    }

    int getCustomColorSelection(int xPos, int yPos) {
        if (yPos < customColorStartY || yPos > customColorStartY + customColorSelectorSize) {
            return -1;
        }
        xPos += deviceW - width;
        int index = xPos / (deviceW / 4 + 1);
        return index > -1 && index < 4 ? index : -1;
    }


    int setBgStartY;
    /**
     * click to set back background to color[currentColorIndex]
     */
    void drawSetBackground(Canvas c) {
        brush.setStyle(Paint.Style.FILL_AND_STROKE);
        brush.setColor(Color.BLACK);
        c.drawRoundRect(0, setBgStartY, width, setBgStartY + width, 40, 40, brush);
        brush.setTextSize(width / 2f);
        brush.setTextAlign(Paint.Align.CENTER);
        brush.setColor(Color.WHITE);
        c.drawText("B", width / 3f * 1.2f, setBgStartY + width / 1.8f, brush);
        c.drawText("G", width / 3f * 1.7f, setBgStartY + width / 1.4f, brush);
    }

    int clickSetBackground(int yPos) {
        if (yPos < setBgStartY || yPos > setBgStartY + width) return -1;
        PV.setBackground(colors[currColorIndex]);
        return 1;
    }



    // state machine
    int startX;
    int startY;
    boolean selectingColor;
    boolean selectingWidth;
    boolean selectingCustomColor;
    int customColorIndex;
    float lastY;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) e.getX();
                startY = (int) e.getY();
                touchStart(e);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(e);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(e);
            default:
        }
        return true;
    }

    void touchStart(MotionEvent e) {
        hoveredColorBoxIndex = getColorBoxIndex((int) e.getY());
        if (hoveredColorBoxIndex != -1) {
            selectingColor = true;
        } else if (Math.abs(e.getY() - adjustSizeCenterY) < 70) {
            selectingWidth = true;
            lastY = e.getY();
        } else {
            int index = getCustomColorSelection((int) e.getX(), (int) e.getY());
            if (index != -1) {
                selectingCustomColor = true;
                customColorIndex = index;
                lastY = e.getY();
            }
        }
    }

    void touchMove(MotionEvent e) {
        if (selectingColor) {
            int nextI = getColorBoxIndex((int) e.getY());
            if (nextI != hoveredColorBoxIndex) invalidate();
            hoveredColorBoxIndex = nextI;
        } else if (selectingWidth) {
            currStrokeWidth -= (e.getY() - lastY) / 4;
            lastY = e.getY();
            currStrokeWidth = Math.min(200, Math.max(5, currStrokeWidth));
            invalidate();
        } else if (selectingCustomColor) {
            int newIndex = getCustomColorSelection((int) e.getX(), customColorStartY);
            if (newIndex != -1 && customColorIndex != newIndex) {
                customColorIndex = newIndex;
                lastY = e.getY();
            }
            customColor[customColorIndex] -= (e.getY() - lastY) / 1.5;
            lastY = e.getY();
            customColor[customColorIndex] = Math.max(0, customColor[customColorIndex]);
            customColor[customColorIndex] = Math.min(255, customColor[customColorIndex]);
            colors[0] = Color.argb(customColor[3], customColor[0], customColor[1], customColor[2]);
            invalidate();
        }

    }

    void touchUp(MotionEvent e) {
        if (selectingColor) { // select color
            selectingColor = false;
            if (hoveredColorBoxIndex > -1 &&
                    hoveredColorBoxIndex < colors.length &&
                    currColorIndex != hoveredColorBoxIndex) {
                selectedColorBoxTranslation = 0;
                prevColorIndex = currColorIndex;
                currColorIndex = hoveredColorBoxIndex;
                PV.setColor(colors[currColorIndex]);
                invalidate();
            }
        } else if (selectingWidth) {
            selectingWidth = false;
            PV.setStrokeWeight(currStrokeWidth);
        } else if (selectingCustomColor) {
            selectingCustomColor = false;
            System.out.println("setting to false");
            if (currColorIndex == 0) PV.setColor(colors[currColorIndex]);
            invalidate();
        } else { // undo redo
            if (Math.abs(startY - e.getY()) > 200) {
                if (startY - e.getY() < 0) PV.undo();
                else PV.redo();
                invalidate();
            } else {
                clickSetBackground((int) e.getY());
            }
        }
    }

}
