package com.yaga.targetnav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
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

    private SensorManager sensorManager;
    private float pitch = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.surfaceView);
        coordsText = findViewById(R.id.coordsText);
        targetTypeSpinner = findViewById(R.id.targetTypeSpinner);
        overlayView = findViewById(R.id.overlayView);
        azimuth = getIntent().getFloatExtra("azimuth", 0f);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.target_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetTypeSpinner.setAdapter(adapter);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor != null) {
            sensorManager.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }

        surfaceView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Object selectedItem = targetTypeSpinner.getSelectedItem();
                if (selectedItem == null) {
                    Toast.makeText(this, "❗ Выберите тип цели", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (currentLat == 0.0 && currentLon == 0.0) {
                    Toast.makeText(this, "📡 Ожидание GPS", Toast.LENGTH_SHORT).show();
                    return true;
                }

                String type = selectedItem.toString();
                double realHeight;
                switch (type) {
                    case "Автомобиль": realHeight = 1.5; break;
                    case "Танк":       realHeight = 2.3; break;
                    case "Здание":     realHeight = 10.0; break;
                    default:           realHeight = 1.75; break;
                }

                double screenHeight = surfaceView.getHeight();
                double centerY = screenHeight / 2.0;
                double tapY = event.getY();
                double pixelHeight = Math.abs(centerY - tapY);

                if (pixelHeight < 10) {
                    Toast.makeText(this, "❗ Цель слишком мала на экране", Toast.LENGTH_SHORT).show();
                    return true;
                }

                double verticalFOV = Math.toRadians(60.0); // стандартное значение для многих камер
                distance = (realHeight * screenHeight) / (2.0 * pixelHeight * Math.tan(verticalFOV / 2.0));

                Toast.makeText(this, "📏 Расстояние: " + (int) distance + " м", Toast.LENGTH_SHORT).show();

                overlayView.clearTaps();
                overlayView.markTap((float) tapY);


                calculateTargetCoordinates();
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
        if (distance <= 0) return;

        double R = 6371000.0;
        double φ1 = Math.toRadians(currentLat);
        double λ1 = Math.toRadians(currentLon);
        double θ = Math.toRadians(azimuth);
        double δ = distance / R;

        double φ2 = Math.asin(Math.sin(φ1) * Math.cos(δ) + Math.cos(φ1) * Math.sin(δ) * Math.cos(θ));
        double λ2 = λ1 + Math.atan2(Math.sin(θ) * Math.sin(δ) * Math.cos(φ1),
                Math.cos(δ) - Math.sin(φ1) * Math.sin(φ2));

        double targetLat = Math.toDegrees(φ2);
        double targetLon = Math.toDegrees(λ2);

        coordsText.setText(String.format("🎯 Цель: %.1f м\n📍Широта: %.6f\n📍Долгота: %.6f",
                distance, targetLat, targetLon));
        Toast.makeText(this, "✅ Координаты цели получены", Toast.LENGTH_SHORT).show();
    }

    private final SensorEventListener rotationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                pitch = (float) Math.toDegrees(orientation[1]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(rotationListener);
    }
}
