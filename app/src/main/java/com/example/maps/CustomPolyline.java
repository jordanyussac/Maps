package com.example.maps;

import android.graphics.Color;
import android.graphics.Paint;

import org.osmdroid.views.overlay.Polyline;

public class CustomPolyline extends Polyline{
    public CustomPolyline() {
        super();
    }

    //@Override
    protected Paint onCreatePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE); // Set the desired default stroke color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        return paint;
    }

}
