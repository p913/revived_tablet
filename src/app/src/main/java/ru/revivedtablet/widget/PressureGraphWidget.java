package ru.revivedtablet.widget;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import ru.revivedtablet.ImageUtils;
import ru.revivedtablet.R;
import ru.revivedtablet.RevivedTabletApp;

import java.util.Date;


public class PressureGraphWidget extends PlacedInLineWidget {
    private Rect rect;
    private int height;
    private int width;

    private Bitmap bmpPressGrid;

    private String name;

    private Paint paintFill;
    private Paint paintHistory;

    private double[] hourMeasurements = new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Date lastMeasurement = new Date();

    public PressureGraphWidget(int line, String name) {
        super(line);
        this.name = name;

        bmpPressGrid = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.pressgrid);
        bmpPressGrid.setDensity(Bitmap.DENSITY_NONE);

        width = bmpPressGrid.getWidth() + 2 * ImageUtils.URGE_PADDING_SMALL;
        height = bmpPressGrid.getHeight();
        rect = new Rect(0, 0, width, height);

        paintFill = new Paint();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(ImageUtils.URGE_FILL_COLOR);

        paintHistory = new Paint();
        paintHistory.setColor(Color.CYAN);
        paintHistory.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(rect, paintFill);
        canvas.drawBitmap(bmpPressGrid, rect.left + ImageUtils.URGE_PADDING_SMALL, rect.top, null);

        int x = rect.left + ImageUtils.URGE_PADDING_SMALL + 111;
        int centerY = rect.top + 32;
        int y1 = centerY, x1 = x, y2;
        double zeroValue = hourMeasurements[0];
        for (double value: hourMeasurements) {
            if (value != 0) {
                double diff = value - zeroValue;
                diff = Math.max(Math.min(diff, 6), -6);
                y2 = centerY - (int) (28.0 * diff / 6.0);
                canvas.drawLine(x1, y1, x, y2, paintHistory);
                y1 = y2;
                x1 = x;
            }
            x -= 7;
        }

    }

    public void setValue(double mm) {
        Date now = new Date();
        long diffSec = (now.getTime() - lastMeasurement.getTime()) / 1000;
        if (diffSec >= 3600) {
            int shiftHours = Math.min((int)(diffSec / 3600), hourMeasurements.length);
            lastMeasurement = now;
            for (int i = hourMeasurements.length - 1; i >= shiftHours; i--)
                hourMeasurements[i] = hourMeasurements[i - shiftHours];
            for (int i = 1; i < shiftHours; i++)
                hourMeasurements[i] = 0;
            hourMeasurements[0] = mm;
        } else {
            hourMeasurements[0] = mm;
        }
        if (invalidateBroker != null)
             invalidateBroker.invalidate();
    }

    @Override
    public void pause() {
        saveMeasurements();
    }

    @Override
    public void resume() {
        loadMeasurements();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setDrawRect(Rect rect) {
        this.rect.set(rect);
    }

    @Override
    public Rect getDrawRect() {
        return rect;
    }

    private void saveMeasurements() {
        SharedPreferences settings = RevivedTabletApp.getContext().getSharedPreferences(name, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("lastMeasurement", lastMeasurement.getTime());
        StringBuilder sb = new StringBuilder();
        for (double value: hourMeasurements)
            sb.append(value).append(";");
        editor.putString("hourMeasurements", sb.toString());
        editor.commit();
    }

    private void loadMeasurements() {
        SharedPreferences settings = RevivedTabletApp.getContext().getSharedPreferences(name, 0);
        lastMeasurement = new Date(settings.getLong("lastMeasurement", 0));
        String[] values = settings.getString("hourMeasurements", "").split(";");
        if (values.length == hourMeasurements.length)
            for (int i = 0; i < values.length; i++)
                try {
                    hourMeasurements[i] = Double.parseDouble(values[i]);
                } catch (NumberFormatException e) {
                }
    }

}
