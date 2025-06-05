package com.yaga.targetnav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {

    private Paint paintLine;
    private Paint paintTap;

    private Float tapY = null;

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
    }

    public void markTap(float y) {
        tapY = y;
        invalidate();
    }

    public void clearTaps() {
        tapY = null;
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

        // 🟡 Линия касания
        if (tapY != null) {
            canvas.drawLine(0, tapY, width, tapY, paintTap);
        }
    }
}
