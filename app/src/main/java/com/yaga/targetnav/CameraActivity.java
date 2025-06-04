package com.yaga.targetnav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    private TextView coordsText;
    private Spinner targetTypeSpinner;
    private OverlayView overlayView;

    private float azimuth = 0f;
    private double distance = 0;

    private LocationManager locationManager;
    private double currentLat = 0;
    private double currentLon = 0;

    private float firstY = -1;

    private final double verticalFOV = Math.toRadians(60.0); // поле зрения камеры

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.surfaceView);
        coordsText = findViewById(R.id.coordsText);
        targetTypeSpinner = findViewById(R.id.targetTypeSpinner);
        overlayView = findViewById(R.id.overlayView); // теперь берём из XML
        azimuth = getIntent().getFloatExtra("azimuth", 0f);

        // Установка адаптера для Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.target_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetTypeSpinner.setAdapter(adapter);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        surfaceView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float y = event.getY();
                if (firstY == -1) {
                    firstY = y;
                    overlayView.clearTaps();
                    overlayView.markTap(y);
                    Toast.makeText(this, "👆 Коснись верхней части цели", Toast.LENGTH_SHORT).show();
                } else {
                    float secondY = y;
                    overlayView.markTap(secondY);
                    float heightPixels = Math.abs(secondY - firstY);
                    firstY = -1;

                    if (heightPixels < 10) {
                        Toast.makeText(this, "❗ Слишком маленькая высота", Toast.LENGTH_SHORT).show();
                        overlayView.clearTaps();
                        return true;
                    }

                    Object selectedItem = targetTypeSpinner.getSelectedItem();
                    if (selectedItem == null) {
                        Toast.makeText(this, "❗ Пожалуйста, выберите тип цели", Toast.LENGTH_SHORT).show();
                        overlayView.clearTaps();
                        return true;
                    }

                    String type = selectedItem.toString();
                    double realHeight = 1.75;
                    if (type.equals("Автомобиль")) {
                        realHeight = 1.5;
                    } else if (type.equals("Танк")) {
                        realHeight = 2.3;
                    } else if (type.equals("Здание")) {
                        realHeight = 10.0;
                    }

                    double screenHeight = getResources().getDisplayMetrics().heightPixels;
                    distance = realHeight / (2 * Math.tan(verticalFOV / 2)) * (screenHeight / heightPixels);

                    Toast.makeText(this, "📏 Расстояние рассчитано: " + (int) distance + " м", Toast.LENGTH_SHORT).show();
                    overlayView.updateData(azimuth, (float) distance);
                    calculateTargetCoordinates();
                }
            }
            return true;
        });

        // Запрос координат
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
    }

    private void calculateTargetCoordinates() {
        double bearing = azimuth;
        double R = 6371000.0;
        double φ1 = Math.toRadians(currentLat);
        double λ1 = Math.toRadians(currentLon);
        double θ = Math.toRadians(bearing);
        double δ = distance / R;

        double φ2 = Math.asin(Math.sin(φ1) * Math.cos(δ) + Math.cos(φ1) * Math.sin(δ) * Math.cos(θ));
        double λ2 = λ1 + Math.atan2(Math.sin(θ) * Math.sin(δ) * Math.cos(φ1),
                Math.cos(δ) - Math.sin(φ1) * Math.sin(φ2));

        double targetLat = Math.toDegrees(φ2);
        double targetLon = Math.toDegrees(λ2);

        coordsText.setText(String.format("🎯 Цель: %.1f м\n📍Широта: %.6f\n📍Долгота: %.6f", distance, targetLat, targetLon));
        Toast.makeText(this, "✅ Координаты цели получены", Toast.LENGTH_SHORT).show();
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();
        }
    };

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }
}
