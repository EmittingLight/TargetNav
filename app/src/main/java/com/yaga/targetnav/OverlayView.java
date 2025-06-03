package com.yaga.targetnav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {

    private float azimuth = 0f;
    private float distance = 0f;

    private Paint paintLine;
    private Paint paintText;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLine = new Paint();
        paintLine.setColor(Color.RED);
        paintLine.setStrokeWidth(4f);

        paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(42f);
        paintText.setAntiAlias(true);
    }

    public void updateData(float azimuth, float distance) {
        this.azimuth = azimuth;
        this.distance = distance;
        invalidate(); // перерисовать
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Перекрестие по центру экрана
        canvas.drawLine(width / 2f - 50, height / 2f, width / 2f + 50, height / 2f, paintLine);
        canvas.drawLine(width / 2f, height / 2f - 50, width / 2f, height / 2f + 50, paintLine);

        // Подписи: азимут и дистанция
        canvas.drawText("Азимут: " + (int) azimuth + "°", 30, 60, paintText);
        canvas.drawText("Дистанция: " + (int) distance + " м", 30, 110, paintText);
    }
}

