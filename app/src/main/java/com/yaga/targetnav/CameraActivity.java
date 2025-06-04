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

        azimuth = getIntent().getFloatExtra("azimuth", 0f);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        // 👆 Обработка тапов
        surfaceView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float y = event.getY();
                if (firstY == -1) {
                    firstY = y;
                    Toast.makeText(this, "Коснись верхней части цели", Toast.LENGTH_SHORT).show();
                } else {
                    float secondY = y;
                    float heightPixels = Math.abs(secondY - firstY);
                    firstY = -1; // сброс

                    if (heightPixels < 10) {
                        Toast.makeText(this, "Слишком маленькая высота", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    // Пример: считаем, что цель — человек 1.75 м
                    double realHeight = 1.75;
                    double screenHeight = getResources().getDisplayMetrics().heightPixels;

                    distance = realHeight / (2 * Math.tan(verticalFOV / 2)) * (screenHeight / heightPixels);

                    // Вычисляем координаты цели
                    calculateTargetCoordinates();
                }
            }
            return true;
        });

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

        coordsText.setText(String.format("Цель на расстоянии %.1f м\nШирота: %.6f\nДолгота: %.6f", distance, targetLat, targetLon));
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
            camera.setDisplayOrientation(90); // 🔧 добавь это!
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
