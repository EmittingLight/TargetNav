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
    private Paint paintTap;

    private Float tapY1 = null;
    private Float tapY2 = null;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLine = new Paint();
        paintLine.setColor(Color.RED);
        paintLine.setStrokeWidth(4f);

        paintTap = new Paint();
        paintTap.setColor(Color.YELLOW);
        paintTap.setStrokeWidth(3f);

        paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(42f);
        paintText.setAntiAlias(true);
    }

    public void updateData(float azimuth, float distance) {
        this.azimuth = azimuth;
        this.distance = distance;
        invalidate();
    }

    public void markTap(float y) {
        if (tapY1 == null) {
            tapY1 = y;
        } else {
            tapY2 = y;
        }
        invalidate();
    }

    public void clearTaps() {
        tapY1 = null;
        tapY2 = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 🔺 Перекрестие в центре
        canvas.drawLine(width / 2f - 50, height / 2f, width / 2f + 50, height / 2f, paintLine);
        canvas.drawLine(width / 2f, height / 2f - 50, width / 2f, height / 2f + 50, paintLine);

        // 🟡 Линии касания
        if (tapY1 != null) {
            canvas.drawLine(0, tapY1, width, tapY1, paintTap);
        }
        if (tapY2 != null) {
            canvas.drawLine(0, tapY2, width, tapY2, paintTap);
        }
    }

}
